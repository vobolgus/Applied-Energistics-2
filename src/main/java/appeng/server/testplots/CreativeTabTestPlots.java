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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import appeng.api.ids.AECreativeTabIds;
import appeng.server.testworld.PlotBuilder;
import appeng.server.testworld.PlotTestHelper;

/**
 * Creative-tab build gate. Nothing else in the test suite, on either loader, ever runs a tab's display-items
 * generator: a dedicated server does not build tab contents, and the only caller of
 * {@link CreativeModeTabs#tryRebuildTabContents} in normal play is the creative inventory screen. That makes every
 * failure mode of a tab generator — a throw, or a silently empty tab — invisible until a player presses E in
 * creative.
 * <p>
 * The specific failure this pins is the <b>duplicate-accept</b> class (found in ExtendedAE, 2026-07-31, and swept
 * across the Fabric ports): a generator that walks two registration collections which overlap (block registration
 * also puts a {@code BlockItem} into the item collection) accepts the same stack twice. NeoForge's tab builder
 * tolerates that; <b>vanilla throws</b> {@code IllegalStateException: Accidentally adding the same item stack twice}
 * out of {@code CreativeModeTab$ItemDisplayBuilder#accept}, and neither {@code tryRebuildTabContents} nor
 * {@code buildAllTabContents} catches it (26.1.2 disassembly), so the client dies on open.
 * <p>
 * AE2's own {@code MainCreativeTab} fills from a single {@code itemDefs} list, so it is structurally safe today —
 * this keeps it that way, and additionally covers the per-item {@code addToMainCreativeTab} overrides (charged
 * variants, encoded patterns, wrapped generic stacks) that <em>do</em> emit several stacks per item.
 */
@TestPlotClass
public final class CreativeTabTestPlots {

    private CreativeTabTestPlots() {
    }

    private static final List<ResourceKey<CreativeModeTab>> AE2_TABS = List.of(
            AECreativeTabIds.MAIN,
            AECreativeTabIds.FACADES);

    @TestPlot("creative_tabs_build")
    public static void creativeTabsBuild(PlotBuilder plot) {
        plot.test(helper -> {
            var level = helper.getLevel();
            var params = new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true,
                    level.registryAccess());

            for (var key : AE2_TABS) {
                var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(key);
                helper.check(tab != null, "creative tab " + key.identifier() + " is not registered");
                var nonNull = tab;

                try {
                    nonNull.buildContents(params);
                } catch (RuntimeException e) {
                    throw helper.assertionException("creative tab " + key.identifier() + " threw while building its"
                            + " contents — the creative inventory would crash on open. Duplicate accept? " + e);
                }

                var entries = new ArrayList<>(nonNull.getDisplayItems());
                helper.check(!entries.isEmpty(), "creative tab " + key.identifier() + " built EMPTY");
                assertNoDuplicates(helper, key, entries);
                helper.check(!nonNull.getIconItem().isEmpty(),
                        "creative tab " + key.identifier() + " has an empty icon stack");
            }

            // The exact call the creative screen makes: rebuilds every tab in the registry.
            try {
                CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
            } catch (RuntimeException e) {
                throw helper.assertionException("CreativeModeTabs.tryRebuildTabContents threw — the creative"
                        + " inventory would crash on open: " + e);
            }
            for (var key : AE2_TABS) {
                var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(key);
                helper.check(tab != null && !tab.getDisplayItems().isEmpty(),
                        "creative tab " + key.identifier() + " is empty after the vanilla rebuild");
            }
            helper.succeed();
        });
    }

    /**
     * Belt-and-braces census: keeps the gate meaningful on NeoForge (whose builder tolerates duplicates) and on any
     * future MC that drops the vanilla guard.
     */
    private static void assertNoDuplicates(PlotTestHelper helper, ResourceKey<CreativeModeTab> key,
            List<ItemStack> entries) {
        var seen = new ArrayList<ItemStack>(entries.size());
        for (var stack : entries) {
            helper.check(!stack.isEmpty(), "empty stack in creative tab " + key.identifier());
            for (var other : seen) {
                if (ItemStack.isSameItemSameComponents(stack, other)) {
                    throw helper.assertionException("creative tab " + key.identifier() + " lists "
                            + BuiltInRegistries.ITEM.getKey(stack.getItem())
                            + " twice with identical components — vanilla throws on this, NeoForge does not");
                }
            }
            seen.add(stack);
        }
    }
}
