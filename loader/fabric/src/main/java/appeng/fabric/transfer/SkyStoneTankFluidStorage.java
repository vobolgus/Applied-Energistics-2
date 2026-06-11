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

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.stacks.AEFluidKey;
import appeng.blockentity.storage.SkyStoneTankBlockEntity;
import appeng.util.AESnapshotJournal;

/**
 * Exposes the loader-neutral sky stone tank state as a Fabric {@code Storage<FluidVariant>}. The Fabric twin of
 * {@code appeng.blockentity.storage.SkyStoneTankFluidHandler}, with all amount conversions routed through
 * {@link FluidUnits}.
 */
public class SkyStoneTankFluidStorage implements SingleSlotStorage<FluidVariant> {
    private final SkyStoneTankBlockEntity tank;
    private final Journal journal = new Journal();

    private SkyStoneTankFluidStorage(SkyStoneTankBlockEntity tank) {
        this.tank = tank;
    }

    /**
     * Gets the storage for the given tank, reusing the instance cached on the block entity (so that API lookups and
     * in-world interaction share journals within a transaction).
     */
    public static SkyStoneTankFluidStorage get(SkyStoneTankBlockEntity tank) {
        return (SkyStoneTankFluidStorage) tank.getOrCreateFluidHandler(SkyStoneTankFluidStorage::new);
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        int currentAmount = tank.getStoredAmount();

        if (currentAmount == 0 || matches(resource)) {
            int inserted = (int) Math.min(FluidUnits.dropletsToMb(maxAmount), tank.getCapacity() - currentAmount);

            if (inserted > 0) {
                journal.updateSnapshots(transaction);
                tank.setContents(FabricResources.of(resource), currentAmount + inserted);
                return FluidUnits.mbToDroplets(inserted);
            }
        }

        return 0;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);

        if (matches(resource)) {
            int currentAmount = tank.getStoredAmount();
            int extracted = (int) Math.min(FluidUnits.dropletsToMb(maxAmount), currentAmount);

            if (extracted > 0) {
                journal.updateSnapshots(transaction);
                tank.setContents(tank.getStoredFluid(), currentAmount - extracted);
                return FluidUnits.mbToDroplets(extracted);
            }
        }

        return 0;
    }

    private boolean matches(FluidVariant resource) {
        return Objects.equals(FabricResources.of(resource), tank.getStoredFluid());
    }

    @Override
    public boolean isResourceBlank() {
        return tank.getStoredFluid() == null;
    }

    @Override
    public FluidVariant getResource() {
        var fluid = tank.getStoredFluid();
        return fluid == null ? FluidVariant.blank() : FabricResources.toVariant(fluid);
    }

    @Override
    public long getAmount() {
        return FluidUnits.mbToDroplets(tank.getStoredAmount());
    }

    @Override
    public long getCapacity() {
        return FluidUnits.mbToDroplets(tank.getCapacity());
    }

    private record TankSnapshot(@Nullable AEFluidKey fluid, int amount) {
    }

    private class Journal extends AESnapshotJournal<TankSnapshot> {
        @Override
        protected TankSnapshot createSnapshot() {
            return new TankSnapshot(tank.getStoredFluid(), tank.getStoredAmount());
        }

        @Override
        protected void revertToSnapshot(TankSnapshot snapshot) {
            tank.setContents(snapshot.fluid(), snapshot.amount());
        }

        @Override
        protected void onRootCommit(TankSnapshot originalState) {
            tank.onTankContentsChanged();
        }
    }
}
