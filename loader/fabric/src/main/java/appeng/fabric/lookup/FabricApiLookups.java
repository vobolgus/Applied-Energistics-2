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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import appeng.api.lookup.AEApiCache;
import appeng.api.lookup.AEApiInvalidationListener;
import appeng.api.lookup.AEApiLookup;
import appeng.api.lookup.AEApiLookups;

/**
 * Fabric implementation of the {@link AEApiLookups} seam, backed by {@link BlockApiLookup} and {@link BlockApiCache}.
 * <p>
 * <strong>Behavior notes (vs. NeoForge):</strong>
 * <ul>
 * <li>The Fabric API lookup system has no invalidation notifications, so this class maintains its own per-level,
 * per-position registry of {@linkplain #registerInvalidationListener invalidation listeners} (weakly held, mirroring
 * NeoForge's contract) and delivers notifications from {@link #invalidateApis} — i.e. from AE2's own
 * {@code invalidateCapabilities()} call sites (interface grid changes, part/orientation changes, ...). This is what
 * wakes a sleeping storage bus when the AE2 machine it points at becomes available, exactly like the NeoForge
 * capability invalidation system does. The remaining delta to NeoForge: placing/removing <em>non-AE2</em> blocks does
 * not produce an invalidation (NeoForge auto-invalidates on any block change); those cases are covered by the vanilla
 * neighbor-update path instead ({@code onNeighborChanged} alerts the device, which then re-queries).</li>
 * <li>{@link BlockApiCache} re-validates the block entity and provider on every query, so consumers that re-query
 * always observe changes even without a notification.</li>
 * <li>{@link #hasNonEmptyItemHandler}/{@link #hasNonEmptyFluidHandler} approximate the NeoForge {@code size()} checks:
 * a found Fabric storage is considered non-empty if it is not a {@link SlottedStorage} with zero slots (the Fabric API
 * has no slot-count query for generic storages).</li>
 * </ul>
 */
public final class FabricApiLookups implements AEApiLookups {

    /**
     * Per-level, per-position invalidation listeners. Levels are keyed weakly (a ServerLevel lives for the duration of
     * the server, but dev-time server restarts in the same JVM must not leak), listeners are held weakly per the
     * NeoForge contract (the caller keeps the strong reference). Only ever touched on the server thread, matching
     * NeoForge's thread-affinity for capability invalidation.
     */
    private static final Map<ServerLevel, Long2ObjectMap<List<WeakReference<AEApiInvalidationListener>>>> INVALIDATION_LISTENERS = new WeakHashMap<>();

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
        addInvalidationListener(level, pos, listener);
    }

    private static void addInvalidationListener(ServerLevel level, BlockPos pos,
            AEApiInvalidationListener listener) {
        INVALIDATION_LISTENERS
                .computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(pos.asLong(), ignored -> new ArrayList<>(2))
                .add(new WeakReference<>(listener));
    }

    @Override
    public void invalidateApis(BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        var levelListeners = INVALIDATION_LISTENERS.get(serverLevel);
        if (levelListeners == null) {
            return;
        }
        var posKey = blockEntity.getBlockPos().asLong();
        var listeners = levelListeners.get(posKey);
        if (listeners == null) {
            return;
        }
        listeners.removeIf(ref -> {
            var listener = ref.get();
            return listener == null || !listener.onInvalidate();
        });
        if (listeners.isEmpty()) {
            levelListeners.remove(posKey);
        }
    }

    /**
     * Builds the weakly-registered listener for the {@code createCache} overload taking an invalidation listener,
     * mirroring NeoForge's {@code BlockCapabilityCache} semantics: on invalidation, a cache that is no longer valid is
     * permanently unregistered, otherwise the listener is notified. The returned wrapper must be strongly referenced by
     * the cache for as long as it lives.
     */
    private static AEApiInvalidationListener createCacheListener(ServerLevel level, BlockPos pos,
            BooleanSupplier isValid, Runnable invalidationListener) {
        AEApiInvalidationListener wrapper = () -> {
            if (!isValid.getAsBoolean()) {
                return false;
            }
            invalidationListener.run();
            return true;
        };
        addInvalidationListener(level, pos, wrapper);
        return wrapper;
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
            return new Cache<>(BlockApiCache.create(lookup, level, pos), side, null);
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side,
                BooleanSupplier isValid, Runnable invalidationListener) {
            return new Cache<>(BlockApiCache.create(lookup, level, pos), side,
                    createCacheListener(level, pos, isValid, invalidationListener));
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
            return new Cache<>(BlockApiCache.create(lookup, level, pos), null, null);
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side,
                BooleanSupplier isValid, Runnable invalidationListener) {
            return new Cache<>(BlockApiCache.create(lookup, level, pos), null,
                    createCacheListener(level, pos, isValid, invalidationListener));
        }
    }

    /**
     * @param registeredListener strong reference keeping the (weakly registered) invalidation listener of this cache
     *                           alive; unused otherwise.
     */
    private record Cache<A, C>(BlockApiCache<A, C> cache, @Nullable C context,
            @Nullable AEApiInvalidationListener registeredListener) implements AEApiCache<A> {
        @Override
        @Nullable
        public A get() {
            return cache.find(context);
        }
    }
}
