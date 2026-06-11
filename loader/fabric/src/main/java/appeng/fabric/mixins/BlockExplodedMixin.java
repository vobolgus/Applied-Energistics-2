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

import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import appeng.hooks.extensions.BlockExplodedHook;

/**
 * Dispatches {@link BlockExplodedHook} (the shim for NeoForge's {@code IBlockExtension#onBlockExploded}). NeoForge
 * replaces the trailing {@code level.setBlock(pos, AIR, 3); block.wasExploded(...)} pair in
 * {@code BlockBehaviour#onExplosionHit} with a call to the (overridable) extension method, whose default body is
 * exactly that pair; this mixin injects right before the {@code setBlock} call (i.e. after the explosion drops were
 * handled, matching NeoForge's order) and cancels the vanilla pair for implementors.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockExplodedMixin {

    @Inject(method = "onExplosionHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), cancellable = true)
    private void ae2$onBlockExploded(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit, CallbackInfo ci) {
        if ((Object) this instanceof BlockExplodedHook hook) {
            hook.onBlockExploded(state, level, pos, explosion);
            ci.cancel();
        }
    }
}
