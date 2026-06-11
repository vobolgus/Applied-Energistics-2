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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loader-neutral seam for the block API lookup machinery (NeoForge capabilities, Fabric API lookup). The
 * loader-specific implementation (e.g. {@code appeng.neoforge.lookup.NeoForgeApiLookups}) is injected once during mod
 * construction via {@link #init}.
 */
public interface AEApiLookups {
    /**
     * Resolves the loader-specific lookup for the given id/API class. Used internally by handles created via
     * {@link AEApiLookup#sided}/{@link AEApiLookup#unsided}; do not call directly.
     */
    @ApiStatus.Internal
    <A> AEApiLookup<A> createLookup(Identifier id, Class<A> apiClass, boolean sided);

    /**
     * Registers a listener that is notified when the APIs available at the given position may have changed.
     * <p>
     * NeoForge capability-invalidation semantics apply: the listener is only held <strong>weakly</strong>, so the
     * caller must keep a strong reference to it for as long as it should stay registered. Returning false from the
     * listener unregisters it. On Fabric, where no invalidation notifications exist, this is a no-op.
     */
    void registerInvalidationListener(ServerLevel level, BlockPos pos, AEApiInvalidationListener listener);

    /**
     * Notifies the loader that the set of APIs exposed by the given block entity may have changed (was NeoForge's
     * {@code BlockEntity#invalidateCapabilities()}). On Fabric, where lookups are uncached, this is a no-op.
     */
    void invalidateApis(BlockEntity blockEntity);

    /**
     * {@return whether the given position exposes a loader item handler with at least one slot from the given side}
     * <p>
     * This is a semantic query rather than a typed {@link AEApiLookup} because there is no loader-neutral type for
     * external item handlers (yet).
     */
    boolean hasNonEmptyItemHandler(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            @Nullable Direction side);

    /**
     * {@return whether the given position exposes a loader fluid handler with at least one slot from the given side}
     */
    boolean hasNonEmptyFluidHandler(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            @Nullable Direction side);

    static AEApiLookups get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific AEApiLookups has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(AEApiLookups lookups) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The AEApiLookups has already been initialized");
        }
        Holder.INSTANCE = lookups;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile AEApiLookups INSTANCE;

        private Holder() {
        }
    }
}
