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

package appeng.parts.automation;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import appeng.api.behaviors.StackImportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.core.AELog;
import appeng.fabric.transfer.FabricHandlerStrategy;

/**
 * Imports stacks from adjacent Fabric storages into the network. The Fabric twin of
 * {@code appeng.parts.automation.StorageImportStrategy}.
 */
public class FabricStorageImportStrategy<V extends TransferVariant<?>> implements StackImportStrategy {
    private final BlockApiCache<Storage<V>, Direction> cache;
    private final Direction fromSide;
    private final FabricHandlerStrategy<V> conversion;

    public FabricStorageImportStrategy(FabricHandlerStrategy<V> conversion,
            ServerLevel level,
            BlockPos fromPos,
            Direction fromSide) {
        this.cache = BlockApiCache.create(conversion.getLookup(), level, fromPos);
        this.fromSide = fromSide;
        this.conversion = conversion;
    }

    @Override
    public boolean transfer(StackTransferContext context) {
        if (!context.isKeyTypeEnabled(conversion.getKeyType())) {
            return false;
        }

        var adjacentHandler = cache.find(fromSide);
        if (adjacentHandler == null) {
            return false;
        }

        var adjacentStorage = conversion.getFacade(adjacentHandler);

        long remainingTransferAmount = context.getOperationsRemaining()
                * (long) conversion.getKeyType().getAmountPerOperation();

        var inv = context.getInternalStorage();

        // Try to find an extractable resource that fits our filter
        for (int i = 0; i < adjacentStorage.getSlots() && remainingTransferAmount > 0; i++) {
            var resource = adjacentStorage.getStackInSlot(i);
            if (resource == null
                    // Regard a filter that is set on the bus
                    || context.isInFilter(resource.what()) == context.isInverted()) {
                continue;
            }

            // Check how much of *this* resource we can actually insert into the network, it might be 0
            // if the cells are partitioned or there's not enough types left, etc.
            var amountForThisResource = inv.getInventory().insert(resource.what(), remainingTransferAmount,
                    Actionable.SIMULATE,
                    context.getActionSource());

            // Try to extract it
            var amount = adjacentStorage.extract(resource.what(), amountForThisResource, Actionable.MODULATE,
                    context.getActionSource());
            if (amount > 0) {
                var inserted = inv.getInventory().insert(resource.what(), amount, Actionable.MODULATE,
                        context.getActionSource());

                if (inserted < amount) {
                    // Be nice and try to give the overflow back
                    long leftover = amount - inserted;
                    leftover -= adjacentStorage.insert(resource.what(), leftover, Actionable.MODULATE,
                            context.getActionSource());
                    if (leftover > 0) {
                        AELog.warn("Extracted %dx%s from adjacent storage and voided it because network refused insert",
                                leftover, resource.what());
                    }
                }

                var opsUsed = Math.max(1, inserted / conversion.getKeyType().getAmountPerOperation());
                context.reduceOperationsRemaining(opsUsed);
                remainingTransferAmount -= inserted;
            }
        }

        return false;
    }

    public static StackImportStrategy createItem(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new FabricStorageImportStrategy<>(FabricHandlerStrategy.ITEMS, level, fromPos, fromSide);
    }

    public static StackImportStrategy createFluid(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new FabricStorageImportStrategy<>(FabricHandlerStrategy.FLUIDS, level, fromPos, fromSide);
    }
}
