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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.server.testplots.AnnihilationPlaneTests;
import appeng.server.testplots.AutoCraftingTestPlots;
import appeng.server.testplots.ChannelTests;
import appeng.server.testplots.CrystalResonanceGeneratorTestPlots;
import appeng.server.testplots.ExternalEnergyTestPlots;
import appeng.server.testplots.GuideRecipeSyncTestPlots;
import appeng.server.testplots.GuidebookPlot;
import appeng.server.testplots.InscriberTestPlots;
import appeng.server.testplots.InterfaceCapabilityTestPlots;
import appeng.server.testplots.InterfaceTestPlots;
import appeng.server.testplots.InvalidPatternTestPlot;
import appeng.server.testplots.ItemP2PTestPlots;
import appeng.server.testplots.MemoryCardTestPlots;
import appeng.server.testplots.P2PTestPlots;
import appeng.server.testplots.PatternProviderLockModePlots;
import appeng.server.testplots.PatternProviderPlots;
import appeng.server.testplots.QnbTestPlots;
import appeng.server.testplots.RecipeSerializerSyncTestPlots;
import appeng.server.testplots.SavedDataTestPlots;
import appeng.server.testplots.SkyStoneTestPlots;
import appeng.server.testplots.SpatialTestPlots;
import appeng.server.testplots.SubnetPlots;
import appeng.server.testplots.TestPlotClass;
import appeng.server.testplots.TestPlotEvents;
import appeng.server.testplots.TestPlotPlatform;
import appeng.server.testplots.TestPlots;
import appeng.server.testplots.TrinketsIntegrationTestPlots;

/**
 * Fabric implementation of the {@link TestPlotPlatform} seam.
 * <p>
 * <strong>Behavior note (vs. NeoForge):</strong> Fabric has no annotation scan data, so the {@link TestPlotClass}
 * classes cannot be discovered automatically. AE2's own plot classes are listed explicitly; other mods can contribute
 * theirs via {@link #addTestPlotClass}. {@code InterfaceCapabilityTestPlots} (interface_slot_filtering) is a
 * loader-specific plot with a Fabric twin under this source set (asserted via the Fabric transfer API instead of
 * NeoForge capabilities) and the NeoForge original under {@code loader/neoforge}.
 */
public class FabricTestPlotPlatform implements TestPlotPlatform {
    private static final List<Class<?>> PLOT_CLASSES = new CopyOnWriteArrayList<>(List.of(
            TestPlots.class,
            AnnihilationPlaneTests.class,
            AutoCraftingTestPlots.class,
            ChannelTests.class,
            CrystalResonanceGeneratorTestPlots.class,
            ExternalEnergyTestPlots.class,
            GuideRecipeSyncTestPlots.class,
            GuidebookPlot.class,
            InscriberTestPlots.class,
            InterfaceCapabilityTestPlots.class,
            InterfaceTestPlots.class,
            InvalidPatternTestPlot.class,
            ItemP2PTestPlots.class,
            MemoryCardTestPlots.class,
            P2PTestPlots.class,
            PatternProviderLockModePlots.class,
            PatternProviderPlots.class,
            QnbTestPlots.class,
            RecipeSerializerSyncTestPlots.class,
            SavedDataTestPlots.class,
            SkyStoneTestPlots.class,
            SpatialTestPlots.class,
            SubnetPlots.class,
            TrinketsIntegrationTestPlots.class));

    /**
     * Allows other mods (or the AE2 client/test sources) to contribute additional {@link TestPlotClass} classes, since
     * Fabric has no annotation scanning.
     */
    public static void addTestPlotClass(Class<?> clazz) {
        PLOT_CLASSES.add(clazz);
    }

    @Override
    public List<Class<?>> findTestPlotClasses() {
        return new ArrayList<>(PLOT_CLASSES);
    }

    @Override
    public void postKitOutPlayer(ServerPlayer player) {
        TestPlotEvents.fireKitOutPlayer(player);
    }

    @Override
    public void postSpawnExtraGridTestTools(Identifier plotId, InternalInventory inventory, IGrid grid) {
        TestPlotEvents.fireSpawnExtraGridTestTools(plotId, inventory, grid);
    }
}
