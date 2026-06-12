package appeng.client.integrations.jei;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.neoforge.NeoForgeTypes;

/**
 * Loader-duplicated JEI glue (a Fabric twin with the same FQN exists in the Fabric loader project): installs AE2's
 * fluid-as-block renderer on a recipe slot. The JEI fluid ingredient type is loader-specific (NeoForge
 * {@code FluidStack} vs Fabric {@code IJeiFluidIngredient}), so the type/renderer pair cannot be named from shared
 * code.
 */
public final class JeiFluidRendering {
    private static final FluidBlockRenderer RENDERER = new FluidBlockRenderer();

    private JeiFluidRendering() {
    }

    public static void setFluidSlotRenderer(IRecipeSlotBuilder slot) {
        slot.setCustomRenderer(NeoForgeTypes.FLUID_STACK, RENDERER);
    }
}
