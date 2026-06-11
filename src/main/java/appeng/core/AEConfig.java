/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

import appeng.api.config.CondenserOutput;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.networking.pathing.ChannelMode;
import appeng.core.config.ConfigStore;
import appeng.core.settings.TickRates;
import appeng.util.EnumCycler;
import appeng.util.Platform;

public final class AEConfig {

    private final ClientConfig client;
    private final CommonConfig common;

    // Default Energy Conversion Rates
    private static final double DEFAULT_FE_EXCHANGE = 0.5;

    private static AEConfig instance;

    private AEConfig(ConfigStore clientStore, ConfigStore commonStore) {
        this.client = new ClientConfig(clientStore);
        this.common = new CommonConfig(commonStore);
        commonStore.onLoadOrReload(common::sync);
    }

    /**
     * Builds the config structure into the given loader-provided stores. The caller is responsible for registering the
     * stores with the loader's config machinery afterwards.
     */
    public static void register(ConfigStore clientStore, ConfigStore commonStore) {
        instance = new AEConfig(clientStore, commonStore);
    }

    public static AEConfig instance() {
        return instance;
    }

    // Tunnels
    public double getP2PTunnelEnergyTax() {
        return common.p2pTunnelEnergyTax.get();
    }

    public double getP2PTunnelTransportTax() {
        return common.p2pTunnelTransportTax.get();
    }

    public double wireless_getDrainRate(double range) {
        return common.wirelessTerminalDrainMultiplier.get() * range;
    }

    public double wireless_getMaxRange(int boosters) {
        return common.wirelessBaseRange.get()
                + common.wirelessBoosterRangeMultiplier.get() * Math.pow(boosters, common.wirelessBoosterExp.get());
    }

    public double wireless_getPowerDrain(int boosters) {
        return common.wirelessBaseCost.get()
                + common.wirelessCostMultiplier.get()
                        * Math.pow(boosters, 1 + boosters / common.wirelessHighWirelessCount.get());
    }

    public boolean isSearchModNameInTooltips() {
        return client.searchModNameInTooltips.get();
    }

    public void setSearchModNameInTooltips(boolean enable) {
        if (enable != client.searchModNameInTooltips.get()) {
            client.searchModNameInTooltips.set(enable);
            client.store.save();
        }
    }

    public boolean isUseExternalSearch() {
        return client.useExternalSearch.get();
    }

    public void setUseExternalSearch(boolean enable) {
        if (enable != client.useExternalSearch.get()) {
            client.useExternalSearch.set(enable);
            client.store.save();
        }
    }

    public boolean isClearExternalSearchOnOpen() {
        return client.clearExternalSearchOnOpen.get();
    }

    public void setClearExternalSearchOnOpen(boolean enable) {
        if (enable != client.clearExternalSearchOnOpen.get()) {
            client.clearExternalSearchOnOpen.set(enable);
            client.store.save();
        }
    }

    public boolean isRememberLastSearch() {
        return client.rememberLastSearch.get();
    }

    public void setRememberLastSearch(boolean enable) {
        if (enable != client.rememberLastSearch.get()) {
            client.rememberLastSearch.set(enable);
            client.store.save();
        }
    }

    public boolean isAutoFocusSearch() {
        return client.autoFocusSearch.get();
    }

    public void setAutoFocusSearch(boolean enable) {
        if (enable != client.autoFocusSearch.get()) {
            client.autoFocusSearch.set(enable);
            client.store.save();
        }
    }

    public boolean isSyncWithExternalSearch() {
        return client.syncWithExternalSearch.get();
    }

    public void setSyncWithExternalSearch(boolean enable) {
        if (enable != client.syncWithExternalSearch.get()) {
            client.syncWithExternalSearch.set(enable);
            client.store.save();
        }
    }

    public TerminalStyle getTerminalStyle() {
        return client.terminalStyle.get();
    }

    public void setTerminalStyle(TerminalStyle setting) {
        if (setting != client.terminalStyle.get()) {
            client.terminalStyle.set(setting);
            client.store.save();
        }
    }

    public double getGridEnergyStoragePerNode() {
        return common.gridEnergyStoragePerNode.get();
    }

    public double getCrystalResonanceGeneratorRate() {
        return common.crystalResonanceGeneratorRate.get();
    }

