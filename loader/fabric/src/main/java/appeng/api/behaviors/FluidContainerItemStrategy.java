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

package appeng.api.behaviors;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.fabric.transfer.FabricResources;
import appeng.fabric.transfer.FluidUnits;
import appeng.util.GenericContainerHelper;
import appeng.util.fluid.FluidSoundHelper;

/**
 * The Fabric twin of the NeoForge overlay class of the same name, exposing fluid container items (buckets, tanks)
 * through the Fabric transfer API. The context type is the fluid {@link Storage} of the container item.
 */
class FluidContainerItemStrategy implements ContainerItemStrategy<AEFluidKey, Storage<FluidVariant>> {
    @Override
    public @Nullable GenericStack getContainedStack(ItemStack stack) {
        return GenericContainerHelper.getContainedFluidStack(stack);
    }

    @Override
    public @Nullable Storage<FluidVariant> findCarriedContext(Player player, AbstractContainerMenu menu) {
        return ContainerItemContext.ofPlayerCursor(player, menu).find(FluidStorage.ITEM);
    }

    @Override
    public @Nullable Storage<FluidVariant> findPlayerSlotContext(Player player, int slot) {
        var slotStorage = PlayerInventoryStorage.of(player).getSlots().get(slot);
        return ContainerItemContext.ofPlayerSlot(player, slotStorage).find(FluidStorage.ITEM);
    }

    @Override
    public long extract(Storage<FluidVariant> context, AEFluidKey what, long amount, Actionable mode) {
        try (var tx = Transaction.openOuter()) {
            var extracted = context.extract(FabricResources.toVariant(what), FluidUnits.mbToDroplets(amount), tx);
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return FluidUnits.dropletsToMb(extracted);
        }
    }

    @Override
    public long insert(Storage<FluidVariant> context, AEFluidKey what, long amount, Actionable mode) {
        try (var tx = Transaction.openOuter()) {
            var inserted = context.insert(FabricResources.toVariant(what), FluidUnits.mbToDroplets(amount), tx);
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return FluidUnits.dropletsToMb(inserted);
        }
    }

    @Override
    public void playFillSound(Player player, AEFluidKey what) {
        FluidSoundHelper.playFillSound(player, what);
    }

    @Override
    public void playEmptySound(Player player, AEFluidKey what) {
        FluidSoundHelper.playEmptySound(player, what);
    }

    @Override
    public @Nullable GenericStack getExtractableContent(Storage<FluidVariant> context) {
        try (var tx = Transaction.openOuter()) {
            var content = StorageUtil.findExtractableContent(context, tx);
            if (content != null) {
                return FabricResources.fromFluidVariant(content.resource(), content.amount());
            }
        }
        return null;
    }
}
