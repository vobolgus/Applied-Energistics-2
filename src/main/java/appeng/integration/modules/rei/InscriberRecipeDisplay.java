/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.integration.modules.rei;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import appeng.recipes.handlers.InscriberRecipe;

public class InscriberRecipeDisplay implements Display {
    private final RecipeHolder<InscriberRecipe> holder;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;

    public InscriberRecipeDisplay(RecipeHolder<InscriberRecipe> holder) {
        this.holder = holder;
        var recipe = holder.value();
        // The category addresses the inputs by fixed slot index (top=0, middle=1, bottom=2), so always emit all
        // three entries. (The pre-26.1 code dropped the middle ingredient by accident and mis-indexed when the
        // top ingredient was absent.)
        this.inputs = ImmutableList.of(
                recipe.getTopOptional().map(EntryIngredients::ofIngredient).orElse(EntryIngredient.empty()),
                EntryIngredients.ofIngredient(recipe.getMiddleInput()),
                recipe.getBottomOptional().map(EntryIngredients::ofIngredient).orElse(EntryIngredient.empty()));
        // was: recipe.getResultItem() - renamed to result() returning ItemStackTemplate in 26.1
        this.outputs = ImmutableList.of(EntryIngredients.of(recipe.result().create()));
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
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CategoryIds.INSCRIBER;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.of(holder.id().identifier());
    }

    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        // Not synced to the server; AE2 brings its own transfer handlers.
        return null;
    }
}
