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

package appeng.parts.p2p;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import appeng.api.parts.IPartItem;
import appeng.fabric.transfer.VariantConversion;

/**
 * The Fabric twin of the NeoForge fluid P2P tunnel, based on the Fabric fluid {@code Storage}.
 */
public class FluidP2PTunnelPart extends StorageP2PTunnelPart<FluidP2PTunnelPart, FluidVariant> {
    public FluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, FluidStorage.SIDED, VariantConversion.FLUID);
    }
}
