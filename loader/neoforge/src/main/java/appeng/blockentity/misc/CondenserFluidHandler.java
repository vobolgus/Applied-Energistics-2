package appeng.blockentity.misc;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKeyType;
import appeng.util.InsertionOnlyResourceHandlerWithJournal;

/**
 * Exposes the condenser's void slot as a fluid handler. This used to be an inner class of {@link CondenserBlockEntity}
 * ({@code CondenseResourceHandler}) before the block entity was decoupled from loader types. The handler is stateless
 * across transactions, so it can be created on demand.
 */
public class CondenserFluidHandler extends InsertionOnlyResourceHandlerWithJournal<FluidResource, Double> {
    private static final double ENERGY_FACTOR = 1.0 / AEKeyType.fluids().getAmountPerOperation();
    private static final int MAX_AMOUNT_PER_OPERATION = AEFluidKey.AMOUNT_BUCKET;

    private final CondenserBlockEntity blockEntity;

    public CondenserFluidHandler(CondenserBlockEntity blockEntity) {
        super(FluidResource.EMPTY);
        this.blockEntity = blockEntity;
        this.pendingSideEffect = 0D;
    }

    @Override
    public int insert(FluidResource resource, int maxAmount, TransactionContext transaction) {
        // Clamp the amount per operation
        var amount = Math.min(MAX_AMOUNT_PER_OPERATION, maxAmount);
        updateSnapshots(transaction);
        pendingSideEffect += amount * ENERGY_FACTOR;
        return amount;
    }

    @Override
    protected void onRootCommit(Double originalState) {
        blockEntity.addPower(pendingSideEffect);
        pendingSideEffect = 0.0;
    }
}