    public PowerUnit getSelectedEnergyUnit() {
        return this.client.selectedPowerUnit.get();
    }

    public void nextEnergyUnit(boolean backwards) {
        var selected = EnumCycler.rotateEnum(getSelectedEnergyUnit(), backwards,
                Settings.POWER_UNITS.getValues());
        client.selectedPowerUnit.set(selected);
        client.store.save();
    }

    // Getters
    public boolean isDebugToolsEnabled() {
        return common.debugTools.get();
    }

    public int getFormationPlaneEntityLimit() {
        return common.formationPlaneEntityLimit.get();
    }

    public boolean isEnableEffects() {
        return client.enableEffects.get();
    }

    public boolean isUseLargeFonts() {
        return client.useLargeFonts.get();
    }

    public boolean isUseColoredCraftingStatus() {
        return client.useColoredCraftingStatus.get();
    }

    public boolean isDisableColoredCableRecipesInRecipeViewer() {
        return client.disableColoredCableRecipesInRecipeViewer.get();
    }

    public boolean isEnableFacadesInRecipeViewer() {
        return client.enableFacadesInRecipeViewer.get();
    }

    public boolean isEnableFacadeRecipesInRecipeViewer() {
        return client.enableFacadeRecipesInRecipeViewer.get();
    }

    public boolean isExposeNetworkInventoryToEmi() {
        return client.exposeNetworkInventoryToEmi.get();
    }

    public int getCraftingCalculationTimePerTick() {
        return common.craftingCalculationTimePerTick.get();
    }

    public boolean isSpatialAnchorEnablesRandomTicks() {
        return common.spatialAnchorEnableRandomTicks.get();
    }

    public double getSpatialPowerExponent() {
        return common.spatialPowerExponent.get();
    }

    public double getSpatialPowerMultiplier() {
        return common.spatialPowerMultiplier.get();
    }

    public double getChargerChargeRate() {
        return common.chargerChargeRate.get();
    }

    public DoubleSupplier getWirelessTerminalBattery() {
        return common.wirelessTerminalBattery::get;
    }

    public DoubleSupplier getEntropyManipulatorBattery() {
        return common.entropyManipulatorBattery::get;
    }

    public DoubleSupplier getMatterCannonBattery() {
        return common.matterCannonBattery::get;
    }

    public DoubleSupplier getPortableCellBattery() {
        return common.portableCellBattery::get;
    }

    public DoubleSupplier getColorApplicatorBattery() {
        return common.colorApplicatorBattery::get;
    }

    public DoubleSupplier getChargedStaffBattery() {
        return common.chargedStaffBattery::get;
    }

    public boolean isShowDebugGuiOverlays() {
        return client.debugGuiOverlays.get();
    }

    public void setShowDebugGuiOverlays(boolean enable) {
        if (enable != client.debugGuiOverlays.get()) {
            client.debugGuiOverlays.set(enable);
            client.store.save();
        }
    }

    public boolean isSpawnPressesInMeteoritesEnabled() {
        return common.spawnPressesInMeteorites.get();
    }

    public boolean isSpawnFlawlessOnlyEnabled() {
        return common.spawnFlawlessOnly.get();
    }

    public boolean isMatterCanonBlockDamageEnabled() {
        return common.matterCannonBlockDamage.get();
    }

    public boolean isTinyTntBlockDamageEnabled() {
        return common.tinyTntBlockDamage.get();
    }

    public int getGrowthAcceleratorSpeed() {
        return common.growthAcceleratorSpeed.get();
    }

    public boolean isAnnihilationPlaneSkyDustGenerationEnabled() {
        return common.annihilationPlaneSkyDustGeneration.get();
    }

    public boolean isBlockUpdateLogEnabled() {
        return common.blockUpdateLog.get();
    }

    public boolean isChunkLoggerTraceEnabled() {
        return common.chunkLoggerTrace.get();
    }

    public ChannelMode getChannelMode() {
        return common.channels.get();
    }

    public void setChannelModel(ChannelMode mode) {
        if (mode != common.channels.get()) {
            common.channels.set(mode);
            client.store.save();
        }
    }

    /**
     * @return True if an in-world preview of parts and facade placement should be shown when holding one in hand.
     */
    public boolean isPlacementPreviewEnabled() {
        return client.showPlacementPreview.get();
    }

