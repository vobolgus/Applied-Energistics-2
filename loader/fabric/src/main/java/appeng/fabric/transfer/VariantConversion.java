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
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;

/**
 * Conversion between {@link AEKey}s and Fabric transfer variants, including the conversion between AE2's internal
 * amounts (items, millibuckets) and the platform's amounts (items, droplets). The Fabric twin of
 * {@code appeng.helpers.ResourceConversion}.
 */
public interface VariantConversion<V extends TransferVariant<?>> {
    VariantConversion<ItemVariant> ITEM = new Item();
    VariantConversion<FluidVariant> FLUID = new Fluid();

    AEKeyType getKeyType();

    /**
     * Convert key to variant. If the key is null or of the wrong type, return a blank variant.
     */
    V getVariant(@Nullable AEKey key);

    @Nullable
    AEKey getKey(V variant);

    default boolean variantMatches(AEKey key, V variant) {
        return getVariant(key).equals(variant);
    }

    long getBaseSlotSize(V variant);

    /**
     * Converts an AE2-internal amount (items / millibuckets) into the platform amount (items / droplets).
     */
    long toPlatformAmount(long aeAmount);

    /**
     * Converts a platform amount (items / droplets) into the AE2-internal amount (items / millibuckets), rounding down
     * for fluids (see {@link FluidUnits}).
     */
    long toAeAmount(long platformAmount);

    class Fluid implements VariantConversion<FluidVariant> {
        @Override
        public AEKeyType getKeyType() {
            return AEKeyType.fluids();
        }

        @Override
        public FluidVariant getVariant(AEKey key) {
            return key instanceof AEFluidKey fluidKey ? FabricResources.toVariant(fluidKey) : FluidVariant.blank();
        }

        @Override
        public AEKey getKey(FluidVariant variant) {
            return FabricResources.of(variant);
        }

        @Override
        public long getBaseSlotSize(FluidVariant variant) {
            return toPlatformAmount(4L * AEFluidKey.AMOUNT_BUCKET);
        }

        @Override
        public long toPlatformAmount(long aeAmount) {
            return FluidUnits.mbToDroplets(aeAmount);
        }

        @Override
        public long toAeAmount(long platformAmount) {
            return FluidUnits.dropletsToMb(platformAmount);
        }
    }

    class Item implements VariantConversion<ItemVariant> {
        @Override
        public AEKeyType getKeyType() {
            return AEKeyType.items();
        }

        @Override
        public ItemVariant getVariant(AEKey key) {
            return key instanceof AEItemKey itemKey ? FabricResources.toVariant(itemKey) : ItemVariant.blank();
        }

        @Nullable
        @Override
        public AEItemKey getKey(ItemVariant variant) {
            return FabricResources.of(variant);
        }

        @Override
        public long getBaseSlotSize(ItemVariant variant) {
            var stack = variant.toStack();
            return Math.min(64, stack.getMaxStackSize());
        }

        @Override
        public long toPlatformAmount(long aeAmount) {
            return aeAmount;
        }

        @Override
        public long toAeAmount(long platformAmount) {
            return platformAmount;
        }
    }
}
