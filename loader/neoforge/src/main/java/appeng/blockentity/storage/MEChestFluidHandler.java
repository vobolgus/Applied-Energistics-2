package appeng.blockentity.storage;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import appeng.neoforge.transfer.NeoForgeResources;
import appeng.util.InsertionOnlyResourceHandlerWithJournal;

/**
 * Pushes fluids inserted into an ME chest into its inserted storage cell. This used to be an inner class of
 * {@link MEChestBlockEntity} ({@code FluidHandler}) before the block entity was decoupled from loader types.
 */
public class MEChestFluidHandler extends InsertionOnlyResourceHandlerWithJournal<FluidResource, GenericStack> {
    private final MEChestBlockEntity blockEntity;

    public MEChestFluidHandler(MEChestBlockEntity blockEntity) {
        super(FluidResource.EMPTY);
        this.blockEntity = blockEntity;
    }

    @Override
    public int insert(FluidResource resource, int maxAmount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, maxAmount);

        if (pendingSideEffect != null) {
            return 0; // Can only insert once per action
        }

        blockEntity.updateHandler();
        if (blockEntity.canAcceptLiquids()) {
            var what = NeoForgeResources.of(resource);
            var inserted = blockEntity.pushFluidToNetwork(what, maxAmount, Actionable.SIMULATE);
            if (inserted > 0) {
                updateSnapshots(transaction);
                pendingSideEffect = new GenericStack(what, inserted);
            }
            return inserted;
        }
        return 0;
    }

    @Override
    public int size() {
        if (!blockEntity.canAcceptLiquids()) {
            return 0;
        }
        return super.size();
    }

    @Override
    protected void onRootCommit(GenericStack originalState) {
        blockEntity.pushFluidToNetwork(pendingSideEffect.what(), (int) pendingSideEffect.amount(),
                Actionable.MODULATE);
        pendingSideEffect = null;
    }
}