    // Tooltip settings

    /**
     * Show upgrade inventory in tooltips of storage cells and similar devices.
     */
    public boolean isTooltipShowCellUpgrades() {
        return client.tooltipShowCellUpgrades.get();
    }

    /**
     * Show part of the content in tooltips of storage cells and similar devices.
     */
    public boolean isTooltipShowCellContent() {
        return client.tooltipShowCellContent.get();
    }

    /**
     * How much of the content to show in storage cellls and similar devices.
     */
    public int getTooltipMaxCellContentShown() {
        return client.tooltipMaxCellContentShown.get();
    }

    public boolean isPinAutoCraftedItems() {
        return client.pinAutoCraftedItems.get();
    }

    public void setPinAutoCraftedItems(boolean enabled) {
        if (enabled != client.pinAutoCraftedItems.get()) {
            client.pinAutoCraftedItems.set(enabled);
            client.store.save();
        }
    }

    public boolean isNotifyForFinishedCraftingJobs() {
        return client.notifyForFinishedCraftingJobs.get();
    }

    public void setNotifyForFinishedCraftingJobs(boolean enabled) {
        if (enabled != client.notifyForFinishedCraftingJobs.get()) {
            client.notifyForFinishedCraftingJobs.set(enabled);
            client.store.save();
        }
    }

    public boolean isClearGridOnClose() {
        return client.clearGridOnClose.get();
    }

    public void setClearGridOnClose(boolean enabled) {
        if (enabled != client.clearGridOnClose.get()) {
            client.clearGridOnClose.set(enabled);
            client.store.save();
        }
    }

    public double getVibrationChamberBaseEnergyPerFuelTick() {
        return common.vibrationChamberBaseEnergyPerFuelTick.get();
    }

    public int getVibrationChamberMinEnergyPerGameTick() {
        return common.vibrationChamberMinEnergyPerTick.get();
    }

    public int getVibrationChamberMaxEnergyPerGameTick() {
        return common.vibrationChamberMaxEnergyPerTick.get();
    }

    public int getTerminalMargin() {
        return client.terminalMargin.get();
    }

    public void save() {
        common.store.save();
        client.store.save();
    }

    private static class ClientConfig {
        private final ConfigStore store;

        // Misc
        public final ConfigStore.Value<Boolean> enableEffects;
        public final ConfigStore.Value<Boolean> useLargeFonts;
        public final ConfigStore.Value<Boolean> useColoredCraftingStatus;
        public final ConfigStore.Value<Boolean> disableColoredCableRecipesInRecipeViewer;
        public final ConfigStore.Value<Boolean> enableFacadesInRecipeViewer;
        public final ConfigStore.Value<Boolean> enableFacadeRecipesInRecipeViewer;
        public final ConfigStore.Value<Boolean> exposeNetworkInventoryToEmi;
        public final ConfigStore.Value<PowerUnit> selectedPowerUnit;
        public final ConfigStore.Value<Boolean> debugGuiOverlays;
        public final ConfigStore.Value<Boolean> showPlacementPreview;
        public final ConfigStore.Value<Boolean> notifyForFinishedCraftingJobs;

        // Terminal Settings
        public final ConfigStore.Value<TerminalStyle> terminalStyle;
        public final ConfigStore.Value<Boolean> pinAutoCraftedItems;
        public final ConfigStore.Value<Boolean> clearGridOnClose;
        public final ConfigStore.Value<Integer> terminalMargin;

        // Search Settings
        public final ConfigStore.Value<Boolean> searchModNameInTooltips;
        public final ConfigStore.Value<Boolean> useExternalSearch;
        public final ConfigStore.Value<Boolean> clearExternalSearchOnOpen;
        public final ConfigStore.Value<Boolean> syncWithExternalSearch;
        public final ConfigStore.Value<Boolean> rememberLastSearch;
        public final ConfigStore.Value<Boolean> autoFocusSearch;

        // Tooltip settings
        public final ConfigStore.Value<Boolean> tooltipShowCellUpgrades;
        public final ConfigStore.Value<Boolean> tooltipShowCellContent;
        public final ConfigStore.Value<Integer> tooltipMaxCellContentShown;

