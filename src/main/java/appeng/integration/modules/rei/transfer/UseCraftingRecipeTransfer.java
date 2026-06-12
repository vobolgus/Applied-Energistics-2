package appeng.integration.modules.rei.transfer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Blocks;

import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;

import appeng.core.localization.ItemModText;
import appeng.integration.modules.itemlists.CraftingHelper;
import appeng.integration.modules.itemlists.TransferHelper;
import appeng.menu.me.items.CraftingTermMenu;

/**
 * Recipe transfer implementation with the intended purpose of actually crafting an item. Most of the work is done
 * server-side because permission-checks and inventory extraction cannot be done client-side.
 * <p>
 * Here is how it works, depending on the various cases. In any case, we highlight missing entries in red and craftable
 * entries in blue. How the {@code +} button is rendered and what it does depends on various cases:
 * <ul>
 * <li><b>All items are present:</b> normal gray, can click + to move.</li>
 * <li><b>All items are missing:</b> red, can't click.</li>
 * <li><b>Some items are missing, all craftable:</b> blue, can click to move what's available or ctrl + click to
 * additionally schedule autocrafting of what's craftable.</li>
 * <li><b>Some items are missing, some not craftable:</b> orange, same action as above.</li>
 * </ul>
 */
public class UseCraftingRecipeTransfer<T extends CraftingTermMenu> extends AbstractTransferHandler<T> {

    public UseCraftingRecipeTransfer(Class<T> containerClass) {
        super(containerClass);
    }

    @Override
    protected Result transferRecipe(T menu, RecipeHolder<?> holder, Display display, boolean doTransfer) {

        var recipeId = holder != null ? holder.id() : null;
        var recipe = holder != null ? holder.value() : null;

        boolean craftingRecipe = isCraftingRecipe(recipe, display);
        if (!craftingRecipe) {
            return Result.createNotApplicable();
        }

        if (!fitsIn3x3Grid(recipe, display)) {
            return Result.createFailed(ItemModText.RECIPE_TOO_LARGE.text());
        }

        if (recipe == null) {
            recipe = createFakeRecipe(display);
        }

        // Thank you RS for pioneering this amazing feature! :)
        boolean craftMissing = Minecraft.getInstance().hasControlDown();
        // Find missing ingredient
        var slotToIngredientMap = getGuiSlotToIngredientMap(recipe);
        var missingSlots = menu.findMissingIngredients(slotToIngredientMap);

        if (missingSlots.missingSlots().size() == slotToIngredientMap.size()) {
            // All missing, can't do much...
            // TODO (REI 26.1): the missing/craftable slot highlight overlays (Result#renderer /
            // TransferHandlerRenderer) cannot be ported yet: REI 21.11's TransferHandlerRenderer signature uses the
            // pre-26.1 net.minecraft.client.gui.GuiGraphics (renamed to GuiGraphicsExtractor in 26.1). Restore the
            // overlays from git history once REI ships a 26.1 build.
            return Result.createFailed(ItemModText.NO_ITEMS.text());
        }

        if (!doTransfer) {
            if (missingSlots.anyMissingOrCraftable()) {
                // Highlight the + button to signal that ingredients are missing or need autocrafting
                int color = missingSlots.anyMissing() ? TransferHelper.ORANGE_PLUS_BUTTON_COLOR
                        : TransferHelper.BLUE_PLUS_BUTTON_COLOR;
                var result = Result.createSuccessful()
                        .color(color);

                var tooltip = TransferHelper.createCraftingTooltip(missingSlots, craftMissing, true);
                result.overrideTooltipRenderer((point, sink) -> sink.accept(Tooltip.create(tooltip)));

                return result;
            }
        } else {
            // was: TODO 1.21.4 - mirrors the JEI integration's transfer call
            CraftingHelper.performTransfer(menu, recipeId, recipe, craftMissing);
        }

        // No error
        return Result.createSuccessful().blocksFurtherHandling();
    }

    private Recipe<?> createFakeRecipe(Display display) {
        var ingredients = NonNullList.<Optional<Ingredient>>withSize(CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT,
                Optional.empty());

        for (int i = 0; i < Math.min(display.getInputEntries().size(), ingredients.size()); i++) {
            var items = display.getInputEntries().get(i).stream()
                    .filter(es -> es.getType() == VanillaEntryTypes.ITEM)
                    .map(es -> ((ItemStack) es.castValue()).getItem())
                    .distinct()
                    .toList();
            if (!items.isEmpty()) {
                // was: Ingredient.of(Stream<ItemStack>) (TODO 1.21.4)
                ingredients.set(i, Optional.of(Ingredient.of(items.toArray(Item[]::new))));
            }
        }

        var pattern = new ShapedRecipePattern(CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT, ingredients,
                Optional.empty());
        // was: new ShapedRecipe("", CraftingBookCategory.MISC, pattern, ItemStack.EMPTY) - 26.1 recipe ctor shape;
        // the result template must be non-empty but is never read (only the placement info is queried).
        var commonInfo = new Recipe.CommonInfo(false);
        var bookInfo = new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "");
        return new ShapedRecipe(commonInfo, bookInfo, pattern,
                ItemStackTemplate.fromNonEmptyStack(new ItemStack(Blocks.BARRIER)));
    }

    public static Map<Integer, Ingredient> getGuiSlotToIngredientMap(Recipe<?> recipe) {
        var placement = recipe.placementInfo();
        if (placement.isImpossibleToPlace()) {
            return Map.of();
        }

        // REI lays out non-shaped recipes top-left-aligned in the 3x3 grid (unlike JEI, which centers them);
        // the historic REI mapping is kept.
        int width;
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            width = shapedRecipe.getWidth();
        } else {
            width = CRAFTING_GRID_WIDTH;
        }

        var result = new HashMap<Integer, Ingredient>(placement.slotsToIngredientIndex().size());
        for (int i = 0; i < placement.slotsToIngredientIndex().size(); i++) {
            var guiSlot = (i / width) * CRAFTING_GRID_WIDTH + (i % width);
            int index = placement.slotsToIngredientIndex().getInt(i);
            if (index < 0) {
                continue;
            }
            var ingredient = placement.ingredients().get(index);
            result.put(guiSlot, ingredient);
        }
        return result;
    }
}
