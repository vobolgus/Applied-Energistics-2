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

package appeng.server.testplots;

import java.util.List;

import net.minecraft.core.GlobalPos;

import appeng.api.config.Actionable;
import appeng.api.features.GridLinkables;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.core.definitions.AEItems;

/**
 * The Fabric twin of the NeoForge overlay class of the same name; registered as a listener in {@link TestPlotEvents}.
 */
final class SpawnTestTools {
    private SpawnTestTools() {
    }

    static void spawnWirelessTerminals(TestPlotEvents.SpawnExtraGridTestTools e) {
        // Find a suitable WAP to link to
        var waps = e.getGrid().getMachines(WirelessAccessPointBlockEntity.class);
        if (waps.isEmpty()) {
            return;
        }

        var wap = waps.iterator().next();
        var inventory = e.getInventory();

        for (var item : List.of(AEItems.WIRELESS_CRAFTING_TERMINAL, AEItems.WIRELESS_TERMINAL)) {
            var terminal = item.stack();
            // Fully charge it
            item.get().injectAEPower(terminal, Double.MAX_VALUE, Actionable.MODULATE);
            // Link it to the WAP we just placed
            GridLinkables.get(item).link(terminal, GlobalPos.of(wap.getLevel().dimension(), wap.getBlockPos()));
            inventory.addItems(terminal);
        }
    }
}
