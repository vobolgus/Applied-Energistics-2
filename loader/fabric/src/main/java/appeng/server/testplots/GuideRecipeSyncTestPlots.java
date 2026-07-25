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

import java.util.HashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.Blocks;

import guideme.internal.GuideME;

import appeng.core.AppEng;
import appeng.core.AppEngBase;
import appeng.recipes.AERecipeTypes;
import appeng.server.testworld.PlotBuilder;

/**
 * Fabric-only regression guard for the guidebook's recipe rendering (no NeoForge twin: there a single
 * {@code OnDatapackSyncEvent} collects every mod's {@code sendRecipes} request into one packet, so it cannot break).
 * <p>
 * On Fabric, AE2 and GuideME each run their own recipe-sync payload into their own client-side recipe map. GuideME's
 * {@code RecipeCompiler} — what {@code <RecipeFor id="..."/>} in the guidebook resolves through — only ever sees
 * GuideME's map, so every AE2 recipe type that is not registered via {@link guideme.GuidesCommon#addSyncedRecipeTypes}
 * renders as "Couldn't find recipe for ..." (found live on the dedicated server 2026-07-25 with
 * {@code ae2:transform}/{@code damaged_budding_quartz} on the "Getting Started" page).
 * <p>
 * The guide export in CI cannot catch this: with no server connection it falls back to
 * {@code Platform.fallbackClientRecipeMap}, which holds the complete server-side recipe manager.
 */
@TestPlotClass
public final class GuideRecipeSyncTestPlots {
    private GuideRecipeSyncTestPlots() {
    }

    @TestPlot("guideme_recipe_sync_types")
    public static void guideMeRecipeSyncTypes(PlotBuilder builder) {
        builder.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        builder.test(helper -> {
            var syncedByGuideMe = new HashSet<>(GuideME.getSyncedRecipeTypes());

            for (var recipeType : ((AppEngBase) AppEng.instance()).getServerSyncedRecipeTypes()) {
                helper.check(syncedByGuideMe.contains(recipeType),
                        "GuideME does not sync recipe type " + BuiltInRegistries.RECIPE_TYPE.getKey(recipeType)
                                + ": guide pages using it would render \"Couldn't find recipe for ...\"");
            }

            // ...and the type the live bug was reported for actually has recipes to send.
            var recipeMap = RecipeMap.create(helper.getLevel().getServer().getRecipeManager().getRecipes());
            helper.check(!recipeMap.byType(AERecipeTypes.TRANSFORM).isEmpty(),
                    "no ae2:transform recipes are loaded on the server");

            helper.succeed();
        });
    }
}
