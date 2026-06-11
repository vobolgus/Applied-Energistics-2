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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.fabric.transfer.FabricHandlerStrategy;

/**
 * Exports stacks from the network into adjacent Fabric storages. The Fabric twin of
 * {@code appeng.parts.automation.StorageExportStrategy}.
 */
public class FabricStorageExportStrategy<V extends TransferVariant<?>> implements StackExportStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(FabricStorageExportStrategy.class);
    private final BlockApiCache<Storage<V>, Direction> cache;
    private final Direction fromSide;
    private final FabricHandlerStrategy<V> handlerStrategy;

    public FabricStorageExportStrategy(FabricHandlerStrategy<V> handlerStrategy,
            ServerLevel level,
            BlockPos fromPos,
            Direction fromSide) {
        this.handlerStrategy = handlerStrategy;
        this.cache = BlockApiCache.create(handlerStrategy.getLookup(), level, fromPos);
        this.fromSide = fromSide;
    }

    @Override
    public long transfer(StackTransferContext context, AEKey what, long amount) {
        if (!handlerStrategy.isSupported(what)) {
            return 0;
        }

        var adjacentStorage = cache.find(fromSide);
        if (adjacentStorage == null) {
            return 0;
        }

        var inv = context.getInternalStorage();

        var extracted = StorageHelper.poweredExtraction(
                context.getEnergySource(),
                inv.getInventory(),
                what,
                amount,
                context.getActionSource(),
                Actionable.SIMULATE);

        long wasInserted = handlerStrategy.insert(adjacentStorage, what, extracted, Actionable.SIMULATE);

        if (wasInserted > 0) {
            extracted = StorageHelper.poweredExtraction(
                    context.getEnergySource(),
                    inv.getInventory(),
                    what,
                    wasInserted,
                    context.getActionSource(),
                    Actionable.MODULATE);

            wasInserted = handlerStrategy.insert(adjacentStorage, what, extracted, Actionable.MODULATE);

            if (wasInserted < extracted) {
                // Be nice and try to give the overflow back
                long leftover = extracted - wasInserted;
                leftover -= inv.getInventory().insert(what, leftover, Actionable.MODULATE, context.getActionSource());
                if (leftover > 0) {
                    LOG.error("Storage export: adjacent block unexpectedly refused insert, voided {}x{}", leftover,
                            what);
                }
            }
        }

        return wasInserted;
    }

    @Override
    public long push(AEKey what, long amount, Actionable mode) {
        if (!handlerStrategy.isSupported(what)) {
            return 0;
        }

        var adjacentStorage = cache.find(fromSide);
        if (adjacentStorage == null) {
            return 0;
        }

        return handlerStrategy.insert(adjacentStorage, what, amount, mode);
    }

    public static StackExportStrategy createItem(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new FabricStorageExportStrategy<>(FabricHandlerStrategy.ITEMS, level, fromPos, fromSide);
    }

    public static StackExportStrategy createFluid(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new FabricStorageExportStrategy<>(FabricHandlerStrategy.FLUIDS, level, fromPos, fromSide);
    }
}
