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

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.me.storage.ExternalStorageFacade;

/**
 * Bundles the per-key-type pieces needed to adapt external Fabric storages: the sided block API lookup, the key/variant
 * conversion and the facade factory. The Fabric twin of {@code appeng.parts.automation.HandlerStrategy}.
 */
public final class FabricHandlerStrategy<V extends TransferVariant<?>> {
    public static final FabricHandlerStrategy<ItemVariant> ITEMS = new FabricHandlerStrategy<>(
            ItemStorage.SIDED, VariantConversion.ITEM);

    public static final FabricHandlerStrategy<FluidVariant> FLUIDS = new FabricHandlerStrategy<>(
            FluidStorage.SIDED, VariantConversion.FLUID);

    private final BlockApiLookup<Storage<V>, Direction> lookup;
    private final VariantConversion<V> conversion;

    private FabricHandlerStrategy(BlockApiLookup<Storage<V>, Direction> lookup, VariantConversion<V> conversion) {
        this.lookup = lookup;
        this.conversion = conversion;
    }

    public BlockApiLookup<Storage<V>, Direction> getLookup() {
        return lookup;
    }

    public VariantConversion<V> getConversion() {
        return conversion;
    }

    public AEKeyType getKeyType() {
        return conversion.getKeyType();
    }

    public boolean isSupported(AEKey what) {
        return what.getType() == conversion.getKeyType();
    }

    public ExternalStorageFacade getFacade(Storage<V> storage) {
        return new FabricStorageFacade<>(storage, conversion);
    }

    /**
     * Inserts the given AE2-internal amount into the storage, returning the AE2-internal amount inserted.
     */
    public long insert(Storage<V> storage, AEKey what, long amount, Actionable mode) {
        var variant = conversion.getVariant(what);
        if (!variant.isBlank() && amount > 0) {
            try (var tx = Transaction.openOuter()) {
                var inserted = storage.insert(variant, conversion.toPlatformAmount(amount), tx);
                if (!mode.isSimulate()) {
                    tx.commit();
                }
                return conversion.toAeAmount(inserted);
            }
        }

        return 0;
    }
}
