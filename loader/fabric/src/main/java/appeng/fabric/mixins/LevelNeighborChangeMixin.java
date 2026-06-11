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

package appeng.fabric.mixins;

import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import appeng.hooks.extensions.NeighborChangeHook;

/**
 * Dispatches {@link NeighborChangeHook} (the shim for NeoForge's {@code IBlockExtension#onNeighborChange}). NeoForge
 * patches {@code Level#updateNeighbourForOutputSignal} to call {@code onNeighborChange} on every direct neighbor
 * (horizontal directions first, then vertical); this mixin delivers the same per-direction notifications to
 * implementors before the vanilla comparator updates run.
 */
@Mixin(Level.class)
public abstract class LevelNeighborChangeMixin {

    /** NeoForge's notification order: the four horizontal directions, then the two vertical ones. */
    @Unique
    private static final Direction[] AE2$NEIGHBOR_UPDATE_ORDER = Stream
            .concat(Direction.Plane.HORIZONTAL.stream(), Direction.Plane.VERTICAL.stream())
            .toArray(Direction[]::new);

    @Inject(method = "updateNeighbourForOutputSignal", at = @At("HEAD"))
    private void ae2$onNeighborChange(BlockPos pos, Block changedBlock, CallbackInfo ci) {
        var self = (Level) (Object) this;
        for (var direction : AE2$NEIGHBOR_UPDATE_ORDER) {
            var relativePos = pos.relative(direction);
            if (self.hasChunkAt(relativePos)) {
                var state = self.getBlockState(relativePos);
                if (state.getBlock() instanceof NeighborChangeHook hook) {
                    hook.onNeighborChange(state, self, relativePos, pos);
                }
            }
        }
    }
}
