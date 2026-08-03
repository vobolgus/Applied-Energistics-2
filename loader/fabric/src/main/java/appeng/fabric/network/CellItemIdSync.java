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

package appeng.fabric.network;

import org.jetbrains.annotations.Nullable;

import io.netty.handler.codec.DecoderException;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import appeng.core.AELog;

/**
 * Identifier-based wire form for the cell items a drive-like block entity syncs to its viewers.
 * <p>
 * <strong>Why this exists.</strong> {@code DriveBlockEntity#writeToStream} sends each cell's item as
 * {@code BuiltInRegistries.ITEM.getId(item)} - a <em>static-registry raw id</em>. On NeoForge that is sound, because
 * the loader aligns static-registry raw ids across a connection. On Fabric 26.1 it is not: vanilla stopped shipping
 * registry-id remaps for the static registries and fabric-registry-sync no longer aligns them, so client and server
 * disagree about which int means which item as soon as their mod sets differ at all (playbook Part 10 - the same class
 * of bug as the recipe-serializer desync fixed in {@link SyncRecipesPayload}). Live report 2026-08-03: 16x
 * {@code "Received unknown item id from server for disk drive ...: 9"}, i.e. drives rendering the wrong cells or none.
 * <p>
 * <strong>The wire form.</strong> One length-prefixed UTF string per cell:
 * <ul>
 * <li>{@code ""} - the cell is empty (this is what the raw-id form encoded as {@code 0}),</li>
 * <li>otherwise the item's registry key, e.g. {@code "ae2:item_storage_cell_64k"}.</li>
 * </ul>
 * <p>
 * <strong>This is a deliberate Fabric-only divergence from the NeoForge wire.</strong> That is safe because a drive
 * update packet never crosses loaders: both ends of any connection run the same jar. The shared sources keep the raw-id
 * form byte-for-byte (so {@code :neoforge} is untouched and the fork stays rebaseable) and the Fabric layer swaps the
 * two calls out with {@code DriveCellItemIdMixin}.
 * <p>
 * <strong>Decoding is strict about the form and lenient about content.</strong> A string that is not a valid
 * {@link Identifier} - which is what the old varint form decodes to - throws {@link DecoderException} rather than
 * silently producing a wrong item; an id that simply is not registered on this side (a mod present on the server only)
 * warns once per occurrence and renders as an empty cell, which is what the raw-id form intended to do.
 *
 * @see appeng.fabric.mixins.DriveCellItemIdMixin
 */
public final class CellItemIdSync {

    /**
     * Cell items are registry keys; the longest conceivable one is far below this. Keeping the bound tight is load
     * bearing: it is what turns a stream still written in the old varint form into an immediate, named decode failure
     * instead of a plausible-looking garbage string.
     */
    private static final int MAX_ID_LENGTH = 256;

    private CellItemIdSync() {
    }

    /**
     * Writes one cell slot. {@code null} and {@link Items#AIR} both mean "empty cell".
     */
    public static void writeItem(FriendlyByteBuf data, @Nullable Item item) {
        if (item == null || item == Items.AIR) {
            data.writeUtf("", MAX_ID_LENGTH);
        } else {
            data.writeUtf(BuiltInRegistries.ITEM.getKey(item).toString(), MAX_ID_LENGTH);
        }
    }

    /**
     * Adapter for call sites that only have the raw id the shared sources computed (the mixin redirect). Any
     * non-positive id means "empty cell": {@code 0} is {@code minecraft:air} and {@code -1} is what {@code getId(null)}
     * yields for an empty slot.
     *
     * @return {@code data}, so the redirect can forward {@code writeVarInt}'s return value.
     */
    public static FriendlyByteBuf writeItemByRawId(FriendlyByteBuf data, int rawId) {
        writeItem(data, rawId <= 0 ? null : BuiltInRegistries.ITEM.byId(rawId));
        return data;
    }

    /**
     * Reads one cell slot written by {@link #writeItem}.
     *
     * @return the item, or {@code null} for an empty cell / an item this side does not have.
     * @throws DecoderException if the payload is not in this wire form at all.
     */
    @Nullable
    public static Item readItem(FriendlyByteBuf data) {
        String raw;
        try {
            raw = data.readUtf(MAX_ID_LENGTH);
        } catch (RuntimeException e) {
            throw new DecoderException("Drive cell sync: expected an item id string, got an undecodable payload. "
                    + "This connection is not speaking AE2-Fabric's identifier-based cell wire form "
                    + "(pre-2026-08-03 builds sent a registry raw id here).", e);
        }
        if (raw.isEmpty()) {
            return null;
        }
        var id = Identifier.tryParse(raw);
        if (id == null) {
            throw new DecoderException("Drive cell sync: '" + raw + "' is not a valid item id. "
                    + "This connection is not speaking AE2-Fabric's identifier-based cell wire form "
                    + "(pre-2026-08-03 builds sent a registry raw id here).");
        }
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            // Well-formed but unknown here: the server has a mod we do not. Render an empty cell, do not desync.
            AELog.warn("Received an unknown cell item id from the server: %s", id);
        }
        return item;
    }

    /**
     * Adapter for call sites that expect the {@code readVarInt()} the shared sources call (the mixin redirect):
     * re-encodes the decoded item as a <em>local</em> raw id, which is exactly what the following
     * {@code BuiltInRegistries.ITEM.byId(..)} in the shared code needs.
     *
     * @return this side's raw id for the item, or {@code 0} ({@code minecraft:air}, the shared code's "empty" marker).
     */
    public static int readItemAsRawId(FriendlyByteBuf data) {
        var item = readItem(data);
        return item == null ? 0 : BuiltInRegistries.ITEM.getId(item);
    }
}
