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
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;

import appeng.hooks.extensions.EntityDestroyHook;

/**
 * Dispatches {@link EntityDestroyHook} (the shim for NeoForge's {@code IBlockExtension#canEntityDestroy}) for the
 * wither's block-breaking burst. NeoForge gates {@code level.destroyBlock} in {@code WitherBoss#customServerAiStep} on
 * {@code state.canEntityDestroy}; this redirect refuses the destruction for implementors that veto it.
 */
@Mixin(WitherBoss.class)
public abstract class WitherBossEntityDestroyMixin {

    @Redirect(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private boolean ae2$canEntityDestroy(ServerLevel level, BlockPos pos, boolean dropBlock, Entity breaker) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof EntityDestroyHook hook && !hook.canEntityDestroy(state, level, pos, breaker)) {
            return false;
        }
        return level.destroyBlock(pos, dropBlock, breaker);
    }
}
