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

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import appeng.hooks.extensions.LadderHook;

/**
 * Dispatches {@link LadderHook} (the shim for NeoForge's {@code IBlockExtension#isLadder}). NeoForge replaces the
 * {@code BlockTags.CLIMBABLE} check in {@code LivingEntity#onClimbable} with the overridable extension method; this
 * mixin replicates the vanilla guards that run before that check (spectator, glide-through) and then substitutes the
 * hook's verdict (both ways — an overriding implementor fully replaces the tag check on NeoForge too).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLadderMixin extends Entity {

    @Shadow
    private Optional<BlockPos> lastClimbablePos;

    @Shadow
    public abstract boolean isFallFlying();

    private LivingEntityLadderMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void ae2$ladderHook(CallbackInfoReturnable<Boolean> cir) {
        if (isSpectator()) {
            return; // vanilla returns false
        }
        var state = getInBlockState();
        if (!(state.getBlock() instanceof LadderHook hook)) {
            return;
        }
        if (isFallFlying() && state.is(BlockTags.CAN_GLIDE_THROUGH)) {
            return; // vanilla returns false
        }
        var pos = blockPosition();
        if (hook.isLadder(state, level(), pos, (LivingEntity) (Object) this)) {
            this.lastClimbablePos = Optional.of(pos);
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }
}