        public ClientConfig(ConfigStore store) {
            this.store = store;

            store.push("recipeViewers");
            this.disableColoredCableRecipesInRecipeViewer = define(store, "disableColoredCableRecipesInRecipeViewer",
                    true);
            this.enableFacadesInRecipeViewer = define(store, "enableFacadesInRecipeViewer", false,
                    "Show facades in REI/JEI/EMI item list");
            this.enableFacadeRecipesInRecipeViewer = define(store, "enableFacadeRecipesInRecipeViewer", true,
                    "Show facade recipes in REI/JEI/EMI for supported blocks");
            this.exposeNetworkInventoryToEmi = define(store, "provideNetworkInventoryToEmi", false,
                    "Expose the full network inventory to EMI, which might cause performance problems.");
            store.pop();

            store.push("client");
            this.enableEffects = define(store, "enableEffects", true);
            this.useLargeFonts = define(store, "useTerminalUseLargeFont", false);
            this.useColoredCraftingStatus = define(store, "useColoredCraftingStatus", true);
            this.selectedPowerUnit = defineEnum(store, "powerUnit", PowerUnit.AE, "Unit of power shown in AE UIs");
            this.debugGuiOverlays = define(store, "showDebugGuiOverlays", false, "Show debugging GUI overlays");
            this.showPlacementPreview = define(store, "showPlacementPreview", true,
                    "Show a preview of part and facade placement");
            this.notifyForFinishedCraftingJobs = define(store, "notifyForFinishedCraftingJobs", true,
                    "Show toast when long-running crafting jobs finish.");
            store.pop();

            store.push("terminals");
            this.terminalStyle = defineEnum(store, "terminalStyle", TerminalStyle.SMALL);
            this.pinAutoCraftedItems = define(store, "pinAutoCraftedItems", true,
                    "Pin items that the player auto-crafts to the top of the terminal");
            this.clearGridOnClose = define(store, "clearGridOnClose", false,
                    "Automatically clear the crafting/encoding grid when closing the terminal");
            this.terminalMargin = define(store, "terminalMargin", 25,
                    "The vertical margin to apply when sizing terminals. Used to make room for centered item mod search bars");
            store.pop();

            // Search Settings
            store.push("search");
            this.searchModNameInTooltips = define(store, "searchModNameInTooltips", false,
                    "Should the mod name be included when searching in tooltips.");
            this.useExternalSearch = define(store, "useExternalSearch", false,
                    "Replaces AEs own search with the search of REI or JEI");
            this.clearExternalSearchOnOpen = define(store, "clearExternalSearchOnOpen", true,
                    "When using useExternalSearch, clears the search when the terminal opens");
            this.syncWithExternalSearch = define(store, "syncWithExternalSearch", true,
                    "When REI/JEI is installed, automatically set the AE or REI/JEI search text when either is changed while the terminal is open");
            this.rememberLastSearch = define(store, "rememberLastSearch", true,
                    "Remembers the last search term and restores it when the terminal opens");
            this.autoFocusSearch = define(store, "autoFocusSearch", false,
                    "Automatically focuses the search field when the terminal opens");
            store.pop();

            store.push("tooltips");
            this.tooltipShowCellUpgrades = define(store, "showCellUpgrades", true,
                    "Show installed upgrades in the tooltips of storage cells, color applicators and matter cannons");
            this.tooltipShowCellContent = define(store, "showCellContent", true,
                    "Show a preview of the content in the tooltips of storage cells, color applicators and matter cannons");
            this.tooltipMaxCellContentShown = define(store, "maxCellContentShown", 5, 1, 32,
                    "The maximum number of content entries to show in the tooltip of storage cells, color applicators and matter cannons");
            store.pop();
        }

    }

    private static class CommonConfig {
        private final ConfigStore store;

        // Misc
        public final ConfigStore.Value<Integer> formationPlaneEntityLimit;
        public final ConfigStore.Value<Integer> craftingCalculationTimePerTick;
        public final ConfigStore.Value<Boolean> debugTools;
        public final ConfigStore.Value<Boolean> matterCannonBlockDamage;
        public final ConfigStore.Value<Boolean> tinyTntBlockDamage;
        public final ConfigStore.Value<ChannelMode> channels;
        public final ConfigStore.Value<Boolean> spatialAnchorEnableRandomTicks;

