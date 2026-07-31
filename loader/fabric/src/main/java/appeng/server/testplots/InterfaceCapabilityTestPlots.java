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

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.server.testworld.PlotBuilder;
import appeng.server.testworld.PlotTestHelper;

/**
 * Fabric twin of the NeoForge {@code InterfaceCapabilityTestPlots}: the same {@code interface_slot_filtering} plot,
 * asserted through the Fabric transfer API ({@link ItemStorage#SIDED}/{@link FluidStorage#SIDED}) instead of NeoForge
 * block capabilities.
 *
 * <p>
 * NeoForge probes each slot with {@code capability.isValid(slot, resource)} — a pure <em>filter</em> query independent
 * of the slot's current contents. Fabric's {@link SingleSlotStorage} has no {@code isValid}; the equivalent is a
 * <em>simulated</em> {@code insert} inside a rolled-back transaction. That distinction matters here: by the time the
 * plot's test sequence starts, the hopper has already fed its brick into the first open buffer slot, so we cannot probe
 * that slot as if it were empty. Instead we probe:
 * <ul>
 * <li>the configured slot 0 (which stays empty — nothing on the grid stocks it) to prove the config filter accepts its
 * item and rejects everything else, including fluids;</li>
 * <li>a guaranteed-empty high buffer slot (the plot's single brick + stick only ever route to slots 0/1) to prove an
 * open slot accepts otherwise-filtered resources.</li>
 * </ul>
 * The hopper-driven routing (brick → first open slot, then stick → the configured slot) is asserted afterwards exactly
 * as NeoForge does, against AE2's own storage view.
 *
 * <p>
 * Split out of {@link InterfaceTestPlots} because it depends on loader-specific transfer APIs, and registered on the
 * Fabric plot list by {@code FabricTestPlotPlatform} (Fabric has no annotation scan). Closes the fabric↔neoforge
 * gametest-count gap — both loaders now exercise {@code interface_slot_filtering}.
 */
@TestPlotClass
public final class InterfaceCapabilityTestPlots {
    private InterfaceCapabilityTestPlots() {
    }

    /**
     * Test that configured slots of interfaces are filtered and prevent insertion.
     */
    @TestPlot("interface_slot_filtering")
    public static void interfaceSlotFiltering(PlotBuilder builder) {
        var o = BlockPos.ZERO;
        builder.blockEntity(o, AEBlocks.INTERFACE, iface -> {
            // Set slot 0 to sticks
            iface.getInterfaceLogic().getConfig().setStack(0, new GenericStack(AEItemKey.of(Items.STICK), 1));
        });
        builder.hopper(o.above(), Direction.DOWN, Items.BRICK);
        builder.test(helper -> {
            helper.startSequence()
                    .thenExecute(() -> {
                        var itemStorage = itemStorage(helper, o);
                        // A buffer slot the plot's single brick + stick can never reach, so it stays open.
                        int openSlot = itemStorage.getSlotCount() - 1;
                        helper.check(itemStorage.getSlot(openSlot).getResource().isBlank(),
                                "expected an open buffer slot to probe", o);

                        // slot 0 is configured to sticks: the capability enforces the config filter
                        helper.check(simulateInsert(itemStorage.getSlot(0), ItemVariant.of(Items.STICK), 1) > 0,
                                "stick should be insertable into stick-configured slot 0");
                        helper.check(simulateInsert(itemStorage.getSlot(0), ItemVariant.of(Blocks.BRICKS), 1) == 0,
                                "bricks should be filtered out of stick-configured slot 0");
                        // an open buffer slot accepts anything
                        helper.check(
                                simulateInsert(itemStorage.getSlot(openSlot), ItemVariant.of(Blocks.BRICKS), 1) > 0,
                                "bricks should be insertable into an open slot");

                        var fluidStorage = fluidStorage(helper, o);
                        // the item filter on slot 0 also excludes fluids
                        helper.check(
                                simulateInsert(fluidStorage.getSlot(0), FluidVariant.of(Fluids.WATER),
                                        FluidConstants.BUCKET) == 0,
                                "water should be filtered out of item-configured slot 0");
                        // an open buffer slot accepts fluid
                        helper.check(
                                simulateInsert(fluidStorage.getSlot(openSlot), FluidVariant.of(Fluids.WATER),
                                        FluidConstants.BUCKET) > 0,
                                "water should be insertable into an open slot");
                    })
                    .thenWaitUntil(() -> {
                        var iface = helper.getBlockEntity(o, InterfaceBlockEntity.class);
                        helper.assertEquals(o, null, iface.getStorage().getKey(0));
                        helper.assertEquals(o, AEItemKey.of(Items.BRICK), iface.getStorage().getKey(1));
                    })
                    .thenExecute(() -> {
                        var hopper = helper.getBlockEntity(o.above(), HopperBlockEntity.class);
                        hopper.setItem(0, Items.STICK.getDefaultInstance());
                    })
                    .thenWaitUntil(() -> {
                        var iface = helper.getBlockEntity(o, InterfaceBlockEntity.class);
                        helper.assertEquals(o, AEItemKey.of(Items.STICK), iface.getStorage().getKey(0));
                        helper.assertEquals(o, AEItemKey.of(Items.BRICK), iface.getStorage().getKey(1));
                    })
                    .thenSucceed();
        });
    }

    private static SlottedStorage<ItemVariant> itemStorage(PlotTestHelper helper, BlockPos ref) {
        var storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(ref), Direction.UP);
        helper.check(storage instanceof SlottedStorage, "interface should expose a slotted item storage", ref);
        return (SlottedStorage<ItemVariant>) storage;
    }

    private static SlottedStorage<FluidVariant> fluidStorage(PlotTestHelper helper, BlockPos ref) {
        var storage = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(ref), Direction.UP);
        helper.check(storage instanceof SlottedStorage, "interface should expose a slotted fluid storage", ref);
        return (SlottedStorage<FluidVariant>) storage;
    }

    /**
     * Simulated per-slot insertion: opens an outer transaction, attempts the insert, and rolls it back (the transaction
     * is never committed). Returns the amount that <em>would</em> be accepted, so a return of 0 means the slot's config
     * filter (or a fluid-into-item-slot mismatch) rejected the resource.
     */
    private static <V> long simulateInsert(SingleSlotStorage<V> slot, V resource, long amount) {
        try (var tx = Transaction.openOuter()) {
            return slot.insert(resource, amount, tx);
        }
    }
}
