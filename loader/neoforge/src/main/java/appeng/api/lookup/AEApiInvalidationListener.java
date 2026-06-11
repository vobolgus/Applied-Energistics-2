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

import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;

/**
 * Listener for block API invalidation at a position, registered via {@link AEApiLookups#registerInvalidationListener}.
 * <p>
 * This is a <strong>loader-duplicated</strong> interface: on NeoForge it simply extends
 * {@link ICapabilityInvalidationListener} so listeners can be registered with the capability invalidation system
 * directly (preserving its weak-reference semantics exactly); the Fabric version declares the single
 * {@code boolean onInvalidate()} method itself and is never invoked (Fabric has no invalidation notifications).
 * <p>
 * Contract (mirroring NeoForge): the listener is held weakly, so keep a strong reference to it as long as it should
 * stay registered. Return true to stay registered, false to be removed.
 */
@FunctionalInterface
public interface AEApiInvalidationListener extends ICapabilityInvalidationListener {
}
