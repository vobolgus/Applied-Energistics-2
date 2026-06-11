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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.neoforged.neoforge.registries.callback.BakeCallback;

import appeng.api.lookup.AEApiLookups;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypesInternal;
import appeng.core.AppEng;
import appeng.core.AppEngBase;
import appeng.core.LoaderEventHooks;
import appeng.core.MainCreativeTab;
import appeng.core.PlayerCtrlAttachment;
import appeng.core.network.NetworkAdapter;
import appeng.core.particles.InitParticleTypes;
import appeng.debug.EnergyGeneratorPlatform;
import appeng.hooks.SkyStoneBreakSpeed;
import appeng.hooks.WrenchHook;
import appeng.hooks.ticking.TickHandler;
import appeng.hotkeys.HotkeyActions;
import appeng.init.InitAdvancementTriggers;
import appeng.init.InitCapabilityProviders;
import appeng.init.InitMenuTypes;
import appeng.init.InitStats;
import appeng.init.InitVillager;
import appeng.integration.Integrations;
import appeng.integration.modules.curios.CuriosSupport;
import appeng.me.cluster.implementations.QuantumCluster;
import appeng.menu.implementations.MenuTypePlatform;
import appeng.neoforge.config.NeoForgeConfigStore;
import appeng.neoforge.debug.NeoForgeEnergyGenerator;
import appeng.neoforge.fluids.NeoForgeFluidPlatform;
import appeng.neoforge.gametest.AENeoForgeGameTests;
import appeng.neoforge.gametest.NeoForgeTestPlotPlatform;
import appeng.neoforge.integration.NeoForgeCuriosSupport;
import appeng.neoforge.lookup.NeoForgeApiLookups;
import appeng.neoforge.menu.NeoForgeMenuTypePlatform;
import appeng.neoforge.network.NeoForgeNetworkAdapter;
import appeng.neoforge.network.NeoForgeNetworkInit;
import appeng.neoforge.registration.NeoForgeRegistrar;
import appeng.neoforge.service.NeoForgeChunkLoadingService;
import appeng.server.services.ChunkLoadingService;
import appeng.server.subcommands.ChunkLogger;
import appeng.server.testplots.TestPlotPlatform;
import appeng.server.testworld.GameTestPlotAdapter;
import appeng.spatial.SpatialStorageChunkGenerator;
import appeng.spatial.SpatialStorageDimensionIds;
import appeng.util.LoaderPlatform;
import appeng.util.fluid.FluidPlatform;

/**
 * The NeoForge entrypoint logic shared by both dists: injects the loader-specific implementations into the static
 * seams, drives the {@link AppEngBase} lifecycle from NeoForge's mod- and game-bus events, and registers the
 * NeoForge-specific registration glue.
 * <p>
 * It is invoked from the {@code @Mod} constructors: {@link AppEngNeoForgeServer} on the dedicated server, and
 * {@code appeng.neoforge.client.AppEngNeoForgeClient} on the client.
 */
public final class AppEngNeoForge {

    private static final Logger LOG = LoggerFactory.getLogger(AppEngNeoForge.class);

    private AppEngNeoForge() {
    }

