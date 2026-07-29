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

import org.jetbrains.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import appeng.fabric.integration.trinkets.TrinketsAccessorySupport;
import appeng.integration.modules.curios.CuriosSupport;

/**
 * Fabric implementation of the {@link CuriosSupport} seam. There is no Curios on Fabric; the equivalent accessory mod
 * is <em>Trinkets</em>, so the lookup is delegated to {@link TrinketsAccessorySupport} when it is installed and returns
 * null otherwise (which makes the shared consumers skip the accessory inventory entirely, as before).
 * <p>
 * The mod id checked is {@code trinkets_updated}, Patbox's actively maintained fork, and deliberately <em>not</em> the
 * {@code trinkets} alias it {@code provides}: the API package moved from {@code dev.emi.trinkets.api} to
 * {@code eu.pb4.trinkets.api} in that fork, so another mod providing the {@code trinkets} id would not satisfy the
 * classes {@link TrinketsAccessorySupport} compiles against. The delegate is only class-loaded behind this guard —
 * Trinkets is a {@code compileOnly} dependency and is absent from most runtimes.
 */
public class FabricCuriosSupport implements CuriosSupport {
    private static final String TRINKETS_MOD_ID = "trinkets_updated";

    private final boolean trinketsLoaded = FabricLoader.getInstance().isModLoaded(TRINKETS_MOD_ID);

    @Override
    @Nullable
    public Inventory getCuriosInventory(Player player) {
        if (!trinketsLoaded) {
            return null;
        }
        return TrinketsAccessorySupport.getInventory(player);
    }
}
