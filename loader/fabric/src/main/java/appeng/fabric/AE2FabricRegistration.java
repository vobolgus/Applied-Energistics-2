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

package appeng.fabric;

import appeng.api.parts.RegisterPartApiEvent;

/**
 * Fabric entrypoint through which addons expose block APIs on cable-bus parts — the Fabric counterpart
 * to subscribing to NeoForge's {@code RegisterPartCapabilitiesEvent}. Declare an implementation under
 * the {@value #ENTRYPOINT} entrypoint key in your {@code fabric.mod.json}:
 *
 * <pre>{@code
 * "entrypoints": {
 *   "ae2:registration": [ "com.example.MyAddonRegistration" ]
 * }
 * }</pre>
 *
 * AE2 invokes every registered entrypoint exactly once during {@code InitApiLookup.init()}, after its
 * own part APIs are registered (see {@link RegisterPartApiEvent}).
 * <p>
 * This mirrors the client-side {@code ae2:client_registration} entrypoint; keep it to server-safe
 * (common) registrations only — it runs on both the client and the dedicated server.
 */
public interface AE2FabricRegistration {
    /**
     * The {@code fabric.mod.json} entrypoint key addons declare their implementations under.
     */
    String ENTRYPOINT = "ae2:registration";

    /**
     * Called once at init so the addon can register its part-API providers on the given event.
     */
    default void registerPartApis(RegisterPartApiEvent event) {
    }
}
