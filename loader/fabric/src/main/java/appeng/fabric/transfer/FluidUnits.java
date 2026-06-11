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

package appeng.fabric.transfer;

import com.google.common.primitives.Ints;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;

/**
 * The single place where AE2's internal fluid unit (millibuckets, 1000 per bucket — matching the NeoForge transfer API)
 * is converted to and from the Fabric transfer API's droplets (81000 per bucket).
 * <p>
 * <strong>Rounding policy:</strong> conversions are always rounded such that AE2 never reports or moves more fluid than
 * the underlying numbers represent:
 * <ul>
 * <li>millibuckets → droplets is exact ({@code mb * 81}).</li>
 * <li>droplets → millibuckets rounds <em>down</em> ({@code droplets / 81}, floor). This applies in both transfer
 * directions: when AE2 receives a droplet amount (insert request, reported storage content), the sub-millibucket
 * remainder is dropped, so up to 80 droplets per slot/operation may be ignored. This mirrors the granularity AE2 had on
 * NeoForge, where sub-millibucket amounts do not exist at all.</li>
 * </ul>
 * No other class may use the literals 81 / 81000 for fluid conversions.
 */
public final class FluidUnits {
    /**
     * Droplets per millibucket.
     */
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private FluidUnits() {
    }

    /**
     * Converts an AE2-internal millibucket amount to Fabric droplets (exact).
     */
    public static long mbToDroplets(long mb) {
        return mb * DROPLETS_PER_MB;
    }

    /**
     * Converts a Fabric droplet amount to AE2-internal millibuckets, rounding down.
     */
    public static long dropletsToMb(long droplets) {
        return droplets / DROPLETS_PER_MB;
    }

    /**
     * Converts a Fabric droplet amount to AE2-internal millibuckets, rounding down and saturating to int (the NeoForge
     * transfer API and AE2's internal fluid handling use int amounts).
     */
    public static int dropletsToMbInt(long droplets) {
        return Ints.saturatedCast(dropletsToMb(droplets));
    }
}
