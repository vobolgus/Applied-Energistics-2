/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.server.services;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Loader-neutral seam for the chunk-loading tickets used by the spatial anchor. The loader-specific implementation
 * (e.g. {@code appeng.neoforge.service.NeoForgeChunkLoadingService}, which is backed by NeoForge's ticket controller)
 * is injected once during mod construction via {@link #init}.
 */
public interface ChunkLoadingService {

    /**
     * Forces the given chunk to stay loaded on behalf of the block at the given owner position.
     *
     * @return True if the ticket was newly added.
     */
    boolean forceChunk(ServerLevel level, BlockPos owner, ChunkPos position);

    /**
     * Releases a chunk-loading ticket previously added via {@link #forceChunk}.
     *
     * @return True if a ticket was removed.
     */
    boolean releaseChunk(ServerLevel level, BlockPos owner, ChunkPos position);

    default boolean isChunkForced(ServerLevel level, int chunkX, int chunkZ) {
        return ChunkLoadState.get(level).isForceLoaded(chunkX, chunkZ);
    }

    static ChunkLoadingService getInstance() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific ChunkLoadingService has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(ChunkLoadingService service) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The ChunkLoadingService has already been initialized");
        }
        Holder.INSTANCE = service;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile ChunkLoadingService INSTANCE;

        private Holder() {
        }
    }
}
