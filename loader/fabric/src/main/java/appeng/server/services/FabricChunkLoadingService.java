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

package appeng.server.services;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import appeng.blockentity.spatial.SpatialAnchorBlockEntity;

/**
 * Fabric implementation of the {@link ChunkLoadingService} seam, backed by vanilla's forced chunks
 * ({@code ServerLevel.setChunkForced}) and the shared {@link ChunkLoadState} saved data (which tracks the owning block
 * positions per chunk, mirroring NeoForge's per-block tickets).
 * <p>
 * On level load, the persisted state is validated like NeoForge's {@code validateTickets} callback: chunks are
 * re-registered with their owning {@link SpatialAnchorBlockEntity} or released if the anchor no longer exists.
 */
public class FabricChunkLoadingService implements ChunkLoadingService {

    // Flag to ignore a server after it is stopping as grid nodes might reevaluate their grids during a shutdown.
    private boolean running = true;

    public FabricChunkLoadingService() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.running = true);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> this.running = false);
        ServerLevelEvents.LOAD.register((server, level) -> validateTickets(level));
    }

    private void validateTickets(ServerLevel level) {
        // Iterate over all block positions registered as chunk loaders to initialize them.
        // Mirrors NeoForge's LoadingValidationCallback.
        var state = ChunkLoadState.get(level);
        state.getAllBlocks().forEach((blockPos, chunks) -> {
            var blockEntity = level.getBlockEntity(blockPos);

            if (blockEntity instanceof SpatialAnchorBlockEntity anchor) {
                for (var chunk : chunks) {
                    anchor.registerChunk(ChunkPos.unpack(chunk));
                }
            } else {
                // The anchor no longer exists: release all chunks it forced.
                state.releaseAll(blockPos);
            }
        });
    }

    @Override
    public boolean forceChunk(ServerLevel level, BlockPos owner, ChunkPos position) {
        if (running) {
            ChunkLoadState.get(level).forceChunk(position, owner);
            return true;
        }

        return false;
    }

    @Override
    public boolean releaseChunk(ServerLevel level, BlockPos owner, ChunkPos position) {
        if (running) {
            ChunkLoadState.get(level).releaseChunk(position, owner);
            return true;
        }

        return false;
    }
}
