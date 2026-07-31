package appeng.recipes.game;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/**
 * A four-slot display for {@link StorageCellUpgradeRecipe}: the cell and component going in, the upgraded cell coming
 * out, and the old component handed back.
 * <p>
 * <b>Currently unreferenced.</b> No recipe viewer understands it — JEI's and REI's built-in crafting categories accept
 * only {@code ShapelessCraftingRecipeDisplay}/{@code ShapedCraftingRecipeDisplay} — and {@link #TYPE} is not registered
 * in {@code Registries.RECIPE_DISPLAY}, so it cannot be serialized either. Returning it from
 * {@link StorageCellUpgradeRecipe#display()} therefore made all 40 upgrade recipes invisible in both viewers on both
 * loaders; that method now reports a shapeless display instead. Kept because it is the right shape for a dedicated AE2
 * upgrade category, which is what would be needed to show the returned component again.
 */
public record StorageCellUpgradeDisplay(
        SlotDisplay inputCell,
        SlotDisplay inputComponent,
        SlotDisplay result,
        SlotDisplay resultComponent) implements RecipeDisplay {

    public static final MapCodec<StorageCellUpgradeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder.group(
                    SlotDisplay.CODEC.fieldOf("inputCell").forGetter(StorageCellUpgradeDisplay::inputCell),
                    SlotDisplay.CODEC.fieldOf("inputComponent").forGetter(StorageCellUpgradeDisplay::inputComponent),
                    SlotDisplay.CODEC.fieldOf("result").forGetter(StorageCellUpgradeDisplay::result),
                    SlotDisplay.CODEC.fieldOf("resultComponent").forGetter(StorageCellUpgradeDisplay::resultComponent))
                    .apply(builder, StorageCellUpgradeDisplay::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageCellUpgradeDisplay> STREAM_CODEC = StreamCodec
            .composite(
                    SlotDisplay.STREAM_CODEC, StorageCellUpgradeDisplay::inputCell,
                    SlotDisplay.STREAM_CODEC, StorageCellUpgradeDisplay::inputComponent,
                    SlotDisplay.STREAM_CODEC, StorageCellUpgradeDisplay::result,
                    SlotDisplay.STREAM_CODEC, StorageCellUpgradeDisplay::resultComponent,
                    StorageCellUpgradeDisplay::new);

    public static final RecipeDisplay.Type<StorageCellUpgradeDisplay> TYPE = new RecipeDisplay.Type<>(MAP_CODEC,
            STREAM_CODEC);

    @Override
    public SlotDisplay result() {
        return result;
    }

    @Override
    public SlotDisplay craftingStation() {
        return new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE);
    }

    @Override
    public Type<? extends RecipeDisplay> type() {
        return TYPE;
    }
}
