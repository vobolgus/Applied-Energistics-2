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

import net.fabricmc.fabric.impl.recipe.sync.RecipeSyncImpl;
import net.fabricmc.fabric.impl.recipe.sync.SyncedSerializerAwarePreparedRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Blocks;

import appeng.core.AppEng;
import appeng.core.definitions.AEParts;
import appeng.core.registration.AERegistries;
import appeng.recipes.quartzcutting.QuartzCuttingRecipe;
import appeng.server.testworld.PlotBuilder;

/**
 * Fabric-only regression guard for recipe-viewer visibility (no NeoForge twin: there {@code OnDatapackSyncEvent} syncs
 * by recipe <em>type</em>, and AE2 asks for all of {@code RecipeType.CRAFTING}, so a custom crafting serializer cannot
 * go missing).
 * <p>
 * Since 1.21.2 vanilla stopped shipping recipes to the client; fabric-api's replacement is opt-in per recipe
 * <em>serializer</em>. JEI's Fabric entrypoint opts in the {@code minecraft:} namespace and nothing else, so any AE2
 * recipe with an AE2 serializer is invisible in every recipe viewer unless AE2 opts it in itself. Live report
 * 2026-07-30: {@code ae2:cable_anchor} (the sole {@code ae2:quartz_cutting} recipe) showed no recipe in JEI.
 *
 * @see appeng.fabric.network.RecipeSync
 */
@TestPlotClass
public final class RecipeSerializerSyncTestPlots {
    private RecipeSerializerSyncTestPlots() {
    }

    /**
     * Census: every AE2 recipe serializer must be opted into fabric-api's sync. Written against the registration
     * collector rather than a hand-written list, so a serializer added later fails this test until it is covered.
     */
    @TestPlot("fabricapi_synced_recipe_serializers")
    public static void syncedRecipeSerializers(PlotBuilder builder) {
        builder.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        builder.test(helper -> {
            var entries = AERegistries.entries(Registries.RECIPE_SERIALIZER);
            helper.check(!entries.isEmpty(), "no AE2 recipe serializers were collected at all");

            for (var entry : entries) {
                var serializer = (RecipeSerializer<?>) entry.get();
                helper.check(RecipeSyncImpl.isSynced(serializer),
                        "recipe serializer " + entry.getId() + " is not opted into fabric-api's recipe sync: "
                                + "its recipes are invisible in JEI/REI and any other viewer on Fabric");
            }

            helper.succeed();
        });
    }

    /**
     * ...and the recipe the live bug was reported for really is picked up by the sync path: fabric-api sends exactly
     * {@code RecipeMap#fabric_getRecipesBySyncedSerializer(serializer)} per opted-in serializer, and that grouping is
     * built at {@code RecipeMap.create} time from the serializers registered during mod init.
     */
    @TestPlot("fabricapi_synced_quartz_cutting")
    public static void syncedQuartzCuttingRecipes(PlotBuilder builder) {
        builder.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        builder.test(helper -> {
            var recipeMap = RecipeMap.create(helper.getLevel().getServer().getRecipeManager().getRecipes());

            var synced = ((SyncedSerializerAwarePreparedRecipe) recipeMap)
                    .fabric_getRecipesBySyncedSerializer(QuartzCuttingRecipe.SERIALIZER);
            helper.check(synced != null,
                    "ae2:quartz_cutting was not opted into fabric-api's recipe sync, so nothing is grouped for it");
            helper.check(synced != null && !synced.isEmpty(),
                    "no ae2:quartz_cutting recipes would be sent to the client");

            var cableAnchor = AppEng.makeId("network/parts/cable_anchor");
            helper.check(
                    synced != null && synced.stream().anyMatch(holder -> holder.id().identifier().equals(cableAnchor)),
                    "the " + cableAnchor + " recipe would not be sent to the client, i.e. JEI shows no recipe for "
                            + AEParts.CABLE_ANCHOR.id());

            helper.succeed();
        });
    }
}
