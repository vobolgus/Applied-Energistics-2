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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.server.testworld.PlotBuilder;
import appeng.server.testworld.PlotTestHelper;

/**
 * Interface test plots that assert against the NeoForge item/fluid block capabilities. Split out of
 * {@link InterfaceTestPlots} because they depend on loader-specific transfer APIs.
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
                        var itemCap = getCapability(helper, o, Capabilities.Item.BLOCK, Direction.UP);
                        helper.check(itemCap.isValid(0, ItemResource.of(Items.STICK)),
                                "stick should be valid in slot 0");
                        helper.check(itemCap.isValid(1, ItemResource.of(Items.STICK)),
                                "stick should be valid in slot 1");
                        helper.check(!itemCap.isValid(0, ItemResource.of(Blocks.BRICKS)),
                                "bricks should not be valid in slot 0");
                        helper.check(itemCap.isValid(1, ItemResource.of(Blocks.BRICKS)),
                                "bricks should be valid in slot 1");

                        var fluidCap = getCapability(helper, o, Capabilities.Fluid.BLOCK, Direction.UP);
                        helper.check(!fluidCap.isValid(0, FluidResource.of(Fluids.WATER)),
                                "fluid should not be valid in slot 0");
                        helper.check(fluidCap.isValid(1, FluidResource.of(Fluids.WATER)),
                                "fluid should be valid in slot 1");
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

    private static <T, C> T getCapability(PlotTestHelper helper, BlockPos ref, BlockCapability<T, C> cap, C context) {
        return helper.getLevel().getCapability(cap, helper.absolutePos(ref), context);
    }
}
