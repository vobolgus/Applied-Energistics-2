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

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;

/**
 * Wraps a Fabric {@link Storage} such that it can be used as an {@link InternalInventory}. The Fabric twin of
 * {@code appeng.api.inventories.PlatformInventoryWrapper}.
 * <p>
 * Slot-based access maps onto {@link SlottedStorage} slots when the wrapped storage supports them; for non-slotted
 * storages, only the slot-less {@code addItems}/{@code removeItems} operations work reliably and slot-based access
 * degrades to a best-effort snapshot of the current storage views.
 */
public class StorageInternalInventory implements InternalInventory {
    private final Storage<ItemVariant> storage;

    public StorageInternalInventory(Storage<ItemVariant> storage) {
        this.storage = storage;
    }

    public Storage<ItemVariant> getStorage() {
        return storage;
    }

    @Override
    public int size() {
        if (storage instanceof SlottedStorage<ItemVariant> slotted) {
            return slotted.getSlotCount();
        }
        int count = 0;
        for (var ignored : storage) {
            count++;
        }
        return count;
    }

    @Override
    public int getSlotLimit(int slot) {
        var view = getView(slot);
        return view == null ? 0 : Ints.saturatedCast(view.getCapacity());
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        var view = getView(slotIndex);
        if (view == null || view.isResourceBlank()) {
            return ItemStack.EMPTY;
        }
        return view.getResource().toStack(Ints.saturatedCast(view.getAmount()));
    }

    @Override
    public void setItemDirect(int slotIndex, ItemStack stack) {
        if (storage instanceof SlottedStorage<ItemVariant> slotted) {
            var slot = slotted.getSlot(slotIndex);
            try (var tx = Transaction.openOuter()) {
                if (!slot.isResourceBlank()) {
                    slot.extract(slot.getResource(), slot.getAmount(), tx);
                }
                if (!stack.isEmpty()) {
                    slot.insert(ItemVariant.of(stack), stack.getCount(), tx);
                }
                tx.commit();
            }
        } else {
            // Best effort for non-slotted storages: replace the content of the given view.
            try (var tx = Transaction.openOuter()) {
                var view = getView(slotIndex);
                if (view != null && !view.isResourceBlank()) {
                    view.extract(view.getResource(), view.getAmount(), tx);
                }
                if (!stack.isEmpty()) {
                    storage.insert(ItemVariant.of(stack), stack.getCount(), tx);
                }
                tx.commit();
            }
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // The Fabric transfer API has no validity query; simulate an insertion instead.
        if (stack.isEmpty()) {
            return true;
        }
        try (var tx = Transaction.openOuter()) {
            return insertIntoSlot(slot, ItemVariant.of(stack), 1, tx) > 0;
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (var tx = Transaction.openOuter()) {
            var inserted = insertIntoSlot(slot, ItemVariant.of(stack), stack.getCount(), tx);
            if (!simulate) {
                tx.commit();
            }
            return stack.copyWithCount(stack.getCount() - Ints.saturatedCast(inserted));
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        try (var tx = Transaction.openOuter()) {
            var view = getView(slot);
            if (view == null || view.isResourceBlank()) {
                return ItemStack.EMPTY;
            }
            var resource = view.getResource();
            var extracted = view.extract(resource, amount, tx);
            if (!simulate) {
                tx.commit();
            }
            return resource.toStack(Ints.saturatedCast(extracted));
        }
    }

    private long insertIntoSlot(int slot, ItemVariant variant, long amount,
            net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx) {
        if (storage instanceof SlottedStorage<ItemVariant> slotted) {
            return slotted.getSlot(slot).insert(variant, amount, tx);
        }
        // Non-slotted: insertion is not slot-addressable.
        return storage.insert(variant, amount, tx);
    }

    @org.jetbrains.annotations.Nullable
    private net.fabricmc.fabric.api.transfer.v1.storage.StorageView<ItemVariant> getView(int index) {
        if (storage instanceof SlottedStorage<ItemVariant> slotted) {
            if (index < 0 || index >= slotted.getSlotCount()) {
                return null;
            }
            return slotted.getSlot(index);
        }
        int i = 0;
        for (var view : storage) {
            if (i++ == index) {
                return view;
            }
        }
        return null;
    }

    /**
     * Slot-less bulk add, overriding the default per-slot loop with a direct storage insert (works for non-slotted
     * storages too).
     */
    @Override
    public ItemStack addItems(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (var tx = Transaction.openOuter()) {
            var inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), tx);
            if (!simulate) {
                tx.commit();
            }
            return stack.copyWithCount(stack.getCount() - Ints.saturatedCast(inserted));
        }
    }

}
