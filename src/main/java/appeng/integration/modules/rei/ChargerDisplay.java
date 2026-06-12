package appeng.integration.modules.rei;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import appeng.recipes.handlers.ChargerRecipe;

public record ChargerDisplay(RecipeHolder<ChargerRecipe> holder) implements Display {

    @Override
    public List<EntryIngredient> getInputEntries() {
        // was: getIngredient() - renamed to ingredient() in 26.1
        return List.of(EntryIngredients.ofIngredient(holder.value().ingredient()));
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        // was: getResultItem() - renamed to result() returning ItemStackTemplate in 26.1
        return List.of(EntryIngredients.of(holder.value().result().create()));
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIds.CHARGER;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.of(holder.id().identifier());
    }

    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }
}
