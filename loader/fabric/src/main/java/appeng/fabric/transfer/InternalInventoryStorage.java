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

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

import com.google.common.primitives.Ints;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.core.definitions.AEItems;
import appeng.util.AESnapshotJournal;

/**
 * Adapts an {@link InternalInventory} to the Fabric {@link SlottedStorage} interface. The Fabric twin of
 * {@code appeng.api.inventories.InternalInventoryResourceHandler}, with identical snapshot/journal semantics.
 */
public class InternalInventoryStorage extends AESnapshotJournal<InternalInventoryStorage.Snapshot>
        implements SlottedStorage<ItemVariant> {
    private final InternalInventory inventory;
    @Nullable
    private Snapshot lastReleasedSnapshot;
    @Nullable
    private List<SingleSlotStorage<ItemVariant>> slots;

    public InternalInventoryStorage(InternalInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        var stack = resource.toStack(Ints.saturatedCast(maxAmount));

        updateSnapshots(transaction);

        var overflow = inventory.addItems(stack);
        return stack.getCount() - overflow.getCount();
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        // Do not allow extraction of wrapped fluid stacks because they're an internal detail
        if (resource.getItem() == AEItems.WRAPPED_GENERIC_STACK.asItem()) {
            return 0;
        }

        updateSnapshots(transaction);

        ItemStack extracted = inventory.removeItems(Ints.saturatedCast(maxAmount), resource.toStack(), null);

        return extracted.getCount();
    }

    @Override
    public int getSlotCount() {
        return inventory.size();
    }

    @Override
    public SingleSlotStorage<ItemVariant> getSlot(int slot) {
        return new SlotStorage(slot);
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        // Storage's iterator contract: iterate the per-slot views in slot order.
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < getSlotCount();
            }

            @Override
            public StorageView<ItemVariant> next() {
                return getSlot(index++);
            }
        };
    }

    @Override
    public List<SingleSlotStorage<ItemVariant>> getSlots() {
        if (slots == null) {
            slots = new AbstractList<>() {
                @Override
                public SingleSlotStorage<ItemVariant> get(int index) {
                    return getSlot(index);
                }

                @Override
                public int size() {
                    return getSlotCount();
                }
            };
        }
        return slots;
    }

    private class SlotStorage implements SingleSlotStorage<ItemVariant> {
        private final int index;

        SlotStorage(int index) {
            this.index = index;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);

            updateSnapshots(transaction);

            var stack = resource.toStack(Ints.saturatedCast(maxAmount));
            var overflow = inventory.insertItem(index, stack, false).getCount();
            return stack.getCount() - overflow;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);

            // Do not allow extraction of wrapped fluid stacks because they're an internal detail
            if (resource.getItem() == AEItems.WRAPPED_GENERIC_STACK.asItem()) {
                return 0;
            }

            // Unlike the NeoForge ResourceHandler twin, the Fabric StorageView contract requires only
            // extracting content matching the requested resource.
            if (!resource.matches(inventory.getStackInSlot(index))) {
                return 0;
            }

            updateSnapshots(transaction);

            return inventory.extractItem(index, Ints.saturatedCast(maxAmount), false).getCount();
        }

        @Override
        public boolean isResourceBlank() {
            return inventory.getStackInSlot(index).isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            return ItemVariant.of(inventory.getStackInSlot(index));
        }

        @Override
        public long getAmount() {
            return inventory.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacity() {
            return inventory.getSlotLimit(index);
        }
    }

    @Override
    protected Snapshot createSnapshot() {
        Snapshot snapshot;
        if (this.lastReleasedSnapshot != null && this.lastReleasedSnapshot.items.length == inventory.size()) {
            snapshot = this.lastReleasedSnapshot;
            this.lastReleasedSnapshot = null;
        } else {
            snapshot = new Snapshot();
        }

        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getStackInSlot(i);
            snapshot.items[i] = stack;
            snapshot.counts[i] = stack.getCount();
        }
        return snapshot;
    }

    @Override
    protected void revertToSnapshot(Snapshot snapshot) {
        var items = snapshot.items;
        var counts = snapshot.counts;
        for (int i = 0; i < items.length; i++) {
            var stack = items[i];
            // Restore the previous count as well, the inventory might mutate the stack count for extract/insert
            if (stack.getCount() != counts[i]) {
                stack.setCount(counts[i]);
            }
            inventory.setItemDirect(i, stack);
        }
    }

    @Override
    protected void releaseSnapshot(Snapshot snapshot) {
        this.lastReleasedSnapshot = snapshot;
    }

    public class Snapshot {
        final ItemStack[] items;
        final int[] counts;

        public Snapshot() {
            this.items = new ItemStack[inventory.size()];
            this.counts = new int[inventory.size()];
        }
    }

    @Override
    protected void onRootCommit(Snapshot original) {
        for (int i = 0; i < original.items.length; i++) {
            var current = inventory.getStackInSlot(i);
            if (current != original.items[i] || current.getCount() != original.counts[i]) {
                inventory.sendChangeNotification(i);
            }
        }
    }
}
