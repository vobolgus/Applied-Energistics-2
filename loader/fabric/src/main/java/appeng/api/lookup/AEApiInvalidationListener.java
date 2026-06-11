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

/**
 * Listener for block API invalidation at a position, registered via {@link AEApiLookups#registerInvalidationListener}.
 * <p>
 * This is a <strong>loader-duplicated</strong> interface: on NeoForge it extends
 * {@code ICapabilityInvalidationListener} so listeners can be registered with the capability invalidation system
 * directly; this Fabric version declares the single method itself and is <strong>never invoked</strong>, because the
 * Fabric API lookup system has no invalidation notifications — consumers must re-query instead (see
 * {@code appeng.fabric.lookup.FabricApiLookups}).
 */
@FunctionalInterface
public interface AEApiInvalidationListener {
    /**
     * Notifies the listener that the APIs at the position it was registered for may have changed.
     *
     * @return true to stay registered, false to be removed.
     */
    boolean onInvalidate();
}
