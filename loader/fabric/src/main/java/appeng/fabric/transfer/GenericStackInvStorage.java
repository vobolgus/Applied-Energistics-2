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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;

/**
 * Exposes a {@link GenericInternalInventory} as a Fabric {@link SlottedStorage} of a single key type. The Fabric twin
 * of {@code appeng.helpers.externalstorage.GenericStackInvHandler}, with all amount conversions (millibuckets vs
 * droplets) routed through the {@link VariantConversion}.
 */
public class GenericStackInvStorage<V extends TransferVariant<?>> implements SlottedStorage<V> {
    private final VariantConversion<V> conversion;
    private final GenericInternalInventory inv;
    private final AEKeyType channel;
    private final List<View> views;

    public GenericStackInvStorage(VariantConversion<V> conversion, AEKeyType channel,
            GenericInternalInventory inv) {
        this.conversion = conversion;
        this.channel = channel;
        this.inv = inv;
        this.views = new ArrayList<>(inv.size());
        for (int i = 0; i < inv.size(); i++) {
            views.add(new View(i));
        }
    }

    /**
     * Checks if the slot represented by this view is actually supported by the channel.
     */
    private boolean isSupportedSlot(int index) {
        var key = inv.getKey(index);
        return key == null || channel.tryCast(key) != null;
    }

    @Override
    public long insert(V resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (!inv.canInsert()) {
            return 0;
        }
        long totalInserted = 0;

        // First iteration matches the resource, second iteration inserts into empty slots.
        int size = getSlotCount();
        for (int i = 0; i < size; i++) {
            if (inv.getKey(i) != null) {
                totalInserted += views.get(i).insert(resource, maxAmount - totalInserted, transaction);
                if (totalInserted >= maxAmount) {
                    break;
                }
            }
        }

        for (int i = 0; i < size; i++) {
            if (inv.getKey(i) == null) {
                totalInserted += views.get(i).insert(resource, maxAmount - totalInserted, transaction);
                if (totalInserted >= maxAmount) {
                    break;
                }
            }
        }

        return totalInserted;
    }

    @Override
    public long extract(V resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (!inv.canExtract()) {
            return 0;
        }
        long totalExtracted = 0;
        for (var view : views) {
            totalExtracted += view.extract(resource, maxAmount - totalExtracted, transaction);
            if (totalExtracted >= maxAmount) {
                break;
            }
        }
        return totalExtracted;
    }

    @Override
    public int getSlotCount() {
        return inv.size();
    }

    @Override
    public SingleSlotStorage<V> getSlot(int slot) {
        return views.get(slot);
    }

    @Override
    public Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<V>> iterator() {
        return List.<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<V>>copyOf(views).iterator();
    }

    private class View implements SingleSlotStorage<V> {
        private final int index;

        View(int index) {
            this.index = index;
        }

        @Override
        public long insert(V resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!inv.canInsert()) {
                return 0;
            }

            var currentKey = inv.getKey(index);
            var key = conversion.getKey(resource);
            if ((currentKey == null && inv.isAllowedIn(index, key)) || (currentKey != null && currentKey.equals(key))) {
                long currentAmount = getAeAmount();
                long inserted = Math.min(conversion.toAeAmount(maxAmount), inv.getMaxAmount(key) - currentAmount);

                if (inserted > 0) {
                    inv.updateSnapshots(FabricTransaction.of(transaction));
                    inv.beginBatch();
                    inv.setStack(index, new GenericStack(key, currentAmount + inserted));
                    inv.endBatchSuppressed();
                    return conversion.toPlatformAmount(inserted);
                }
            }

            return 0;
        }

        @Override
        public long extract(V resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!inv.canExtract() || !getResource().equals(resource)) {
                return 0;
            }

            long actuallyExtracted = Math.min(getAeAmount(), conversion.toAeAmount(maxAmount));

            if (actuallyExtracted > 0) {
                inv.updateSnapshots(FabricTransaction.of(transaction));
                var remainder = getAeAmount() - actuallyExtracted;
                inv.beginBatch();
                if (remainder <= 0) {
                    inv.setStack(index, null);
                } else {
                    inv.setStack(index, new GenericStack(conversion.getKey(resource), remainder));
                }
                inv.endBatchSuppressed();
                return conversion.toPlatformAmount(actuallyExtracted);
            }

            return 0;
        }

        private long getAeAmount() {
            if (!isSupportedSlot(index)) {
                return 0;
            }
            return inv.getAmount(index);
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public V getResource() {
            return conversion.getVariant(inv.getKey(index));
        }

        @Override
        public long getAmount() {
            return conversion.toPlatformAmount(getAeAmount());
        }

        @Override
        public long getCapacity() {
            if (!isSupportedSlot(index)) {
                return 0;
            }
            var key = inv.getKey(index);
            if (key != null) {
                return conversion.toPlatformAmount(inv.getMaxAmount(key));
            }
            return conversion.toPlatformAmount(inv.getCapacity(conversion.getKeyType()));
        }
    }
}
