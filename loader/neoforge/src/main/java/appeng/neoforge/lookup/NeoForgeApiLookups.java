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

package appeng.neoforge.lookup;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;

import appeng.api.lookup.AEApiCache;
import appeng.api.lookup.AEApiInvalidationListener;
import appeng.api.lookup.AEApiLookup;
import appeng.api.lookup.AEApiLookups;

/**
 * NeoForge implementation of the {@link AEApiLookups} seam, backed by {@link BlockCapability} and
 * {@link BlockCapabilityCache}.
 */
public final class NeoForgeApiLookups implements AEApiLookups {

    /**
     * Wraps an existing sided {@link BlockCapability} as an {@link AEApiLookup}, e.g. for use with
     * {@link appeng.parts.PartAdjacentApi}.
     */
    public static <A> AEApiLookup<A> of(BlockCapability<A, @Nullable Direction> capability) {
        return new SidedLookup<>(capability);
    }

    /**
     * Wraps an existing void-context {@link BlockCapability} as an (unsided) {@link AEApiLookup}.
     */
    public static <A> AEApiLookup<A> ofVoid(BlockCapability<A, Void> capability) {
        return new VoidLookup<>(capability);
    }

    @Override
    public <A> AEApiLookup<A> createLookup(Identifier id, Class<A> apiClass, boolean sided) {
        // BlockCapability.createSided/createVoid return the existing capability instance if one with the
        // same id and classes was already created (e.g. by appeng.neoforge.AENeoForgeCapabilities).
        if (sided) {
            return of(BlockCapability.createSided(id, apiClass));
        } else {
            return ofVoid(BlockCapability.createVoid(id, apiClass));
        }
    }

    @Override
    public void registerInvalidationListener(ServerLevel level, BlockPos pos, AEApiInvalidationListener listener) {
        // AEApiInvalidationListener extends ICapabilityInvalidationListener on NeoForge, so the listener can be
        // registered directly, preserving the weak-reference semantics of the invalidation system exactly.
        level.registerCapabilityListener(pos, listener);
    }

    @Override
    public void invalidateApis(BlockEntity blockEntity) {
        blockEntity.invalidateCapabilities();
    }

    @Override
    public boolean hasNonEmptyItemHandler(Level level, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        var itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, state, blockEntity, side);
        return itemHandler != null && itemHandler.size() > 0;
    }

    @Override
    public boolean hasNonEmptyFluidHandler(Level level, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, @Nullable Direction side) {
        var fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, pos, state, blockEntity, side);
        return fluidHandler != null && fluidHandler.size() != 0;
    }

    private record SidedLookup<A>(BlockCapability<A, @Nullable Direction> capability) implements AEApiLookup<A> {
        @Override
        @Nullable
        public A find(Level level, BlockPos pos, @Nullable Direction side) {
            return level.getCapability(capability, pos, side);
        }

        @Override
        @Nullable
        public A find(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                @Nullable Direction side) {
            return level.getCapability(capability, pos, state, blockEntity, side);
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side) {
            return new Cache<>(BlockCapabilityCache.create(capability, level, pos, side));
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side,
                BooleanSupplier isValid, Runnable invalidationListener) {
            return new Cache<>(
                    BlockCapabilityCache.create(capability, level, pos, side, isValid, invalidationListener));
        }
    }

    private record VoidLookup<A>(BlockCapability<A, Void> capability) implements AEApiLookup<A> {
        @Override
        @Nullable
        public A find(Level level, BlockPos pos, @Nullable Direction side) {
            return level.getCapability(capability, pos, null);
        }

        @Override
        @Nullable
        public A find(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                @Nullable Direction side) {
            return level.getCapability(capability, pos, state, blockEntity, null);
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side) {
            return new Cache<>(BlockCapabilityCache.create(capability, level, pos, null));
        }

        @Override
        public AEApiCache<A> createCache(ServerLevel level, BlockPos pos, @Nullable Direction side,
                BooleanSupplier isValid, Runnable invalidationListener) {
            return new Cache<>(
                    BlockCapabilityCache.create(capability, level, pos, null, isValid, invalidationListener));
        }
    }

    private record Cache<A, C extends @Nullable Object>(BlockCapabilityCache<A, C> cache) implements AEApiCache<A> {
        @Override
        @Nullable
        public A get() {
            return cache.getCapability();
        }
    }
}
