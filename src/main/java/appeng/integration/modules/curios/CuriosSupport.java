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

package appeng.integration.modules.curios;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Loader-neutral seam for accessing the slot-indexed inventory of an accessory mod (Curios on NeoForge). The
 * loader-specific implementation (e.g. {@code appeng.neoforge.integration.NeoForgeCuriosSupport}) is injected once
 * during mod construction via {@link #init}.
 */
public interface CuriosSupport {
    /**
     * @return The curios inventory of the given player, or null if there is none (e.g. the accessory mod is not
     *         installed).
     */
    @Nullable
    Inventory getCuriosInventory(Player player);

    /**
     * Slot-indexed read-only view of a player's curios inventory. Slot indices are stable and are used to locate menu
     * host items (see {@code appeng.menu.locator.CuriosItemLocator}).
     */
    interface Inventory {
        int size();

        ItemStack getStack(int slot);
    }

    static CuriosSupport get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific CuriosSupport has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(CuriosSupport support) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The CuriosSupport has already been initialized");
        }
        Holder.INSTANCE = support;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile CuriosSupport INSTANCE;

        private Holder() {
        }
    }
}
