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

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.storage.MEStorage;
import appeng.fabric.transfer.FabricHandlerStrategy;

/**
 * Exposes adjacent Fabric storages as {@link MEStorage}s (for the storage bus). The Fabric twin of
 * {@code appeng.parts.automation.ForgeExternalStorageStrategy}.
 */
public class FabricExternalStorageStrategy<V extends TransferVariant<?>> implements ExternalStorageStrategy {
    private final BlockApiCache<Storage<V>, Direction> cache;
    private final Direction fromSide;
    private final FabricHandlerStrategy<V> conversion;

    public FabricExternalStorageStrategy(FabricHandlerStrategy<V> conversion,
            ServerLevel level,
            BlockPos fromPos,
            Direction fromSide) {
        this.cache = BlockApiCache.create(conversion.getLookup(), level, fromPos);
        this.fromSide = fromSide;
        this.conversion = conversion;
    }

    @Nullable
    @Override
    public MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback) {
        var storage = cache.find(fromSide);
        if (storage == null) {
            return null;
        }

        var result = conversion.getFacade(storage);
        result.setChangeListener(injectOrExtractCallback);
        result.setExtractableOnly(extractableOnly);
        return result;
    }

    public static ExternalStorageStrategy createItem(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new FabricExternalStorageStrategy<>(FabricHandlerStrategy.ITEMS, level, fromPos, fromSide);
    }

    public static ExternalStorageStrategy createFluid(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new FabricExternalStorageStrategy<>(FabricHandlerStrategy.FLUIDS, level, fromPos, fromSide);
    }
}
