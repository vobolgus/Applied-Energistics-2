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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import appeng.hooks.extensions.SneakBypassUseHook;

/**
 * Dispatches {@link SneakBypassUseHook} (the shim for NeoForge's {@code IItemExtension#doesSneakBypassUse}). NeoForge
 * computes {@code suppressUsingBlock} in {@code ServerPlayerGameMode#useItemOn} as
 * {@code (isSecondaryUseActive && haveSomethingInOurHands) && !(mainHand.doesSneakBypassUse && offHand.doesSneakBypassUse)}
 * where an <em>empty</em> stack bypasses (NeoForge's {@code IItemStackExtension} semantics). Redirecting the single
 * {@code isSecondaryUseActive()} call yields the same value: {@code (a && !c) && b == (a && b) && !c}.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class SneakBypassUseMixin {

    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z"))
    private boolean ae2$sneakBypassUse(ServerPlayer player, ServerPlayer playerArg, Level level, ItemStack itemStack,
            InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isSecondaryUseActive()) {
            return false;
        }
        var pos = hitResult.getBlockPos();
        return !(ae2$bypassesSneak(player.getMainHandItem(), level, pos, player)
                && ae2$bypassesSneak(player.getOffhandItem(), level, pos, player));
    }

    @Unique
    private static boolean ae2$bypassesSneak(ItemStack stack, Level level, BlockPos pos, Player player) {
        // Mirrors NeoForge's IItemStackExtension#doesSneakBypassUse: empty stacks bypass.
        return stack.isEmpty() || stack.getItem() instanceof SneakBypassUseHook hook
                && hook.doesSneakBypassUse(stack, level, pos, player);
    }
}
