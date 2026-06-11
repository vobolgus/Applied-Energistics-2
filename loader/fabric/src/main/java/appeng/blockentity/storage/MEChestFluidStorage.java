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

package appeng.blockentity.storage;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import appeng.fabric.transfer.FabricResources;
import appeng.fabric.transfer.FluidUnits;
import appeng.fabric.transfer.InsertionOnlyStorageWithJournal;

/**
 * The Fabric twin of {@code appeng.blockentity.storage.MEChestFluidHandler}: pushes inserted fluid into the storage
 * cell of the ME chest. Lives in this package because it uses the package-private push methods of
 * {@link MEChestBlockEntity}.
 */
public class MEChestFluidStorage extends InsertionOnlyStorageWithJournal<FluidVariant, GenericStack> {
    private final MEChestBlockEntity blockEntity;

    public MEChestFluidStorage(MEChestBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        if (pendingSideEffect != null) {
            return 0; // Can only insert once per action
        }

        blockEntity.updateHandler();
        if (blockEntity.canAcceptLiquids()) {
            var what = FabricResources.of(resource);
            var inserted = blockEntity.pushFluidToNetwork(what, FluidUnits.dropletsToMbInt(maxAmount),
                    Actionable.SIMULATE);
            if (inserted > 0) {
                updateSnapshots(transaction);
                pendingSideEffect = new GenericStack(what, inserted);
            }
            return FluidUnits.mbToDroplets(inserted);
        }
        return 0;
    }

    @Override
    protected void onRootCommit(GenericStack originalState) {
        blockEntity.pushFluidToNetwork(pendingSideEffect.what(), (int) pendingSideEffect.amount(),
                Actionable.MODULATE);
        pendingSideEffect = null;
    }
}
