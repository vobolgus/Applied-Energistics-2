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

package appeng.fabric.gametest;

import appeng.server.testworld.GameTestPlotAdapter;

/**
 * Placeholder for AE2's plot-based game test registration on Fabric.
 * <p>
 * TODO (fabric, runtime/test step): NeoForge registers the dynamically generated {@code GameTestInstance}s via
 * {@code RegisterGameTestsEvent} (gated on the {@code appeng.tests} system property). The Fabric game test API v1 on
 * 26.1 only discovers {@code @GameTest}-annotated entrypoint methods and has no dynamic-registration hook for
 * {@link GameTestPlotAdapter#registerAll}; this needs either a small mixin into the test-instance registry data loading
 * or an upstream Fabric API addition.
 */
public final class AEFabricGameTests {
    private AEFabricGameTests() {
    }
}