    public static void init(AppEngBase base, IEventBus modEventBus, ModContainer container) {
        // Inject the loader-specific seam implementations before any registration content is class-loaded.
        AEApiLookups.init(new NeoForgeApiLookups());
        LoaderPlatform.init(new NeoForgeLoaderPlatform());
        LoaderEventHooks.init(new NeoForgeEventHooks());
        MenuTypePlatform.init(new NeoForgeMenuTypePlatform());

        NeoForgeConfigStore.initConfigs(container);

        base.registerContent();

        PlayerCtrlAttachment.init(new NeoForgePlayerCtrlAttachment(modEventBus));
        NeoForgeRegistrar.register(modEventBus);
        NetworkAdapter.init(new NeoForgeNetworkAdapter());
        FluidPlatform.init(new NeoForgeFluidPlatform());
        EnergyGeneratorPlatform.init(new NeoForgeEnergyGenerator());
        CuriosSupport.init(new NeoForgeCuriosSupport());
        ChunkLoadingService.init(new NeoForgeChunkLoadingService(modEventBus));
        TestPlotPlatform.init(new NeoForgeTestPlotPlatform());

        modEventBus.addListener(AppEngNeoForge::registerRegistries);
        modEventBus.addListener((BuildCreativeModeTabContentsEvent event) -> MainCreativeTab
                .initExternal(event.getTabKey(), event::accept));
        modEventBus.addListener(NeoForgeNetworkInit::init);
        modEventBus.addListener(EventPriority.HIGH, InitCapabilityProviders::markProxyableCapabilities);
        modEventBus.addListener(InitCapabilityProviders::register);
        modEventBus.addListener(EventPriority.LOWEST, InitCapabilityProviders::registerGenericAdapters);
        modEventBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey() == Registries.SOUND_EVENT) {
                base.registerSounds(BuiltInRegistries.SOUND_EVENT);
            } else if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
                base.registerCreativeTabs(BuiltInRegistries.CREATIVE_MODE_TAB);
            } else if (event.getRegistryKey() == Registries.CUSTOM_STAT) {
                InitStats.init(event.getRegistry(Registries.CUSTOM_STAT));
            } else if (event.getRegistryKey() == Registries.TRIGGER_TYPE) {
                InitAdvancementTriggers.init(event.getRegistry(Registries.TRIGGER_TYPE));
            } else if (event.getRegistryKey() == Registries.PARTICLE_TYPE) {
                InitParticleTypes.init(event.getRegistry(Registries.PARTICLE_TYPE));
            } else if (event.getRegistryKey() == Registries.MENU) {
                InitMenuTypes.init(event.getRegistry(Registries.MENU));
            } else if (event.getRegistryKey() == Registries.CHUNK_GENERATOR) {
                Registry.register(BuiltInRegistries.CHUNK_GENERATOR, SpatialStorageDimensionIds.CHUNK_GENERATOR_ID,
                        SpatialStorageChunkGenerator.CODEC);
            } else if (event.getRegistryKey() == Registries.VILLAGER_PROFESSION) {
                InitVillager.initProfession(event.getRegistry(Registries.VILLAGER_PROFESSION));
            } else if (event.getRegistryKey() == Registries.POINT_OF_INTEREST_TYPE) {
                InitVillager.initPointOfInterestType(event.getRegistry(Registries.POINT_OF_INTEREST_TYPE));
            } else if (event.getRegistryKey() == AEKeyType.REGISTRY_KEY) {
                base.registerKeyTypes(event.getRegistry(AEKeyType.REGISTRY_KEY));
            } else if (event.getRegistryKey() == Registries.TEST_INSTANCE_TYPE) {
                event.register(Registries.TEST_INSTANCE_TYPE, AppEng.makeId("plot_adapter"),
                        () -> GameTestPlotAdapter.CODEC);
            }
        });

        modEventBus.addListener(Integrations::enqueueIMC);
        modEventBus.addListener((FMLCommonSetupEvent event) -> commonSetup(base, event));

        AENeoForgeGameTests.init(modEventBus);

        // Game-bus wiring for the shared TickHandler (was TickHandler.init()).
        var tickHandler = TickHandler.instance();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre e) -> tickHandler.onServerTickStart());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> tickHandler.onServerTickEnd());
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                tickHandler.onServerLevelTickStart(level);
            }
        });
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> {
            if (e.getLevel() instanceof ServerLevel level) {
                tickHandler.onServerLevelTickEnd(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Unload e) -> tickHandler.onUnloadChunk(e.getLevel(), e.getChunk().getPos().pack()));
        // Quantum clusters must be destroyed before the TickHandler cleans up the level state below
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> QuantumCluster.onLevelUnload(e.getLevel()));
        // Try to go last for level unloads since we use it to clean-up state
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,
                (LevelEvent.Unload e) -> tickHandler.onUnloadLevel(e.getLevel()));

        // Forwarders for the chunk logger debug command (no-ops while the logger is disabled).
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> ChunkLogger.chunkLoaded(e.getLevel(), e.getChunk()));
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload e) -> ChunkLogger.chunkUnloaded(e.getLevel(), e.getChunk()));

        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> base.onServerStopped());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> base.registerCommands(e.getDispatcher()));

        NeoForge.EVENT_BUS.addListener(AppEngNeoForge::onPlayerUseBlockEvent);
        NeoForge.EVENT_BUS.addListener(AppEngNeoForge::handleSkyStoneBreakFaster);
        NeoForge.EVENT_BUS.addListener(
                (OnDatapackSyncEvent event) -> event.sendRecipes(base.getServerSyncedRecipeTypes()));

        HotkeyActions.init();
    }

    private static void commonSetup(AppEngBase base, FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            base.postRegistrationInitialization();
            AENeoForgeP2PAttunement.init();
        }).whenComplete((res, err) -> {
            if (err != null) {
                LOG.error("Common setup failed", err);
            }
        });
    }

    private static void registerRegistries(NewRegistryEvent e) {
        var registry = e.create(new RegistryBuilder<>(AEKeyType.REGISTRY_KEY)
                .sync(true)
                .maxId(127));
        AEKeyTypesInternal.setRegistry(registry);
        registry.addCallback((BakeCallback<AEKeyType>) ignored -> AEKeyTypesInternal.updateAllTypes());
    }

    private static void onPlayerUseBlockEvent(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            // See https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/6900
            return;
        }
        var result = WrenchHook.onPlayerUseBlock(event.getEntity(), event.getLevel(), event.getHand(),
                event.getHitVec());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private static void handleSkyStoneBreakFaster(PlayerEvent.BreakSpeed event) {
        var newSpeed = SkyStoneBreakSpeed.getIncreasedBreakSpeed(event.getEntity(), event.getState(),
                event.getNewSpeed());
        if (newSpeed != null) {
            event.setNewSpeed(newSpeed);
        }
    }
}
