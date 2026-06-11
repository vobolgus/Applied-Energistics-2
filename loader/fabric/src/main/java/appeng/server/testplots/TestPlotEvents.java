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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;

/**
 * Fabric counterparts of the NeoForge test-plot events ({@code KitOutPlayerEvent}, {@code SpawnExtraGridTestTools}):
 * simple callback registries posted to by {@code appeng.fabric.gametest.FabricTestPlotPlatform}.
 */
public final class TestPlotEvents {
    private static final List<Consumer<ServerPlayer>> KIT_OUT_PLAYER_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<SpawnExtraGridTestTools>> SPAWN_EXTRA_GRID_TEST_TOOLS_LISTENERS = new CopyOnWriteArrayList<>();

    static {
        // The default AE2 listener that kits out grid-test-tool chests with linked wireless terminals.
        SPAWN_EXTRA_GRID_TEST_TOOLS_LISTENERS.add(SpawnTestTools::spawnWirelessTerminals);
    }

    private TestPlotEvents() {
    }

    public static void addKitOutPlayerListener(Consumer<ServerPlayer> listener) {
        KIT_OUT_PLAYER_LISTENERS.add(listener);
    }

    public static void addSpawnExtraGridTestToolsListener(Consumer<SpawnExtraGridTestTools> listener) {
        SPAWN_EXTRA_GRID_TEST_TOOLS_LISTENERS.add(listener);
    }

    public static void fireKitOutPlayer(ServerPlayer player) {
        for (var listener : KIT_OUT_PLAYER_LISTENERS) {
            listener.accept(player);
        }
    }

    public static void fireSpawnExtraGridTestTools(Identifier plotId, InternalInventory inventory, IGrid grid) {
        var event = new SpawnExtraGridTestTools(plotId, inventory, grid);
        for (var listener : SPAWN_EXTRA_GRID_TEST_TOOLS_LISTENERS) {
            listener.accept(event);
        }
    }

    /**
     * The Fabric counterpart of the NeoForge {@code SpawnExtraGridTestTools} event payload.
     */
    public static final class SpawnExtraGridTestTools {
        private final Identifier plotId;
        private final InternalInventory inventory;
        private final IGrid grid;

        public SpawnExtraGridTestTools(Identifier plotId, InternalInventory inventory, IGrid grid) {
            this.plotId = plotId;
            this.inventory = inventory;
            this.grid = grid;
        }

        public Identifier getPlotId() {
            return plotId;
        }

        public InternalInventory getInventory() {
            return inventory;
        }

        public IGrid getGrid() {
            return grid;
        }
    }
}
