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

import org.jetbrains.annotations.Nullable;

/**
 * A cache for an {@link AEApiLookup} bound to a fixed position and side, created via {@link AEApiLookup#createCache}.
 *
 * @see AEApiLookup#createCache(net.minecraft.server.level.ServerLevel, net.minecraft.core.BlockPos,
 *      net.minecraft.core.Direction, java.util.function.BooleanSupplier, Runnable) for the validity/invalidation
 *      semantics
 */
public interface AEApiCache<A> {
    /**
     * @return The API instance at the bound position, or null if there is none or the position is not loaded.
     */
    @Nullable
    A get();
}
