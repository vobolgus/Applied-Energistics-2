package appeng.blockentity.storage;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import appeng.api.stacks.AEFluidKey;
import appeng.neoforge.transfer.NeoForgeResources;

/**
 * Exposes the sky stone tank's contents as a NeoForge fluid {@code ResourceHandler}. The tank used to be a
 * {@code FluidStacksResourceHandler} field of {@link SkyStoneTankBlockEntity} before the block entity was decoupled
 * from loader types; this adapter replicates the single-slot behavior of that handler on top of the loader-neutral tank
 * state.
 */
public class SkyStoneTankFluidHandler implements ResourceHandler<FluidResource> {
    private final SkyStoneTankBlockEntity tank;
    private final Journal journal = new Journal();

    public SkyStoneTankFluidHandler(SkyStoneTankBlockEntity tank) {
        this.tank = tank;
    }

    /**
     * Gets the handler for the given tank, reusing the instance cached on the block entity (so that capability lookups
     * and in-world interaction share journals within a transaction).
     */
    public static SkyStoneTankFluidHandler get(SkyStoneTankBlockEntity tank) {
        return (SkyStoneTankFluidHandler) tank.getOrCreateFluidHandler(SkyStoneTankFluidHandler::new);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, size());
        var fluid = tank.getStoredFluid();
        return fluid == null ? FluidResource.EMPTY : NeoForgeResources.toResource(fluid);
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        return tank.getStoredAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return tank.getCapacity();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return true;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        int currentAmount = tank.getStoredAmount();

        if (currentAmount == 0 || matches(resource)) {
            int inserted = Math.min(amount, tank.getCapacity() - currentAmount);

            if (inserted > 0) {
                journal.updateSnapshots(transaction);
                tank.setContents(NeoForgeResources.of(resource), currentAmount + inserted);
                return inserted;
            }
        }

        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (matches(resource)) {
            int currentAmount = tank.getStoredAmount();
            int extracted = Math.min(amount, currentAmount);

            if (extracted > 0) {
                journal.updateSnapshots(transaction);
                tank.setContents(tank.getStoredFluid(), currentAmount - extracted);
                return extracted;
            }
        }

        return 0;
    }

    private boolean matches(FluidResource resource) {
        return Objects.equals(NeoForgeResources.of(resource), tank.getStoredFluid());
    }

    private record TankSnapshot(@Nullable AEFluidKey fluid, int amount) {
    }

    private class Journal extends SnapshotJournal<TankSnapshot> {
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
