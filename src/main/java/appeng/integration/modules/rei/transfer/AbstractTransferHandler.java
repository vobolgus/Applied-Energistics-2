package appeng.integration.modules.rei.transfer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;

import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.AEBaseMenu;

public abstract class AbstractTransferHandler<T extends AEBaseMenu> implements TransferHandler {
    protected static final int CRAFTING_GRID_WIDTH = 3;
    protected static final int CRAFTING_GRID_HEIGHT = 3;

    private final Class<T> containerClass;

    AbstractTransferHandler(Class<T> containerClass) {
        this.containerClass = containerClass;
    }

    protected abstract Result transferRecipe(T menu,
            @Nullable RecipeHolder<?> holder,
            Display display,
            boolean doTransfer);

    @Override
    public final Result handle(Context context) {
        if (!containerClass.isInstance(context.getMenu())) {
            return Result.createNotApplicable();
        }

        var display = context.getDisplay();

        T menu = containerClass.cast(context.getMenu());

        var holder = getRecipeHolder(display);

        return transferRecipe(menu, holder, display, context.isActuallyCrafting());
    }

    @Nullable
    private RecipeHolder<?> getRecipeHolder(Display display) {
        // Displays can be based on completely custom objects, or on actual Vanilla recipes
        var origin = DisplayRegistry.getInstance().getDisplayOrigin(display);

        return origin instanceof RecipeHolder<?> holder ? holder : null;
    }

    protected final boolean isCraftingRecipe(@Nullable Recipe<?> recipe, Display display) {
        return EncodingHelper.isSupportedCraftingRecipe(recipe)
                // was: a hand-built CategoryIdentifier.of("minecraft", "plugins/crafting")
                || display.getCategoryIdentifier().equals(BuiltinPlugin.CRAFTING);
    }

    protected final boolean fitsIn3x3Grid(@Nullable Recipe<?> recipe, Display display) {
        if (recipe != null) {
            // was: recipe.canCraftInDimensions(3, 3) (TODO 1.21.4) - the vanilla method is gone; mirror the
            // JEI integration: a placeable recipe with at most 9 placement slots fits the crafting grid.
            return !recipe.placementInfo().isImpossibleToPlace()
                    && recipe.placementInfo().slotsToIngredientIndex().size() <= CRAFTING_GRID_WIDTH
                            * CRAFTING_GRID_HEIGHT;
        } else if (display instanceof SimpleGridMenuDisplay gridDisplay) {
            return gridDisplay.getWidth() <= CRAFTING_GRID_WIDTH && gridDisplay.getHeight() <= CRAFTING_GRID_HEIGHT;
        } else {
            return true;
        }
    }
}
