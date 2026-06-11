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

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.inventories.ItemTransfer;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigMenuInventory;
import appeng.util.inv.CarriedItemInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import appeng.util.inv.SupplierInternalInventory;

/**
 * Static bridges between AE2's loader-neutral key/inventory abstractions and the Fabric transfer API. The Fabric twin
 * of {@code appeng.neoforge.transfer.NeoForgeResources}.
 */
public final class FabricResources {
    private FabricResources() {
    }

    public static ItemVariant toVariant(AEItemKey key) {
        return ItemVariant.of(key.getReadOnlyStack());
    }

    public static FluidVariant toVariant(AEFluidKey key) {
        return FluidVariant.of(key.getFluid(), key.getComponentsPatch());
    }

    @Nullable
    public static AEItemKey of(ItemVariant variant) {
        if (variant.isBlank()) {
            return null;
        }
        return AEItemKey.of(variant.toStack());
    }

    @Nullable
    public static AEFluidKey of(FluidVariant variant) {
        if (variant.isBlank()) {
            return null;
        }
        return AEFluidKey.of(variant.getFluid(), variant.getComponentsPatch());
    }

    /**
     * Converts a given item variant and amount into a generic stack. If the variant is blank, null is returned.
     */
    @Nullable
    public static GenericStack fromItemVariant(ItemVariant variant, long amount) {
        var key = of(variant);
        if (key == null) {
            return null;
        }
        return new GenericStack(key, amount);
    }

    /**
     * Converts a given fluid variant and <strong>droplet</strong> amount into a generic stack with AE2's internal
     * millibucket amount. If the variant is blank, null is returned.
     */
    @Nullable
    public static GenericStack fromFluidVariant(FluidVariant variant, long droplets) {
        var key = of(variant);
        if (key == null) {
            return null;
        }
        return new GenericStack(key, FluidUnits.dropletsToMb(droplets));
    }

    /**
     * Exposes an adjacent block's item storage as an {@link ItemTransfer}, if there is one.
     */
    @Nullable
    public static ItemTransfer wrapExternal(Level level, BlockPos pos, Direction side) {
        var storage = ItemStorage.SIDED.find(level, pos, side);
        if (storage != null) {
            return new StorageInternalInventory(storage);
        }
        return null;
    }

    /**
     * Adapts an {@link InternalInventory} to the Fabric {@link Storage} interface. Replicates the dispatch of the
     * NeoForge twin ({@code NeoForgeResources.toResourceHandler}). Adapters for {@link BaseInternalInventory}
     * subclasses are cached on the inventory to maintain referential equality over time.
     */
    @SuppressWarnings("unchecked")
    public static Storage<ItemVariant> toStorage(InternalInventory inv) {
        if (inv instanceof SupplierInternalInventory<?> supplier) {
            return toStorage(supplier.getDelegate());
        }
        if (inv instanceof StorageInternalInventory wrapper) {
            return wrapper.getStorage();
        }
        if (inv instanceof PlayerInternalInventory playerInv) {
            return PlayerInventoryStorage.of(playerInv.getInventory());
        }
        if (inv instanceof CarriedItemInventory carriedInv) {
            return PlayerInventoryStorage.getCursorStorage(carriedInv.getMenu());
        }
        if (inv instanceof ConfigMenuInventory) {
            throw new UnsupportedOperationException();
        }
        if (inv == InternalInventory.empty()) {
            return Storage.empty();
        }
        if (inv instanceof CombinedInternalInventory combined) {
            return (Storage<ItemVariant>) combined.getOrCreatePlatformAdapter(ignored -> {
                var inventories = combined.getInventories();
                var parts = new java.util.ArrayList<Storage<ItemVariant>>(inventories.length);
                for (var inventory : inventories) {
                    parts.add(toStorage(inventory));
                }
                return new net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage<>(parts);
            });
        }
        if (inv instanceof BaseInternalInventory base) {
            return (Storage<ItemVariant>) base.getOrCreatePlatformAdapter(InternalInventoryStorage::new);
        }
        return new InternalInventoryStorage(inv);
    }
}
