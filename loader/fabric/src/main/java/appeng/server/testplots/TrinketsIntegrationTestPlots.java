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

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

import appeng.core.ConventionTags;
import appeng.fabric.integration.trinkets.TrinketsSlots;
import appeng.server.testworld.PlotBuilder;

/**
 * Fabric-only guards for the Trinkets integration data (there is no NeoForge twin: Curios' equivalent
 * {@code data/curios/tags/item/curio.json} is produced by datagen and covered by the datagen parity check).
 * <p>
 * Trinkets equippability is <strong>data-driven</strong>: {@code appeng.fabric.FabricCuriosSupport} can read whatever
 * is in a player's accessory slots, but nothing can ever be put into one unless (a) the item opts into the slot's item
 * tag and (b) something attaches the slot to the player entity. Neither is code, so neither fails loudly — a missing
 * file just means the terminal can never be worn. These two tests are what makes that provable headlessly; the
 * equip/open flow itself needs Trinkets installed and is an in-world check (see PORTING_NOTES).
 * <p>
 * Both tags load regardless of whether Trinkets is installed: vanilla builds item tags for every namespace present in
 * the loaded data packs.
 */
@TestPlotClass
public final class TrinketsIntegrationTestPlots {
    private TrinketsIntegrationTestPlots() {
    }

    /**
     * The Trinkets slot tag must carry exactly the items the Curios tag carries. This is the drift guard: the Curios
     * tag is datagen output, so adding a wearable item there (upstream or on a rebase) must not silently leave the
     * hand-written Trinkets twin behind.
     */
    @TestPlot("trinkets_slot_tag")
    public static void trinketsSlotTag(PlotBuilder builder) {
        builder.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        builder.test(helper -> {
            var trinketsItems = collect(TrinketsSlots.ACCESSORY_SLOT_TAG);
            var curiosItems = collect(ConventionTags.CURIOS);

            helper.check(!trinketsItems.isEmpty(),
                    "the trinkets slot tag " + TrinketsSlots.ACCESSORY_SLOT_TAG.location()
                            + " is empty or missing: no AE2 item could ever be worn in an accessory slot");
            helper.check(trinketsItems.equals(curiosItems),
                    "the trinkets slot tag and the curios tag disagree. trinkets-only: "
                            + difference(trinketsItems, curiosItems) + ", curios-only: "
                            + difference(curiosItems, trinketsItems));

            helper.succeed();
        });
    }

    /**
     * ...and the slot has to be attached to the player. Trinkets' own jar ships no {@code entities} file, so without
     * ours the belt slot simply does not exist on a player.
     */
    @TestPlot("trinkets_entity_slots")
    public static void trinketsEntitySlots(PlotBuilder builder) {
        builder.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        builder.test(helper -> {
            var id = Identifier.fromNamespaceAndPath("trinkets", "entities/ae2.json");
            var resource = helper.getLevel().getServer().getResourceManager().getResource(id);
            helper.check(resource.isPresent(), "missing data file " + id
                    + ": nothing attaches the accessory slot to players, so it never appears in the trinkets screen");

            try (var reader = new InputStreamReader(resource.orElseThrow().open(), StandardCharsets.UTF_8)) {
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                helper.check(contains(root, "entities", "player"), id + " does not attach to the player entity");
                helper.check(contains(root, "slots", TrinketsSlots.ACCESSORY_SLOT),
                        id + " does not attach the " + TrinketsSlots.ACCESSORY_SLOT + " slot");
            } catch (Exception e) {
                throw new AssertionError("failed to read " + id, e);
            }

            helper.succeed();
        });
    }

    private static boolean contains(com.google.gson.JsonObject root, String key, String value) {
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return false;
        }
        for (var element : root.getAsJsonArray(key)) {
            if (element.isJsonPrimitive() && value.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static Set<Identifier> collect(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) {
        var ids = new TreeSet<Identifier>(java.util.Comparator.comparing(Identifier::toString));
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            ids.add(BuiltInRegistries.ITEM.getKey(holder.value()));
        }
        return ids;
    }

    private static String difference(Set<Identifier> a, Set<Identifier> b) {
        var only = new LinkedHashSet<>(a);
        only.removeAll(b);
        return only.isEmpty() ? "<none>" : only.toString();
    }
}
