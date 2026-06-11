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

package appeng.hooks.extensions;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shim for NeoForge's {@code IBlockExtension#onCaughtFire}: called when the block is ignited (fire spread, flint and
 * steel, AE2's entropy manipulator, ...). The default body matches NeoForge's default.
 *
 * @see appeng.util.LoaderPlatform#onCaughtFire the call-site seam, whose Fabric implementation dispatches to this
 *      interface
 */
public interface BlockCaughtFireHook {
    default boolean onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction,
            @Nullable LivingEntity igniter) {
        return true;
    }
}
