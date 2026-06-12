package appeng.integration.modules.rei;

import org.jetbrains.annotations.Nullable;

import dev.architectury.fluid.FluidStack;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;

import appeng.api.integrations.rei.IngredientConverter;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;

/**
 * Converts between AE2 fluid stacks (internal unit: millibuckets, {@link AEFluidKey#AMOUNT_BUCKET} per bucket) and
 * REI's fluid entry type ({@link FluidStack dev.architectury FluidStack}, whose amounts are in the current platform's
 * native unit: millibuckets on NeoForge, droplets on Fabric).
 * <p>
 * The unit conversion is loader-neutral by construction: {@link FluidStack#bucketAmount()} reports the platform's
 * units-per-bucket at runtime, so this shared class needs no loader seam. (The Fabric transfer API conversions have
 * their own choke point in {@code appeng.fabric.transfer.FluidUnits}; this here is the architectury/REI boundary, not
 * the transfer-API boundary.) Rounding follows the same policy: REI -> AE2 rounds down so AE2 never sees more fluid
 * than the displayed amount represents.
 */
public class FluidIngredientConverter implements IngredientConverter<FluidStack> {
    @Override
    public EntryType<FluidStack> getIngredientType() {
        return VanillaEntryTypes.FLUID;
    }

    @Nullable
    @Override
    public EntryStack<FluidStack> getIngredientFromStack(GenericStack stack) {
        if (stack.what() instanceof AEFluidKey fluidKey) {
            // was: FluidStackHooksForge.fromForge(fluidKey.toStack(1)).copyWithAmount(...) - both the forge-only
            // hook and AEFluidKey.toStack(int) are gone; build the architectury stack directly.
            return EntryStack.of(getIngredientType(), FluidStack.create(
                    fluidKey.getFluid(),
                    Math.max(1, toReiAmount(stack.amount())),
                    fluidKey.getComponentsPatch()));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public GenericStack getStackFromIngredient(EntryStack<FluidStack> ingredient) {
        if (ingredient.getType() == getIngredientType()) {
            FluidStack fluidStack = ingredient.castValue();
            return new GenericStack(
                    AEFluidKey.of(fluidStack.getFluid(), fluidStack.getPatch()),
                    toAe2Amount(fluidStack.getAmount()));
        }
        return null;
    }

    /**
     * AE2-internal millibuckets to platform-native REI/architectury units (exact; the platform unit is always a
     * multiple of a millibucket).
     */
    private static long toReiAmount(long ae2Amount) {
        return ae2Amount * FluidStack.bucketAmount() / AEFluidKey.AMOUNT_BUCKET;
    }

    /**
     * Platform-native REI/architectury units to AE2-internal millibuckets, rounding down.
     */
    private static long toAe2Amount(long reiAmount) {
        return reiAmount * AEFluidKey.AMOUNT_BUCKET / FluidStack.bucketAmount();
    }
}
