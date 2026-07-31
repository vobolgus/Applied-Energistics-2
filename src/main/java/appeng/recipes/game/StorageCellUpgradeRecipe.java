package appeng.recipes.game;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/**
 * Allows swapping out the storage component of a cell without having to empty it first.
 */
public class StorageCellUpgradeRecipe extends CustomRecipe {
    private final Item inputCell;
    private final Item inputComponent;
    private final Item resultCell;
    private final Item resultComponent;

    public StorageCellUpgradeRecipe(Item inputCell, Item inputComponent, Item resultCell, Item resultComponent) {
        this.inputCell = inputCell;
        this.inputComponent = inputComponent;
        this.resultCell = resultCell;
        this.resultComponent = resultComponent;
    }

    public static final MapCodec<StorageCellUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("input_cell")
                    .forGetter(StorageCellUpgradeRecipe::getInputCell),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("input_component")
                    .forGetter(StorageCellUpgradeRecipe::getInputComponent),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result_cell")
                    .forGetter(StorageCellUpgradeRecipe::getResultCell),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result_component")
                    .forGetter(StorageCellUpgradeRecipe::getResultComponent))
            .apply(builder, StorageCellUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageCellUpgradeRecipe> STREAM_CODEC = StreamCodec
            .composite(
                    ByteBufCodecs.registry(Registries.ITEM), StorageCellUpgradeRecipe::getInputCell,
                    ByteBufCodecs.registry(Registries.ITEM), StorageCellUpgradeRecipe::getInputComponent,
                    ByteBufCodecs.registry(Registries.ITEM), StorageCellUpgradeRecipe::getResultCell,
                    ByteBufCodecs.registry(Registries.ITEM), StorageCellUpgradeRecipe::getResultComponent,
                    StorageCellUpgradeRecipe::new);

    public static final RecipeSerializer<StorageCellUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(CODEC,
            STREAM_CODEC);

    public Item getInputCell() {
        return inputCell;
    }

    public Item getInputComponent() {
        return inputComponent;
    }

    public Item getResultCell() {
        return resultCell;
    }

    public Item getResultComponent() {
        return resultComponent;
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        var cellsFound = 0;
        var componentsFound = 0;

        for (int i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(inputCell)) {
                    // Also bail out if somehow someone managed to stack cells (since we replace it with a component)
                    cellsFound += stack.getCount();
                } else if (stack.is(inputComponent)) {
                    componentsFound++;
                } else {
                    return false;
                }

                if (cellsFound > 1 || componentsFound > 1) {
                    return false;
                }
            }
        }

        return cellsFound == 1 && componentsFound == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput container) {
        ItemStack foundCell = ItemStack.EMPTY;
        var componentsFound = 0;

        for (int i = 0; i < container.size(); i++) {
            var stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(inputCell)) {
                    if (stack.getCount() > 1 || !foundCell.isEmpty()) {
                        return ItemStack.EMPTY; // More than one cell found
                    }
                    foundCell = stack;
                } else if (stack.is(inputComponent)) {
                    if (++componentsFound > 1) {
                        return ItemStack.EMPTY; // More than one component found
                    }
                } else {
                    return ItemStack.EMPTY; // Other item found
                }
            }
        }

        if (foundCell.isEmpty() || componentsFound == 0) {
            return ItemStack.EMPTY;
        } else {
            return foundCell.transmuteCopy(resultCell, 1);
        }
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        var remainder = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < remainder.size(); ++i) {
            var stack = input.getItem(i);
            if (stack.is(inputCell)) {
                // We replace the cell with the component since it is unstackable and forced to be in match
                remainder.set(i, new ItemStack(resultComponent));
            } else {
                var stackRemainder = stack.getCraftingRemainder();
                remainder.set(i, stackRemainder != null ? stackRemainder.create() : ItemStack.EMPTY);
            }
        }

        return remainder;
    }

    @Override
    public RecipeSerializer<StorageCellUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }

    /**
     * Reported as a plain shapeless crafting display so that recipe viewers can actually show it.
     * <p>
     * This used to return {@link StorageCellUpgradeDisplay}, a bespoke {@link RecipeDisplay} carrying a fourth slot for
     * the component handed back by {@link #getRemainingItems}. Nothing consumes it: JEI's built-in crafting category
     * extension accepts a {@link net.minecraft.world.item.crafting.CraftingRecipe} only when its display is a
     * {@code ShapelessCraftingRecipeDisplay} or a {@code ShapedCraftingRecipeDisplay}, and REI's built-in crafting
     * plugin behaves the same way, so all 40 {@code ae2:storage_cell_upgrade} recipes were silently absent from both
     * viewers on both loaders. (The bespoke display type is also never registered in {@code Registries.RECIPE_DISPLAY},
     * so it could not survive the vanilla display stream codec either — it is only ever built viewer-side, which is why
     * this never surfaced as an error.)
     * <p>
     * A shapeless display is the honest shape here: {@link #matches} accepts exactly one cell plus one component in any
     * arrangement, which is what shapeless means. The one thing it cannot express is the returned old component — the
     * same information the pre-26.1 REI integration also dropped when it registered these into the vanilla crafting
     * category via a recipe filler. Recovering it needs a dedicated AE2 viewer category, which is a larger change than
     * making the recipes visible at all.
     */
    @Override
    public List<RecipeDisplay> display() {
        return List.of(
                new ShapelessCraftingRecipeDisplay(
                        List.of(
                                new SlotDisplay.ItemSlotDisplay(inputCell),
                                new SlotDisplay.ItemSlotDisplay(inputComponent)),
                        new SlotDisplay.ItemSlotDisplay(resultCell),
                        new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}
