/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2025, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.server.testplots;

import java.time.Instant;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import appeng.api.features.IPlayerRegistry;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.server.services.compass.ServerCompassService;
import appeng.server.testworld.PlotBuilder;
import appeng.server.testworld.PlotTestHelper;
import appeng.spatial.SpatialStoragePlotManager;
import appeng.spatial.SpatialStorageWorldData;
import appeng.spatial.TransitionInfo;

/**
 * Save/reload round-trip tests for AE2's three {@code SavedData} holders: the spatial storage plot list, the player
 * registry (ME player IDs) and the compass/meteorite regions.
 * <p>
 * All three go through the {@code LoaderPlatform#computeSavedDataIfAbsent} seam (NeoForge: patched level-sensitive
 * {@code SavedDataType}; Fabric: per-level vanilla types with a non-null {@code DataFixTypes}). A silent codec-shape
 * mistake on either loader would corrupt user saves without any runtime error, so each test mutates the live instance
 * through its public API, flushes it to disk through the real save path ({@code SavedDataStorage#saveAndJoin}), evicts
 * the cached instance and asserts that a re-read through {@code SavedDataStorage#readSavedData} — the exact code path
 * used at world load, including the DFU pass on Fabric — restores the state.
 */
@TestPlotClass
public final class SavedDataTestPlots {

    private SavedDataTestPlots() {
    }

    /**
     * Flushes all dirty saved data of the level to disk, then evicts the cached instance for the given id so the next
     * access re-materializes it from the written file via the same factory/codec the loader uses at world load. The
     * {@code cache} field is private in vanilla; widened via accesstransformer.cfg (NeoForge) / ae2.accesswidener
     * (Fabric).
     */
    private static void saveAndEvict(PlotTestHelper helper, ServerLevel level, Identifier id) {
        var storage = level.getDataStorage();
        storage.saveAndJoin();
        var evicted = storage.cache.keySet().removeIf(type -> type.id().equals(id));
        helper.check(evicted, "expected " + id + " to be a cached SavedData before eviction");
    }

    /**
     * Allocates a spatial storage plot (including a last-transition record, exactly what a spatial IO port writes) and
     * asserts that id, dimensions, owner, origin and the transition info survive a save/reload of
     * {@link SpatialStorageWorldData} in the spatial storage level.
     */
    @TestPlot("spatial_plot_saved_data")
    public static void spatialPlotSavedData(PlotBuilder plot) {
        plot.test(helper -> {
            var manager = SpatialStoragePlotManager.INSTANCE;
            var size = new BlockPos(2, 3, 4);
            var ownerId = 1234;

            var allocated = manager.allocatePlot(size, ownerId);
            var plotId = allocated.getId();
            // Fixed timestamp: ExtraCodecs.INSTANT_ISO8601 must round-trip it exactly
            var transition = new TransitionInfo(
                    helper.getLevel().dimension().identifier(),
                    new BlockPos(1, 2, 3),
                    new BlockPos(4, 5, 6),
                    Instant.ofEpochMilli(1234567890123L));
            manager.setLastTransition(plotId, transition);

            saveAndEvict(helper, manager.getLevel(), SpatialStorageWorldData.ID);

            var reloaded = manager.getPlot(plotId);
            helper.check(reloaded != null, "plot " + plotId + " missing after reload");
            // A different instance proves the plot was re-materialized from NBT, not served from the cache
            helper.check(reloaded != allocated, "expected a re-materialized plot instance after reload");
            helper.assertEquals(BlockPos.ZERO, size, reloaded.getSize());
            helper.assertEquals(BlockPos.ZERO, ownerId, reloaded.getOwner());
            helper.assertEquals(BlockPos.ZERO, allocated.getOrigin(), reloaded.getOrigin());
            helper.assertEquals(BlockPos.ZERO, transition, reloaded.getLastTransition());

            manager.freePlot(plotId, false);
            helper.succeed();
        });
    }

    /**
     * Registers a profile UUID with the player registry and asserts that the UUID↔ME-player-id mapping and the next-id
     * continuation survive a save/reload of the {@code ae2:players} data attached to the overworld.
     */
    @TestPlot("player_registry_saved_data")
    public static void playerRegistrySavedData(PlotBuilder plot) {
        plot.test(helper -> {
            var server = helper.getLevel().getServer();
            var overworld = server.getLevel(ServerLevel.OVERWORLD);
            var profileId = UUID.fromString("30b1b2ab-14b1-4362-9eb1-b1b64f52ab72");

            var registry = IPlayerRegistry.getMapping(server);
            var playerId = registry.getPlayerId(profileId);

            saveAndEvict(helper, overworld, AppEng.makeId("players"));

            var reloaded = IPlayerRegistry.getMapping(server);
            helper.check(reloaded != registry, "expected a re-materialized player registry after reload");
            helper.assertEquals(BlockPos.ZERO, profileId, reloaded.getProfileId(playerId));
            helper.assertEquals(BlockPos.ZERO, playerId, reloaded.getPlayerId(profileId));
            // The highest-assigned-id watermark must be restored too: a fresh profile may not collide
            var otherId = reloaded.getPlayerId(UUID.fromString("8ea82cbc-9298-49de-895f-fbdc1e5a0c8f"));
            helper.check(otherId > playerId, "expected a fresh profile to get an id above " + playerId
                    + " after reload, but got " + otherId);
            helper.succeed();
        });
    }

    /**
     * Marks a mysterious cube in the compass region (the same call chunk updates make) and asserts the compass still
     * points at its chunk after the region's {@code ae2:compass_x_z} data is saved and reloaded.
     */
    @TestPlot("compass_region_saved_data")
    public static void compassRegionSavedData(PlotBuilder plot) {
        plot.block(BlockPos.ZERO, AEBlocks.MYSTERIOUS_CUBE);
        plot.test(helper -> {
            var level = helper.getLevel();
            var cubePos = helper.absolutePos(BlockPos.ZERO);
            var chunkPos = ChunkPos.containing(cubePos);

            ServerCompassService.notifyBlockChange(level, cubePos);
            var before = ServerCompassService.getClosestMeteorite(level, chunkPos);
            helper.check(before.isPresent(), "compass target not found before reload");

            // CompassRegion files are per 1024x1024-chunk region; derive the id of the region containing the cube
            var regionId = AppEng.makeId(
                    "compass_" + Math.floorDiv(chunkPos.x(), 1024) + "_" + Math.floorDiv(chunkPos.z(), 1024));
            saveAndEvict(helper, level, regionId);

            var after = ServerCompassService.getClosestMeteorite(level, chunkPos);
            helper.check(after.isPresent(), "compass target lost after reload");
            // The reloaded bitmap must still point at the cube's chunk
            helper.assertEquals(BlockPos.ZERO, chunkPos, ChunkPos.containing(after.get()));
            helper.succeed();
        });
    }
}
