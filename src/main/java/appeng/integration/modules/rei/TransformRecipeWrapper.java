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

import appeng.recipes.transform.TransformCircumstance;
import appeng.recipes.transform.TransformRecipe;

public class TransformRecipeWrapper implements Display {

    private final RecipeHolder<TransformRecipe> holder;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;

    public TransformRecipeWrapper(RecipeHolder<TransformRecipe> holder) {
        this.holder = holder;
        // was: getIngredients()/getResultItem() - renamed to ingredients()/result() (ItemStackTemplate) in 26.1
        this.inputs = EntryIngredients.ofIngredients(holder.value().ingredients());
        this.outputs = List.of(EntryIngredients.of(holder.value().result().create()));
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.of(holder.id().identifier());
    }

    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIds.TRANSFORM;
    }

    public TransformCircumstance getTransformCircumstance() {
        return holder.value().circumstance;
    }
}
