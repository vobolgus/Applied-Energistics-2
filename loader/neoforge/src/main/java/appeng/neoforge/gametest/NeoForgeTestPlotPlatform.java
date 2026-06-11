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

package appeng.neoforge.gametest;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.server.testplots.KitOutPlayerEvent;
import appeng.server.testplots.SpawnExtraGridTestTools;
import appeng.server.testplots.TestPlotClass;
import appeng.server.testplots.TestPlotPlatform;

/**
 * {@link TestPlotPlatform} based on NeoForge's annotation scan data and the NeoForge event bus.
 */
public class NeoForgeTestPlotPlatform implements TestPlotPlatform {
    private static final Logger LOG = LoggerFactory.getLogger(NeoForgeTestPlotPlatform.class);

    @Override
    public List<Class<?>> findTestPlotClasses() {
        var result = new ArrayList<Class<?>>();

        for (var data : ModList.get().getAllScanData()) {
            for (var annotation : data.getAnnotations()) {
                if (annotation.targetType() == ElementType.TYPE
                        && annotation.annotationType().getClassName().equals(TestPlotClass.class.getName())) {
                    try {
                        result.add(Class.forName(annotation.memberName()));
                    } catch (Throwable e) {
                        LOG.error("Failed to load class {} annotated with @TestPlotClass", annotation.memberName(), e);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void postKitOutPlayer(ServerPlayer player) {
        NeoForge.EVENT_BUS.post(new KitOutPlayerEvent(player));
    }

    @Override
    public void postSpawnExtraGridTestTools(Identifier plotId, InternalInventory inventory, IGrid grid) {
        NeoForge.EVENT_BUS.post(new SpawnExtraGridTestTools(plotId, inventory, grid));
    }
}
