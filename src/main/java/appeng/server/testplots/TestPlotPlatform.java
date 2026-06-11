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

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;

/**
 * Loader-neutral seam for the test-plot infrastructure: class scanning for {@link TestPlotClass} and posting the
 * loader-bus extension events that allow listeners to customize test setups. The loader-specific implementation (e.g.
 * {@code appeng.neoforge.gametest.NeoForgeTestPlotPlatform}) is injected once during mod construction via
 * {@link #init}.
 */
public interface TestPlotPlatform {
    /**
     * @return All classes annotated with {@link TestPlotClass}.
     */
    List<Class<?>> findTestPlotClasses();

    /**
     * Notifies listeners that the given player is being kitted out for a test world (was
     * {@code appeng.server.testplots.KitOutPlayerEvent} on the NeoForge bus).
     */
    void postKitOutPlayer(ServerPlayer player);

    /**
     * Notifies listeners to spawn additional testing tools into a container placed next to a spawned AE2 grid (was
     * {@code appeng.server.testplots.SpawnExtraGridTestTools} on the NeoForge bus).
     */
    void postSpawnExtraGridTestTools(Identifier plotId, InternalInventory inventory, IGrid grid);

    static TestPlotPlatform get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific TestPlotPlatform has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(TestPlotPlatform platform) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The TestPlotPlatform has already been initialized");
        }
        Holder.INSTANCE = platform;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile TestPlotPlatform INSTANCE;

        private Holder() {
        }
    }
}
