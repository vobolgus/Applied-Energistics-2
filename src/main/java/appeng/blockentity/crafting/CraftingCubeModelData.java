/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.blockentity.crafting;

import java.util.EnumSet;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;

import appeng.api.util.AEColor;

/**
 * Loader-neutral render data for blocks of the crafting cube multi-block.
 *
 * @param connections which sides of the block are connected to other parts of a formed crafting cube
 * @param color       the color of the attached cable bus for crafting monitors, null for all other crafting cube blocks
 */
public record CraftingCubeModelData(EnumSet<Direction> connections, @Nullable AEColor color) {

    public static CraftingCubeModelData create(EnumSet<Direction> connections) {
        return new CraftingCubeModelData(connections, null);
    }

    public static CraftingCubeModelData create(EnumSet<Direction> connections, AEColor color) {
        return new CraftingCubeModelData(connections, Objects.requireNonNull(color));
    }
}