        public final ConfigStore.Value<Integer> growthAcceleratorSpeed;
        public final ConfigStore.Value<Boolean> annihilationPlaneSkyDustGeneration;

        // Spatial IO/Dimension
        public final ConfigStore.Value<Double> spatialPowerExponent;
        public final ConfigStore.Value<Double> spatialPowerMultiplier;

        // Logging
        public final ConfigStore.Value<Boolean> blockUpdateLog;
        public final ConfigStore.Value<Boolean> craftingLog;
        public final ConfigStore.Value<Boolean> debugLog;
        public final ConfigStore.Value<Boolean> gridLog;
        public final ConfigStore.Value<Boolean> chunkLoggerTrace;

        // Batteries
        public final ConfigStore.Value<Double> chargerChargeRate;
        public final ConfigStore.Value<Integer> wirelessTerminalBattery;
        public final ConfigStore.Value<Integer> entropyManipulatorBattery;
        public final ConfigStore.Value<Integer> matterCannonBattery;
        public final ConfigStore.Value<Integer> portableCellBattery;
        public final ConfigStore.Value<Integer> colorApplicatorBattery;
        public final ConfigStore.Value<Integer> chargedStaffBattery;

        // Meteors
        public final ConfigStore.Value<Boolean> spawnPressesInMeteorites;
        public final ConfigStore.Value<Boolean> spawnFlawlessOnly;

        // Wireless
        public final ConfigStore.Value<Double> wirelessBaseCost;
        public final ConfigStore.Value<Double> wirelessCostMultiplier;
        public final ConfigStore.Value<Double> wirelessTerminalDrainMultiplier;
        public final ConfigStore.Value<Double> wirelessBaseRange;
        public final ConfigStore.Value<Double> wirelessBoosterRangeMultiplier;
        public final ConfigStore.Value<Double> wirelessBoosterExp;
        public final ConfigStore.Value<Double> wirelessHighWirelessCount;

        // Power Ratios
        public final ConfigStore.Value<Double> powerRatioForgeEnergy;
        public final ConfigStore.Value<Double> powerUsageMultiplier;
        public final ConfigStore.Value<Double> gridEnergyStoragePerNode;
        public final ConfigStore.Value<Double> crystalResonanceGeneratorRate;
        public final ConfigStore.Value<Double> p2pTunnelEnergyTax;
        public final ConfigStore.Value<Double> p2pTunnelTransportTax;

        // Vibration Chamber
        public final ConfigStore.Value<Double> vibrationChamberBaseEnergyPerFuelTick;
        public final ConfigStore.Value<Integer> vibrationChamberMinEnergyPerTick;
        public final ConfigStore.Value<Integer> vibrationChamberMaxEnergyPerTick;

        // Condenser Power Requirement
        public final ConfigStore.Value<Integer> condenserMatterBallsPower;
        public final ConfigStore.Value<Integer> condenserSingularityPower;

        public final Map<TickRates, ConfigStore.Value<Integer>> tickRateMin = new HashMap<>();
        public final Map<TickRates, ConfigStore.Value<Integer>> tickRateMax = new HashMap<>();

