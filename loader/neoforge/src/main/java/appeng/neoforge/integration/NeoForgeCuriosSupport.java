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

package appeng.neoforge.integration;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.integration.modules.curios.CuriosIntegration;
import appeng.integration.modules.curios.CuriosSupport;

/**
 * {@link CuriosSupport} backed by the Curios entity capability. The capability is created without loading Curios
 * classes (see {@link CuriosIntegration}), so no mod-loaded guard is needed: the lookup simply returns null when Curios
 * is not installed.
 */
public class NeoForgeCuriosSupport implements CuriosSupport {
    @Override
    @Nullable
    public Inventory getCuriosInventory(Player player) {
        var cap = player.getCapability(CuriosIntegration.ITEM_HANDLER);
        if (cap == null) {
            return null;
        }
        return new Inventory() {
            @Override
            public int size() {
                return cap.size();
            }

            @Override
            public ItemStack getStack(int slot) {
                return cap.getResource(slot).toStack();
            }
        };
    }
}
