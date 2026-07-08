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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import appeng.hooks.SkyStoneBreakSpeed;

/**
 * Dispatches {@link SkyStoneBreakSpeed} (the shim for NeoForge's {@code PlayerEvent.BreakSpeed}). NeoForge posts that
 * event inside its break-speed computation and lets a listener rewrite the speed; Fabric has no equivalent event, so
 * this mixin post-processes {@link Player#getDestroySpeed(BlockState)}: if the hook returns a non-null increased speed
 * for the current block + main-hand tool, it substitutes it as the return value.
 * <p>
 * Net effect (matching NeoForge): sky stone blocks — created with destroy time 50 so they are barely minable with iron
 * — break {@link SkyStoneBreakSpeed#SPEEDUP_FACTOR}× faster when using a tool better than iron.
 */
@Mixin(Player.class)
public abstract class PlayerDestroySpeedMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void ae2$skyStoneBreakSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        var newSpeed = SkyStoneBreakSpeed.getIncreasedBreakSpeed((Player) (Object) this, state, cir.getReturnValueF());
        if (newSpeed != null) {
            cir.setReturnValue(newSpeed);
        }
    }
}