        public CommonConfig(ConfigStore store) {
            this.store = store;

            store.push("general");
            debugTools = define(store, "unsupportedDeveloperTools", Platform.isDevelopmentEnvironment());
            matterCannonBlockDamage = define(store, "matterCannonBlockDamage", true,
                    "Enables the ability of the Matter Cannon to break blocks.");
            tinyTntBlockDamage = define(store, "tinyTntBlockDamage", true,
                    "Enables the ability of Tiny TNT to break blocks.");
            channels = defineEnum(store, "channels", ChannelMode.DEFAULT,
                    "Changes the channel capacity that cables provide in AE2.");
            spatialAnchorEnableRandomTicks = define(store, "spatialAnchorEnableRandomTicks", true,
                    "Whether Spatial Anchors should force random chunk ticks and entity spawning.");
            store.pop();

            store.push("automation");
            formationPlaneEntityLimit = define(store, "formationPlaneEntityLimit", 128);
            store.pop();

            store.push("craftingCPU");
            this.craftingCalculationTimePerTick = define(store, "craftingCalculationTimePerTick", 5);
            store.pop();

            store.push("crafting");
            growthAcceleratorSpeed = define(store, "growthAccelerator", 10, 1, 100,
                    "Number of ticks between two crystal growth accelerator ticks");
            annihilationPlaneSkyDustGeneration = define(store, "annihilationPlaneSkyDustGeneration", true,
                    "If enabled, an annihilation placed face up at the maximum world height will generate sky stone passively.");
            store.pop();

            store.push("spatialio");
            this.spatialPowerMultiplier = define(store, "spatialPowerMultiplier", 1250.0);
            this.spatialPowerExponent = define(store, "spatialPowerExponent", 1.35);
            store.pop();

            store.push("logging");
            blockUpdateLog = define(store, "blockUpdateLog", false);
            craftingLog = define(store, "craftingLog", false);
            debugLog = define(store, "debugLog", false);
            gridLog = define(store, "gridLog", false);
            chunkLoggerTrace = define(store, "chunkLoggerTrace", false,
                    "Enable stack trace logging for the chunk loading debug command");
            store.pop();

            store.push("battery");
            this.chargerChargeRate = define(store, "chargerChargeRate", 1.0,
                    0.1, 10.0,
                    "The chargers charging rate factor, which is applied to the charged items charge rate. 2 means it charges everything twice as fast. 0.5 half as fast.");
            this.wirelessTerminalBattery = define(store, "wirelessTerminal", 1600000);
            this.chargedStaffBattery = define(store, "chargedStaff", 8000);
            this.entropyManipulatorBattery = define(store, "entropyManipulator", 200000);
            this.portableCellBattery = define(store, "portableCell", 20000);
            this.colorApplicatorBattery = define(store, "colorApplicator", 20000);
            this.matterCannonBattery = define(store, "matterCannon", 200000);
            store.pop();

            store.push("worldGen");
            this.spawnPressesInMeteorites = define(store, "spawnPressesInMeteorites", true);
            this.spawnFlawlessOnly = define(store, "spawnFlawlessOnly", false);
            store.pop();

            store.push("wireless");
            this.wirelessBaseCost = define(store, "wirelessBaseCost", 8.0);
            this.wirelessCostMultiplier = define(store, "wirelessCostMultiplier", 1.0);
            this.wirelessBaseRange = define(store, "wirelessBaseRange", 16.0);
            this.wirelessBoosterRangeMultiplier = define(store, "wirelessBoosterRangeMultiplier", 1.0);
            this.wirelessBoosterExp = define(store, "wirelessBoosterExp", 1.5);
            this.wirelessHighWirelessCount = define(store, "wirelessHighWirelessCount", 64.0);
            this.wirelessTerminalDrainMultiplier = define(store, "wirelessTerminalDrainMultiplier", 1.0);
            store.pop();

            store.push("powerRatios");
            powerRatioForgeEnergy = define(store, "forgeEnergy", DEFAULT_FE_EXCHANGE);
            powerUsageMultiplier = define(store, "usageMultiplier", 1.0, 0.01, Double.MAX_VALUE);
            gridEnergyStoragePerNode = define(store, "gridEnergyStoragePerNode", 25.0, 1.0, 1000000.0,
                    "How much energy can the internal grid buffer storage per node attached to the grid.");
            crystalResonanceGeneratorRate = define(store, "crystalResonanceGeneratorRate", 20.0, 0.0, 1000000.0,
                    "How much energy a crystal resonance generator generates per tick.");
            p2pTunnelEnergyTax = define(store, "p2pTunnelEnergyTax", 0.025, 0.0, 1.0,
                    "The cost to transport energy through an energy P2P tunnel expressed as a factor of the transported energy.");
            p2pTunnelTransportTax = define(store, "p2pTunnelTransportTax", 0.025, 0.0, 1.0,
                    "The cost to transport items/fluids/etc. through P2P tunnels, expressed in AE energy per equivalent I/O bus operation for the transported object type (i.e. items=per 1 item, fluids=per 125mb).");
            store.pop();

            store.push("condenser");
            condenserMatterBallsPower = define(store, "matterBalls", 256);
            condenserSingularityPower = define(store, "singularity", 256000);
            store.pop();

            store.comment(
                    " Min / Max Tickrates for dynamic ticking, most of these components also use sleeping, to prevent constant ticking, adjust with care, non standard rates are not supported or tested.");
            store.push("tickRates");
            for (TickRates tickRate : TickRates.values()) {
                tickRateMin.put(tickRate, define(store, tickRate.name() + "Min", tickRate.getDefaultMin()));
                tickRateMax.put(tickRate, define(store, tickRate.name() + "Max", tickRate.getDefaultMax()));
            }
            store.pop();

            store.comment("Settings for the Vibration Chamber");
            store.push("vibrationChamber");
            vibrationChamberBaseEnergyPerFuelTick = define(store, "baseEnergyPerFuelTick", 5.0, 0.1, 1000.0,
                    "AE energy produced per fuel burn tick (reminder: coal = 1600, block of coal = 16000, lava bucket = 20000 burn ticks)");
            vibrationChamberMinEnergyPerTick = define(store, "minEnergyPerGameTick", 4, 0, 1000,
                    "Minimum amount of AE/t the vibration chamber can slow down to when energy is being wasted.");
            vibrationChamberMaxEnergyPerTick = define(store, "baseMaxEnergyPerGameTick", 40, 1, 1000,
                    "Maximum amount of AE/t the vibration chamber can speed up to when generated energy is being fully consumed.");
            store.pop();
        }

