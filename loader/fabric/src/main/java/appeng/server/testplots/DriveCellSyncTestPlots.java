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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;

import appeng.api.storage.cells.CellState;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.fabric.network.CellItemIdSync;
import appeng.server.testworld.PlotBuilder;

/**
 * Fabric-only gates for the drive's cell-item update packet.
 * <p>
 * There is no NeoForge twin on purpose: NeoForge aligns static-registry raw ids across a connection, so the shared
 * sources' {@code writeVarInt(BuiltInRegistries.ITEM.getId(..))} is correct there and stays as upstream wrote it. On
 * Fabric 26.1 it is not correct - see {@link CellItemIdSync} - and {@code DriveCellItemIdMixin} swaps the two calls for
 * an identifier-based form. These plots pin that form, both ends of it, and the fact that it is reached through the
 * real block-entity update path rather than only through the helper.
 * <p>
 * Note that a gametest is not by itself a guard against the bug class this fixes: a desync needs two <em>different</em>
 * registry states and a headless server has one. What it can pin - and does - is the wire format, its symmetry, and
 * that a payload in the old form is rejected loudly instead of decoding into a wrong item.
 */
@TestPlotClass
public final class DriveCellSyncTestPlots {

    private DriveCellSyncTestPlots() {
    }

    /**
     * Round-trips a populated drive through {@code getUpdateTag} into a detached receiver, which is the client's code
     * path exactly ({@code ClientboundBlockEntityDataPacket} -> {@code loadWithComponents} -> {@code readFromStream}),
     * and asserts (a) the payload really carries registry identifiers and (b) both cells come back.
     */
    @TestPlot("drive_cell_sync_identifier_wire")
    public static void identifierWire(PlotBuilder plot) {
        // Two cells of different types, in slots 0 and 1.
        plot.storageDrive(BlockPos.ZERO);

        plot.test(helper -> {
            var registries = helper.getLevel().registryAccess();
            var drive = helper.getBlockEntity(BlockPos.ZERO, DriveBlockEntity.class);

            var updateTag = drive.getUpdateTag(registries);
            var encoded = updateTag.getString("#upd").orElse(null);
            helper.check(encoded != null, "the drive did not produce an update payload at all");
            var payload = Base64.getDecoder().decode(encoded);

            // (a) The identifiers are literally on the wire. ISO_8859_1 keeps every byte addressable, so this is a
            // substring search over the raw payload rather than a hopeful UTF-8 decode.
            var asBytes = new String(payload, StandardCharsets.ISO_8859_1);
            for (var cell : List.of(AEItems.ITEM_CELL_64K.asItem(), AEItems.FLUID_CELL_64K.asItem())) {
                var id = BuiltInRegistries.ITEM.getKey(cell).toString();
                helper.check(asBytes.contains(id),
                        "the drive update payload does not contain '" + id + "': the cell items are still being sent "
                                + "as static-registry raw ids, which Fabric 26.1 does not align across a connection");
            }

            // (b) ... and the receiving side reconstructs them. A detached block entity reports isClientSide(), so the
            // synced client-side arrays are what its public getters return - i.e. what the drive model would render.
            var receiver = new DriveBlockEntity(drive.getType(), BlockPos.ZERO,
                    AEBlocks.DRIVE.block().defaultBlockState());
            receiver.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, updateTag));

            helper.check(receiver.getCellItem(0) == AEItems.ITEM_CELL_64K.asItem(),
                    "cell 0 came back as " + receiver.getCellItem(0) + ", expected " + AEItems.ITEM_CELL_64K.asItem());
            helper.check(receiver.getCellItem(1) == AEItems.FLUID_CELL_64K.asItem(),
                    "cell 1 came back as " + receiver.getCellItem(1) + ", expected " + AEItems.FLUID_CELL_64K.asItem());
            for (int i = 2; i < drive.getCellCount(); i++) {
                helper.check(receiver.getCellItem(i) == null, "cell " + i + " is empty but came back populated");
            }
            for (int i = 0; i < drive.getCellCount(); i++) {
                helper.check(receiver.getCellStatus(i) == drive.getCellStatus(i),
                        "cell " + i + " state desynced: sent " + drive.getCellStatus(i) + ", received "
                                + receiver.getCellStatus(i));
            }

            helper.succeed();
        });
    }

    /**
     * A payload still written in the pre-2026-08-03 raw-id form must fail loudly. The failure mode being bought off
     * here is the silent one: a varint decoded as a varint always "succeeds" and yields whatever item happens to sit at
     * that index on this side.
     */
    @TestPlot("drive_cell_sync_rejects_raw_ids")
    public static void rejectsRawIdWireForm(PlotBuilder plot) {
        plot.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        plot.test(helper -> {
            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                // Exactly what the shared sources put on the wire, and what upstream/NeoForge still does.
                buf.writeVarInt(BuiltInRegistries.ITEM.getId(AEItems.ITEM_CELL_64K.asItem()));
                buf.writeVarInt(0);

                DecoderException thrown = null;
                try {
                    var decoded = CellItemIdSync.readItem(buf);
                    helper.check(false, "the raw-id wire form decoded without complaint (as " + decoded
                            + "); an old client or server would silently render the wrong cells");
                } catch (DecoderException e) {
                    thrown = e;
                }
                helper.check(thrown != null, "expected a DecoderException naming the wire form");
            } finally {
                buf.release();
            }

            helper.succeed();
        });
    }

    /**
     * The packed cell state is a 3-bit field but {@link CellState} has 5 constants, so ordinals 5..7 are reachable on
     * the wire and used to index straight past the end of {@code CellState.values()}. Pins the guard added to
     * {@code DriveBlockEntity#readFromStream}: an out-of-range ordinal degrades to {@link CellState#ABSENT} and the
     * rest of the packet is still decoded.
     */
    @TestPlot("drive_cell_sync_bad_cell_state_ordinal")
    public static void outOfRangeCellStateOrdinal(PlotBuilder plot) {
        plot.blockState(BlockPos.ZERO, Blocks.STONE.defaultBlockState());

        plot.test(helper -> {
            var registries = helper.getLevel().registryAccess();
            var cellItem = AEItems.ITEM_CELL_64K.asItem();

            var receiver = new DriveBlockEntity(AEBlockEntities.DRIVE.get(), BlockPos.ZERO,
                    AEBlocks.DRIVE.block().defaultBlockState());

            var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            byte[] payload;
            try {
                var packedState = 0;
                packedState |= 0b111; // slot 0: an ordinal no CellState constant has
                packedState |= CellState.FULL.ordinal() << 3; // slot 1: a normal one, decoded only if slot 0 survived
                packedState |= 1 << 31;
                buf.writeInt(packedState);
                for (int i = 0; i < receiver.getCellCount(); i++) {
                    CellItemIdSync.writeItem(buf, i == 0 ? cellItem : null);
                }
                payload = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), payload);
            } finally {
                buf.release();
            }

            var tag = new CompoundTag();
            tag.putString("#upd", Base64.getEncoder().encodeToString(payload));
            receiver.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));

            helper.check(receiver.getCellStatus(0) == CellState.ABSENT,
                    "an out-of-range cell state ordinal should degrade to ABSENT, got " + receiver.getCellStatus(0));
            // Both of these are only reached if the unpack did NOT throw ArrayIndexOutOfBoundsException at slot 0:
            // AEBaseBlockEntity#readUpdateData swallows it, so a regression is otherwise invisible.
            helper.check(receiver.getCellStatus(1) == CellState.FULL,
                    "decoding stopped at the bad ordinal: cell 1 should be FULL, got " + receiver.getCellStatus(1));
            helper.check(receiver.getCellItem(0) == cellItem,
                    "decoding stopped at the bad ordinal: the cell items after it were never read");

            helper.succeed();
        });
    }
}
