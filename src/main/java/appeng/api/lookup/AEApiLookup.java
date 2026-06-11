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

package appeng.api.lookup;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.AECapabilities;

/**
 * Loader-neutral handle for a sided block API lookup ("find an instance of API {@code A} at a given block position,
 * from a given side").
 * <p>
 * On NeoForge this is backed by a {@code BlockCapability} (and {@code BlockCapabilityCache} for the
 * {@linkplain #createCache cached form}), on Fabric it will be backed by a {@code BlockApiLookup} (and
 * {@code BlockApiCache}). The lookups for the APIs provided by AE2 itself are exposed in {@link AECapabilities}.
 */
public interface AEApiLookup<A> {

    /**
     * Finds an instance of the API at the given position, or returns null if there is none.
     *
     * @param side the side of the block the API is requested from, or null for an unspecified/internal query. Ignored
     *             by {@linkplain #unsided unsided} lookups.
     */
    @Nullable
    A find(Level level, BlockPos pos, @Nullable Direction side);

    /**
     * Variant of {@link #find(Level, BlockPos, Direction)} for use when the block state and block entity at the
     * position are already known, to avoid looking them up again.
     */
    @Nullable
    A find(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Direction side);

    /**
     * Creates a cache for repeated API queries bound to a fixed position and side. Equivalent to
     * {@link #createCache(ServerLevel, BlockPos, Direction, BooleanSupplier, Runnable)} with a cache that is always
     * valid and does not care about invalidation notifications.
     */
    AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side);

    /**
     * Creates a cache for repeated API queries bound to a fixed position and side.
     * <p>
     * NeoForge {@code BlockCapabilityCache} semantics apply (on Fabric, where no invalidation notifications exist, the
     * cache simply re-queries and the listener is never invoked):
     * <ul>
     * <li>{@link AEApiCache#get()} returns null while the target position is not loaded.</li>
     * <li>{@code isValid} is checked when an invalidation notification arrives; once it returns false, the cache is
     * permanently unregistered and {@link AEApiCache#get()} must not be called anymore.</li>
     * <li>{@code invalidationListener} is invoked when the cached API may have changed. Do not query the cache from
     * within the listener; re-query later (e.g. on the next tick).</li>
     * </ul>
     */
    AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side, BooleanSupplier isValid,
            Runnable invalidationListener);

    /**
     * Creates the handle for a sided block API lookup with the given id and API class. Handles with the same id and API
     * class refer to the same underlying loader lookup.
     */
    static <A> AEApiLookup<A> sided(Identifier id, Class<A> apiClass) {
        return new IdAEApiLookup<>(id, apiClass, true);
    }

    /**
     * Creates the handle for an unsided (void context) block API lookup with the given id and API class. The side
     * argument of the find methods is ignored by these lookups.
     */
    static <A> AEApiLookup<A> unsided(Identifier id, Class<A> apiClass) {
        return new IdAEApiLookup<>(id, apiClass, false);
    }
}
