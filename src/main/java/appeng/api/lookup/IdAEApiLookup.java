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

import java.util.Objects;
import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An {@link AEApiLookup} identified by id and API class that lazily resolves the loader-specific lookup through
 * {@link AEApiLookups} on first use. This allows the lookup constants (e.g. in {@link appeng.api.AECapabilities}) to be
 * class-initialized before the loader-specific backend is injected.
 */
final class IdAEApiLookup<A> implements AEApiLookup<A> {
    private final Identifier id;
    private final Class<A> apiClass;
    private final boolean sided;

    @Nullable
    private volatile AEApiLookup<A> delegate;

    IdAEApiLookup(Identifier id, Class<A> apiClass, boolean sided) {
        this.id = Objects.requireNonNull(id, "id");
        this.apiClass = Objects.requireNonNull(apiClass, "apiClass");
        this.sided = sided;
    }

    private AEApiLookup<A> delegate() {
        var result = delegate;
        if (result == null) {
            synchronized (this) {
                result = delegate;
                if (result == null) {
                    result = AEApiLookups.get().createLookup(id, apiClass, sided);
                    delegate = result;
                }
            }
        }
        return result;
    }

    @Override
    @Nullable
    public A find(Level level, BlockPos pos, @Nullable Direction side) {
        return delegate().find(level, pos, side);
    }

    @Override
    @Nullable
    public A find(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            @Nullable Direction side) {
        return delegate().find(level, pos, state, blockEntity, side);
    }

    @Override
    public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        return delegate().createCache(level, pos, side);
    }

    @Override
    public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side,
            BooleanSupplier isValid, Runnable invalidationListener) {
        return delegate().createCache(level, pos, side, isValid, invalidationListener);
    }

    @Override
    public String toString() {
        return "AEApiLookup[" + id + "]";
    }
}
