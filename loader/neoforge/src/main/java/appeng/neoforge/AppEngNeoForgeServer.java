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

package appeng.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

import appeng.core.AppEng;
import appeng.core.AppEngServer;

/**
 * The NeoForge mod entrypoint for the dedicated server (the client entrypoint is
 * {@code appeng.neoforge.client.AppEngNeoForgeClient}).
 */
@Mod(value = AppEng.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class AppEngNeoForgeServer extends AppEngServer {
    public AppEngNeoForgeServer(IEventBus modEventBus, ModContainer container) {
        AppEngNeoForge.init(this, modEventBus, container);
    }
}
