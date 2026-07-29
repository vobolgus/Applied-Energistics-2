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

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;

import appeng.integration.modules.curios.CuriosSupport;

/**
 * The Trinkets-backed implementation of {@link CuriosSupport}, the Fabric counterpart to
 * {@code appeng.neoforge.integration.NeoForgeCuriosSupport}. Trinkets is the Fabric analog of Curios; the pack pin is
 * Patbox's <em>Trinkets Updated</em> ({@code trinkets_updated}, package {@code eu.pb4.trinkets.api}).
 * <p>
 * <strong>This class must only be loaded when Trinkets is present</strong> — it references {@code eu.pb4} types
 * directly. {@code appeng.fabric.FabricCuriosSupport} owns that guard and is the only caller.
 *
 * <h2>Slot indices</h2>
 *
 * {@link CuriosSupport.Inventory} is a flat, slot-indexed view, and the index is written to the wire by
 * {@code appeng.menu.locator.CuriosItemLocator} — the client picks the index (hotkey) and the server resolves it. The
 * flattening therefore has to be deterministic and identical on both sides: Trinkets' inventories are keyed by their
 * {@code group/slot} id, so the slots are visited in lexicographic key order and then by index within the slot. The
 * slot set itself is datapack-driven and synced by Trinkets, so both sides see the same keys. (Curios has the exact
 * same property; this is not a new assumption.)
 *
 * <h2>Live stacks</h2>
 *
 * {@code ItemMenuHostLocator#locateItem} documents that the returned stack is modified in place by the menu host
 * (energy drain, linked position). {@link TrinketInventory} is a vanilla {@code Container}, so {@code getItem} hands
 * out the live stack — no copy, unlike the NeoForge resource-handler twin.
 */
public final class TrinketsAccessorySupport {
    private TrinketsAccessorySupport() {
    }

    @Nullable
    public static CuriosSupport.Inventory getInventory(Player player) {
        var attachment = TrinketsApi.getAttachment(player);
        if (attachment == null) {
            return null;
        }

        var inventories = attachment.getInventories();
        if (inventories.isEmpty()) {
            return null;
        }

        // Deterministic order, identical on client and server: sort by the "group/slot" key, then by slot index.
        var slotIds = new ArrayList<>(inventories.keySet());
        slotIds.sort(null);

        List<Slot> slots = new ArrayList<>();
        for (var slotId : slotIds) {
            var inventory = inventories.get(slotId);
            if (inventory == null) {
                continue;
            }
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                slots.add(new Slot(inventory, i));
            }
        }

        return new CuriosSupport.Inventory() {
            @Override
            public int size() {
                return slots.size();
            }

            @Override
            public ItemStack getStack(int slot) {
                if (slot < 0 || slot >= slots.size()) {
                    return ItemStack.EMPTY;
                }
                var entry = slots.get(slot);
                return entry.inventory().getItem(entry.index());
            }
        };
    }

    private record Slot(TrinketInventory inventory, int index) {
    }
}