        public void sync() {
            PowerUnit.FE.conversionRatio = powerRatioForgeEnergy.get();
            PowerMultiplier.CONFIG.multiplier = powerUsageMultiplier.get();

            CondenserOutput.MATTER_BALLS.requiredPower = condenserMatterBallsPower.get();
            CondenserOutput.SINGULARITY.requiredPower = condenserSingularityPower.get();

            for (TickRates tr : TickRates.values()) {
                tr.setMin(tickRateMin.get(tr).get());
                tr.setMax(tickRateMax.get(tr).get());
            }

            AELog.setCraftingLogEnabled(craftingLog.get());
            AELog.setDebugLogEnabled(debugLog.get());
            AELog.setGridLogEnabled(gridLog.get());
        }
    }

    private static ConfigStore.Value<Boolean> define(ConfigStore store, String name, boolean defaultValue,
            String comment) {
        store.comment(comment);
        return define(store, name, defaultValue);
    }

    private static ConfigStore.Value<Boolean> define(ConfigStore store, String name, boolean defaultValue) {
        return store.defineBoolean(name, defaultValue);
    }

    private static ConfigStore.Value<Integer> define(ConfigStore store, String name, int defaultValue,
            String comment) {
        store.comment(comment);
        return define(store, name, defaultValue);
    }

    private static ConfigStore.Value<Double> define(ConfigStore store, String name, double defaultValue) {
        return define(store, name, defaultValue, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    private static ConfigStore.Value<Double> define(ConfigStore store, String name, double defaultValue,
            String comment) {
        store.comment(comment);
        return define(store, name, defaultValue);
    }

    private static ConfigStore.Value<Double> define(ConfigStore store, String name, double defaultValue, double min,
            double max, String comment) {
        store.comment(comment);
        return define(store, name, defaultValue, min, max);
    }

    private static ConfigStore.Value<Double> define(ConfigStore store, String name, double defaultValue, double min,
            double max) {
        return store.defineDouble(name, defaultValue, min, max);
    }

    private static ConfigStore.Value<Integer> define(ConfigStore store, String name, int defaultValue, int min,
            int max, String comment) {
        store.comment(comment);
        return define(store, name, defaultValue, min, max);
    }

    private static ConfigStore.Value<Integer> define(ConfigStore store, String name, int defaultValue, int min,
            int max) {
        return store.defineInt(name, defaultValue, min, max);
    }

    private static ConfigStore.Value<Integer> define(ConfigStore store, String name, int defaultValue) {
        return define(store, name, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private static <T extends Enum<T>> ConfigStore.Value<T> defineEnum(ConfigStore store, String name,
            T defaultValue) {
        return store.defineEnum(name, defaultValue);
    }

    private static <T extends Enum<T>> ConfigStore.Value<T> defineEnum(ConfigStore store, String name,
            T defaultValue, String comment) {
        store.comment(comment);
        return defineEnum(store, name, defaultValue);
    }

}
