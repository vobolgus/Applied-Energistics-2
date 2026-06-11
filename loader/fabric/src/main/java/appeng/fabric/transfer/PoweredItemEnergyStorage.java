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

package appeng.fabric.transfer;

import com.google.common.primitives.Ints;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;

import team.reborn.energy.api.EnergyStorage;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.implementations.items.IAEItemPowerStorage;

/**
 * Exposes AE2's chargeable items as Team Reborn {@link EnergyStorage}s. The Fabric twin of
 * {@code appeng.items.tools.powered.powersink.PoweredItemCapabilities}.
 */
public class PoweredItemEnergyStorage implements EnergyStorage {
    private final ContainerItemContext itemAccess;
    private final Item validItem;
    private final IAEItemPowerStorage item;

    public PoweredItemEnergyStorage(ContainerItemContext itemAccess, Item validItem, IAEItemPowerStorage item) {
        this.itemAccess = itemAccess;
        this.validItem = validItem;
        this.item = item;
    }

    @Override
    public long getAmount() {
        var currentItem = itemAccess.getItemVariant();
        if (!currentItem.isOf(validItem)) {
            return 0;
        }
        return (long) PowerUnit.AE.convertTo(PowerUnit.FE, item.getAECurrentPower(currentItem.toStack()));
    }

    @Override
    public long getCapacity() {
        var currentItem = itemAccess.getItemVariant();
        if (!currentItem.isOf(validItem)) {
            return 0;
        }
        return (long) PowerUnit.AE.convertTo(PowerUnit.FE, item.getAEMaxPower(currentItem.toStack()));
    }

    @Override
    public long insert(long amount, TransactionContext transaction) {
        StoragePreconditions.notNegative(amount);

        long accessAmount = itemAccess.getAmount();
        if (accessAmount == 0) {
            return 0;
        }
        long amountPerItem = amount / accessAmount;
        if (amountPerItem == 0) {
            return 0;
        }

        ItemVariant accessVariant = itemAccess.getItemVariant();
        if (!accessVariant.isOf(validItem)) {
            return 0;
        }

        // We'll essentially perform the insertion into a copy of the stack, then convert back to the variant
        var amountAE = PowerUnit.FE.convertTo(PowerUnit.AE, Ints.saturatedCast(amount));
        var mutableStack = accessVariant.toStack();
        double overflowAE = item.injectAEPower(mutableStack, amountAE, Actionable.MODULATE);
        var insertedPerItem = (long) PowerUnit.AE.convertTo(PowerUnit.FE, amountAE - overflowAE);

        insertedPerItem = Math.min(amountPerItem, insertedPerItem);
        if (insertedPerItem > 0) {
            var filledVariant = ItemVariant.of(mutableStack);

            if (!filledVariant.isBlank()) {
                return insertedPerItem * itemAccess.exchange(filledVariant, accessAmount, transaction);
            }
        }

        return 0;
    }

    @Override
    public long extract(long amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }
}
