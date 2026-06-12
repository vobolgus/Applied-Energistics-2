package appeng.client.integrations.jei;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.fabric.ingredients.fluids.JeiFluidIngredient;
import mezz.jei.api.ingredients.IIngredientType;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.client.api.integrations.jei.IngredientConverter;
import appeng.fabric.transfer.FabricResources;
import appeng.fabric.transfer.FluidUnits;

/**
 * Fabric twin of the NeoForge overlay class with the same FQN. JEI's Fabric fluid ingredient amounts are droplets;
 * conversion to/from AE2's internal millibuckets goes through {@link FluidUnits} (droplets to mB rounds down).
 */
public class FluidIngredientConverter implements IngredientConverter<IJeiFluidIngredient> {
    @Override
    public IIngredientType<IJeiFluidIngredient> getIngredientType() {
        return FabricTypes.FLUID_STACK;
    }

    @Nullable
    @Override
    public IJeiFluidIngredient getIngredientFromStack(GenericStack stack) {
        if (stack.what() instanceof AEFluidKey fluidKey) {
            return new JeiFluidIngredient(FabricResources.toVariant(fluidKey),
                    Math.max(1, FluidUnits.mbToDroplets(stack.amount())));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public GenericStack getStackFromIngredient(IJeiFluidIngredient ingredient) {
        var key = FabricResources.of(ingredient.getFluidVariant());
        if (key == null) {
            return null;
        }
        return new GenericStack(key, FluidUnits.dropletsToMb(ingredient.getAmount()));
    }
}
