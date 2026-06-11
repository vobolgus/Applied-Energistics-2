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

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKeyType;
import appeng.blockentity.misc.CondenserBlockEntity;

/**
 * The Fabric twin of {@code appeng.blockentity.misc.CondenserFluidHandler}: voids all inserted fluid and converts it to
 * condenser power. Energy is credited per AE2-internal millibucket.
 */
public class CondenserFluidStorage extends InsertionOnlyStorageWithJournal<FluidVariant, Double> {
    private static final double ENERGY_FACTOR = 1.0 / AEKeyType.fluids().getAmountPerOperation();
    private static final long MAX_AMOUNT_PER_OPERATION = AEFluidKey.AMOUNT_BUCKET;

    private final CondenserBlockEntity blockEntity;

    public CondenserFluidStorage(CondenserBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.pendingSideEffect = 0D;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        // Clamp the amount per operation (in AE2-internal millibuckets, mirroring the NeoForge twin)
        var amount = Math.min(MAX_AMOUNT_PER_OPERATION, FluidUnits.dropletsToMb(maxAmount));
        if (amount <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pendingSideEffect += amount * ENERGY_FACTOR;
        return FluidUnits.mbToDroplets(amount);
    }

    @Override
    protected void onRootCommit(Double originalState) {
        blockEntity.addPower(pendingSideEffect);
        pendingSideEffect = 0.0;
    }
}
