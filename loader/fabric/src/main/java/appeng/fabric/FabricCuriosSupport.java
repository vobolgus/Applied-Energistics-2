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

package appeng.fabric;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;

import appeng.integration.modules.curios.CuriosSupport;

/**
 * Fabric implementation of the {@link CuriosSupport} seam. There is no Curios on Fabric; returning null makes the
 * shared consumers skip the accessory inventory entirely (a Trinkets integration could replace this later).
 */
public class FabricCuriosSupport implements CuriosSupport {
    @Override
    @Nullable
    public Inventory getCuriosInventory(Player player) {
        return null;
    }
}
