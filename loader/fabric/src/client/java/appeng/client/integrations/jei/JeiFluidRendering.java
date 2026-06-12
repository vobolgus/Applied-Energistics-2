package appeng.client.integrations.jei;

import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;

/**
 * Fabric twin of the loader-duplicated JEI glue (same FQN in the NeoForge loader project): installs AE2's
 * fluid-as-block renderer on a recipe slot. The JEI fluid ingredient type is loader-specific (NeoForge
 * {@code FluidStack} vs Fabric {@code IJeiFluidIngredient}), so the type/renderer pair cannot be named from shared
 * code.
 */
public final class JeiFluidRendering {
    private static final FluidBlockRenderer RENDERER = new FluidBlockRenderer();

    private JeiFluidRendering() {
    }

    public static void setFluidSlotRenderer(IRecipeSlotBuilder slot) {
        slot.setCustomRenderer(FabricTypes.FLUID_STACK, RENDERER);
    }
}
