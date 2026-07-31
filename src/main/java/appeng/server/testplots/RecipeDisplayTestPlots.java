/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2025, TeamAppliedEnergistics, All rights reserved.
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

package appeng.server.testplots;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.level.block.Blocks;

import appeng.core.AppEng;
import appeng.recipes.game.StorageCellUpgradeRecipe;
import appeng.server.testworld.PlotBuilder;

/**
 * Presence floor for recipe-viewer visibility of AE2's crafting-table recipes.
 * <p>
 * Getting a recipe as far as the client is only half of being visible — {@link RecipeSerializerSyncTestPlots} covers
 * that half. The other half is the <em>shape</em> of what {@link net.minecraft.world.item.crafting.Recipe#display()}
 * reports: viewers do not render arbitrary {@link RecipeDisplay}s. JEI's built-in crafting category extension accepts a
 * {@link net.minecraft.world.item.crafting.CraftingRecipe} only if its display is a
 * {@link ShapelessCraftingRecipeDisplay} or a {@link ShapedCraftingRecipeDisplay}, and REI's built-in crafting plugin
 * behaves the same way. A recipe reporting anything else is dropped by both, on both loaders, with no error at all.
 * <p>
 * That is exactly what happened to the 40 {@code ae2:storage_cell_upgrade} recipes, which reported a bespoke four-slot
 * display and were therefore absent from every recipe viewer (found 2026-07-30). This plot pins the general rule rather
 * than that one recipe, so the next bespoke display fails here instead of shipping invisible.
 * <p>
 * Loader-agnostic on purpose: the display shape is decided in shared code, so both the NeoForge and the Fabric suite
 * run it.
 */
@TestPlotClass
public final class RecipeDisplayTestPlots {
    private RecipeDisplayTestPlots() {
    }

    private static boolean isViewerRenderable(RecipeDisplay display) {
        return display instanceof ShapelessCraftingRecipeDisplay || display instanceof ShapedCraftingRecipeDisplay;
    }

    /**
     * Every AE2 crafting-table recipe that reports a display at all reports one the built-in crafting categories of JEI
     * and REI can render.
     * <p>
     * Reporting <em>no</em> display is deliberately allowed and is not the bug this guards: it is how AE2 opts a
     * dynamic recipe out of the generic crafting category so a bespoke viewer plugin can present it properly instead
     * ({@code ae2:special/facade} via {@code FacadeRegistryPlugin}, and the add/remove upgrade-module recipes, whose
     * ingredient space is open-ended). The defect is building a display and picking a shape nothing renders — that
     * looks like coverage in the source and produces none in the game.
     */
    @TestPlot("viewer_renderable_crafting_displays")
    public static void viewerRenderableCraftingDisplays(PlotBuilder builder) {
        builder.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        builder.test(helper -> {
            var recipes = helper.getLevel().getServer().getRecipeManager().getRecipes();

            var checked = 0;
            var invisible = new ArrayList<String>();
            for (RecipeHolder<?> holder : recipes) {
                var id = holder.id().identifier();
                if (!AppEng.MOD_ID.equals(id.getNamespace()) || holder.value().getType() != RecipeType.CRAFTING) {
                    continue;
                }

                List<RecipeDisplay> displays = holder.value().display();
                if (displays.isEmpty()) {
                    continue;
                }

                checked++;
                if (displays.stream().noneMatch(RecipeDisplayTestPlots::isViewerRenderable)) {
                    invisible.add(id + " -> " + displays.stream().map(d -> d.getClass().getSimpleName()).toList());
                }
            }

            helper.check(checked > 0,
                    "no ae2 crafting recipes reported a display at all, so this guard proved nothing");
            helper.check(invisible.isEmpty(),
                    "these ae2 crafting recipes report a display, but of no shape JEI/REI's built-in crafting "
                            + "category can render, i.e. they are invisible in every recipe viewer on both loaders: "
                            + invisible);

            helper.succeed();
        });
    }

    /**
     * ...and the specific family the bug was found on is really there in full. Guards the count as well as the shape:
     * the 40 upgrade recipes are datagen output, so losing them is as plausible a regression as mis-displaying them.
     */
    @TestPlot("storage_cell_upgrade_recipes_visible")
    public static void storageCellUpgradeRecipesVisible(PlotBuilder builder) {
        builder.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        builder.test(helper -> {
            var upgrades = new ArrayList<RecipeHolder<?>>();
            for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
                if (holder.value() instanceof StorageCellUpgradeRecipe) {
                    upgrades.add(holder);
                }
            }

            helper.check(!upgrades.isEmpty(), "no ae2:storage_cell_upgrade recipes are loaded");

            for (var holder : upgrades) {
                var displays = holder.value().display();
                helper.check(displays.stream().anyMatch(RecipeDisplayTestPlots::isViewerRenderable),
                        holder.id().identifier() + " reports only "
                                + displays.stream().map(d -> d.getClass().getSimpleName()).toList()
                                + ", which no recipe viewer renders — the cell upgrade would be invisible in JEI/REI");
            }

            helper.succeed();
        });
    }
}
