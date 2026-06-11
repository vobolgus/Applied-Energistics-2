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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import appeng.hooks.extensions.CloneItemStackHook;

/**
 * Dispatches {@link CloneItemStackHook} (the shim for NeoForge's player-aware
 * {@code IBlockExtension#getCloneItemStack}). NeoForge passes the picking player into the clone-item lookup in
 * {@code ServerGamePacketListenerImpl#handlePickItemFromBlock}; AE2's cable bus uses it to pick the part the player is
 * actually looking at.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PickBlockCloneItemStackMixin {

    @Shadow
    public ServerPlayer player;

    @Redirect(method = "handlePickItemFromBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ae2$playerAwareCloneItemStack(BlockState state, LevelReader level, BlockPos pos,
            boolean includeData) {
        if (state.getBlock() instanceof CloneItemStackHook hook) {
            return hook.getCloneItemStack(level, pos, state, includeData, this.player);
        }
        return state.getCloneItemStack(level, pos, includeData);
    }
}
