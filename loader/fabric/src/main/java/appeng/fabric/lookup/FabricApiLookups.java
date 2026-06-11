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

package appeng.fabric.lookup;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.lookup.AEApiCache;
import appeng.api.lookup.AEApiInvalidationListener;
import appeng.api.lookup.AEApiLookup;
import appeng.api.lookup.AEApiLookups;

/**
 * Fabric implementation of the {@link AEApiLookups} seam, backed by {@link BlockApiLookup} and {@link BlockApiCache}.
 * <p>
 * <strong>Behavior notes (vs. NeoForge):</strong>
 * <ul>
 * <li>The Fabric API lookup system has no invalidation notifications. {@link #registerInvalidationListener} is
 * therefore a no-op and the {@code invalidationListener} passed to
 * {@link AEApiLookup#createCache(ServerLevel, BlockPos, Direction, BooleanSupplier, Runnable)} is never invoked. Caches
 * degrade gracefully: {@link BlockApiCache} re-validates the block entity and provider on every query, so consumers
 * that re-query (as all AE2 consumers do on tick/neighbor updates) observe changes, just without the eager
 * notification.</li>
 * <li>{@link #hasNonEmptyItemHandler}/{@link #hasNonEmptyFluidHandler} approximate the NeoForge {@code size()} checks:
 * a found Fabric storage is considered non-empty if it is not a {@link SlottedStorage} with zero slots (the Fabric API
 * has no slot-count query for generic storages).</li>
 * </ul>
 */
public final class FabricApiLookups implements AEApiLookups {

    /**
     * Wraps an existing sided {@link BlockApiLookup} as an {@link AEApiLookup}, e.g. for use with
     * {@link appeng.parts.PartAdjacentApi}.
     */
    public static <A> AEApiLookup<A> of(BlockApiLookup<A, Direction> lookup) {
        return new SidedLookup<>(lookup);
    }

    /**
     * Wraps an existing void-context {@link BlockApiLookup} as an (unsided) {@link AEApiLookup}.
     */
    public static <A> AEApiLookup<A> ofVoid(BlockApiLookup<A, Void> lookup) {
        return new VoidLookup<>(lookup);
    }

    @Override
    public <A> AEApiLookup<A> createLookup(Identifier id, Class<A> apiClass, boolean sided) {
        // BlockApiLookup.get returns the existing lookup instance if one with the same id and classes
        // was already created.
        if (sided) {
            return of(BlockApiLookup.get(id, apiClass, Direction.class));
        } else {
            return ofVoid(BlockApiLookup.get(id, apiClass, Void.class));
        }
    }

    @Override
    public void registerInvalidationListener(ServerLevel level, BlockPos pos, AEApiInvalidationListener listener) {
        // No-op: Fabric has no API invalidation notifications. Consumers (e.g. the storage bus) rely on
        // their tick/neighbor-update re-query paths instead. See the class javadoc.
    }

    @Override
    public void invalidateApis(BlockEntity blockEntity) {
        // No-op: Fabric API lookups are resolved (and BlockApiCache re-validated) on every query, so there is
        // nothing to invalidate eagerly. See the class javadoc.
    }

    @Override
    public boolean hasNonEmptyItemHandler(Level level, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        var storage = ItemStorage.SIDED.find(level, pos, state, blockEntity, side);
        return isNonEmptyStorage(storage);
    }

    @Override
    public boolean hasNonEmptyFluidHandler(Level level, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        var storage = FluidStorage.SIDED.find(level, pos, state, blockEntity, side);
        return isNonEmptyStorage(storage);
    }

    private static boolean isNonEmptyStorage(@Nullable Storage<?> storage) {
        if (storage == null) {
            return false;
        }
        if (storage instanceof SlottedStorage<?> slotted) {
            return slotted.getSlotCount() > 0;
        }
        return true;
    }

    private record SidedLookup<A>(BlockApiLookup<A, Direction> lookup) implements AEApiLookup<A> {
        @Override
        @Nullable
        public A find(Level level, BlockPos pos, @Nullable Direction side) {
            return lookup.find(level, pos, side);
        }

        @Override
        @Nullable
        public A find(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                @Nullable Direction side) {
            return lookup.find(level, pos, state, blockEntity, side);
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side) {
            return new Cache<>(BlockApiCache.create(lookup, level, pos), side);
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side,
                BooleanSupplier isValid, Runnable invalidationListener) {
            // The invalidation listener is never invoked on Fabric, see the class javadoc.
            return new Cache<>(BlockApiCache.create(lookup, level, pos), side);
        }
    }

    private record VoidLookup<A>(BlockApiLookup<A, Void> lookup) implements AEApiLookup<A> {
        @Override
        @Nullable
        public A find(Level level, BlockPos pos, @Nullable Direction side) {
            return lookup.find(level, pos, null);
        }

        @Override
        @Nullable
        public A find(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                @Nullable Direction side) {
            return lookup.find(level, pos, state, blockEntity, null);
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side) {
            return new Cache<>(BlockApiCache.create(lookup, level, pos), null);
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side,
                BooleanSupplier isValid, Runnable invalidationListener) {
            return new Cache<>(BlockApiCache.create(lookup, level, pos), null);
        }
    }

    private record Cache<A, C>(BlockApiCache<A, C> cache, @Nullable C context) implements AEApiCache<A> {
        @Override
        @Nullable
        public A get() {
            return cache.find(context);
        }
    }
}
