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

package appeng.fabric.integration.trinkets;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The Trinkets slot AE2's wearable items opt into, as plain constants — <strong>deliberately free of any {@code eu.pb4}
 * reference</strong> so it can be loaded (by the data guards, or by anything else) with Trinkets absent.
 *
 * <h2>Why {@code legs/belt}</h2>
 *
 * On NeoForge, AE2's wearables opt into Curios' generic {@code curios:curio} slot
 * ({@code data/curios/tags/item/curio.json}, datagen). Trinkets Updated has <em>no</em> generic slot: its twelve
 * built-ins are {@code head/{face,hat}}, {@code chest/{back,cape,necklace}}, {@code hand|offhand/{glove,ring}},
 * {@code legs/belt} and {@code feet/{shoes,aglet}}. Defining a custom {@code curio} group instead would need a
 * hand-picked numeric {@code slot_id} (which collides across mods) plus a slot icon sprite AE2 does not ship, so the
 * items are remapped onto an existing slot.
 * <p>
 * {@code legs/belt} is the pick: a wireless terminal or portable cell is a gadget clipped to the belt, the slot's icon
 * and validator already exist, and it leaves {@code chest/necklace} — the slot amulet-style items from other mods
 * gravitate to — uncontended. Like Curios' single generic slot, this gives exactly one wearable accessory at a time;
 * additional slot tags can be added later without touching any code.
 */
public final class TrinketsSlots {
    private TrinketsSlots() {
    }

    /**
     * The {@code group/slot} id, as Trinkets addresses it in {@code data/trinkets/entities/ae2.json} and as its
     * inventory map is keyed.
     */
    public static final String ACCESSORY_SLOT = "legs/belt";

    /**
     * The item tag that opts an item into {@link #ACCESSORY_SLOT} (Trinkets' default slot validator is tag-driven).
     * Mirrors {@code appeng.core.ConventionTags#CURIOS} on the NeoForge side.
     */
    public static final TagKey<Item> ACCESSORY_SLOT_TAG = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("trinkets", ACCESSORY_SLOT));
}
