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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import appeng.api.lookup.AEApiLookups;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypesInternal;
import appeng.core.AppEng;
import appeng.core.AppEngBase;
import appeng.core.AppEngServer;
import appeng.core.LoaderEventHooks;
import appeng.core.MainCreativeTab;
import appeng.core.PlayerCtrlAttachment;
import appeng.core.network.NetworkAdapter;
import appeng.core.particles.InitParticleTypes;
import appeng.debug.EnergyGeneratorPlatform;
import appeng.fabric.config.FabricConfigStore;
import appeng.fabric.debug.FabricEnergyGenerator;
import appeng.fabric.fluids.FabricFluidPlatform;
import appeng.fabric.gametest.FabricTestPlotPlatform;
import appeng.fabric.init.InitApiLookup;
import appeng.fabric.lookup.FabricApiLookups;
import appeng.fabric.menu.FabricMenuTypePlatform;
import appeng.fabric.network.FabricNetworkAdapter;
import appeng.fabric.network.FabricNetworkInit;
import appeng.fabric.registration.FabricRegistrar;
import appeng.hooks.WrenchHook;
import appeng.hooks.ticking.TickHandler;
import appeng.hotkeys.HotkeyActions;
import appeng.init.InitAdvancementTriggers;
import appeng.init.InitMenuTypes;
import appeng.init.InitStats;
import appeng.init.InitVillager;
import appeng.integration.modules.curios.CuriosSupport;
import appeng.me.cluster.implementations.QuantumCluster;
import appeng.menu.implementations.MenuTypePlatform;
import appeng.server.services.ChunkLoadingService;
import appeng.server.services.FabricChunkLoadingService;
import appeng.server.subcommands.ChunkLogger;
import appeng.server.testplots.TestPlotPlatform;
import appeng.server.testworld.GameTestPlotAdapter;
import appeng.spatial.SpatialStorageChunkGenerator;
import appeng.spatial.SpatialStorageDimensionIds;
import appeng.util.LoaderPlatform;
import appeng.util.fluid.FluidPlatform;

/**
 * The Fabric entrypoint logic: injects the loader-specific implementations into the static seams, drives the
 * {@link AppEngBase} lifecycle, and registers the Fabric-specific registration glue. Mirrors
 * {@code appeng.neoforge.AppEngNeoForge}.
 * <p>
 * On the dedicated server, {@link #onInitialize()} constructs {@link AppEngServer} and runs the full initialization. On
 * the client, the client entrypoint constructs {@code AppEngClient} and calls {@link #init} (Phase 3); until then AE2
 * does not initialize in a Fabric client environment.
 */
public class AppEngFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            init(new AppEngServer());
        }
        // On the client, initialization is driven by the client entrypoint (Phase 3), because the
        // dist-specific AppEngBase subclass (AppEngClient) must be constructed first.
    }

    public static void init(AppEngBase base) {
        // Inject the loader-specific seam implementations before any registration content is class-loaded.
        var loaderPlatform = new FabricLoaderPlatform();
        AEApiLookups.init(new FabricApiLookups());
        LoaderPlatform.init(loaderPlatform);
        LoaderEventHooks.init(new FabricEventHooks());
        MenuTypePlatform.init(new FabricMenuTypePlatform());

        FabricConfigStore.initConfigs();

        base.registerContent();

        PlayerCtrlAttachment.init(new FabricPlayerCtrlAttachment());

        // Create the AEKeyType registry (synced, like NeoForge's RegistryBuilder.sync(true)) and flush
        // all collected registration entries in collector order.
        var keyTypeRegistry = FabricRegistryBuilder.create(AEKeyType.REGISTRY_KEY)
                .attribute(RegistryAttribute.SYNCED)
                .buildAndRegister();
        AEKeyTypesInternal.setRegistry(keyTypeRegistry);

        FabricRegistrar.registerAll();

        // Registry content NeoForge registers from its RegisterEvent dispatch.
        base.registerSounds(BuiltInRegistries.SOUND_EVENT);
        base.registerCreativeTabs(BuiltInRegistries.CREATIVE_MODE_TAB);
        InitStats.init(BuiltInRegistries.CUSTOM_STAT);
        InitAdvancementTriggers.init(BuiltInRegistries.TRIGGER_TYPES);
        InitParticleTypes.init(BuiltInRegistries.PARTICLE_TYPE);
        InitMenuTypes.init(BuiltInRegistries.MENU);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, SpatialStorageDimensionIds.CHUNK_GENERATOR_ID,
                SpatialStorageChunkGenerator.CODEC);
        InitVillager.initProfession(BuiltInRegistries.VILLAGER_PROFESSION);
        InitVillager.initPointOfInterestType(BuiltInRegistries.POINT_OF_INTEREST_TYPE);
        Registry.register(BuiltInRegistries.TEST_INSTANCE_TYPE, AppEng.makeId("plot_adapter"),
                GameTestPlotAdapter.CODEC);

        base.registerKeyTypes(keyTypeRegistry);
        // Keep the key-type cache up to date; mirrors the NeoForge BakeCallback (Fabric registries have no
        // bake/freeze callback, but entries can only be added during mod init).
        AEKeyTypesInternal.updateAllTypes();
        RegistryEntryAddedCallback.event(keyTypeRegistry)
                .register((rawId, id, keyType) -> AEKeyTypesInternal.updateAllTypes());

        // External creative mode tab content (e.g. tools also showing up in the vanilla tabs).
        CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register(
                (tab, output) -> BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab)
                        .ifPresent(tabKey -> MainCreativeTab.initExternal(tabKey, output::accept)));

        FabricNetworkInit.init();
        NetworkAdapter.init(new FabricNetworkAdapter());
        FluidPlatform.init(new FabricFluidPlatform());
        EnergyGeneratorPlatform.init(new FabricEnergyGenerator());
        CuriosSupport.init(new FabricCuriosSupport());
        ChunkLoadingService.init(new FabricChunkLoadingService());
        TestPlotPlatform.init(new FabricTestPlotPlatform());

        InitApiLookup.init();

        // NeoForge runs this enqueued from FMLCommonSetupEvent; on Fabric all registration (including
        // other mods') has completed by the end of mod initialization, so run it directly.
        base.postRegistrationInitialization();

        // Game-event wiring for the shared TickHandler (mirrors the NeoForge game-bus listeners).
        var tickHandler = TickHandler.instance();
        ServerLifecycleEvents.SERVER_STARTING.register(loaderPlatform::setCurrentServer);
        ServerTickEvents.START_SERVER_TICK.register(server -> tickHandler.onServerTickStart());
        ServerTickEvents.END_SERVER_TICK.register(server -> tickHandler.onServerTickEnd());
        ServerTickEvents.START_LEVEL_TICK.register(tickHandler::onServerLevelTickStart);
        ServerTickEvents.END_LEVEL_TICK.register(tickHandler::onServerLevelTickEnd);
        ServerChunkEvents.CHUNK_UNLOAD.register(
                (level, chunk) -> tickHandler.onUnloadChunk(level, chunk.getPos().pack()));
        // Quantum clusters must be destroyed before the TickHandler cleans up the level state below
        // (Fabric events fire in registration order within the same event).
        ServerLevelEvents.UNLOAD.register((server, level) -> QuantumCluster.onLevelUnload(level));
        ServerLevelEvents.UNLOAD.register((server, level) -> tickHandler.onUnloadLevel(level));

        // Forwarders for the chunk logger debug command (no-ops while the logger is disabled).
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) -> ChunkLogger.chunkLoaded(level, chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> ChunkLogger.chunkUnloaded(level, chunk));

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            base.onServerStopped();
            loaderPlatform.setCurrentServer(null);
        });
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> base.registerCommands(dispatcher));

        // Mirrors the NeoForge RightClickBlock wrapper: a non-PASS result cancels the interaction.
        UseBlockCallback.EVENT.register(WrenchHook::onPlayerUseBlock);

        // TODO (fabric): SkyStoneBreakSpeed has no Fabric event equivalent (NeoForge: PlayerEvent.BreakSpeed).
        // Needs a small mixin into Player#getDestroySpeed in a later step. Impact: sky stone does not break
        // faster with the appropriate tools until then.

        // TODO (fabric): server-synced recipe push (NeoForge: OnDatapackSyncEvent#sendRecipes for
        // base.getServerSyncedRecipeTypes()) has no Fabric equivalent yet. AE2's crafting terminal recipe
        // features and GuideME need these on the client; resolve in the runtime/networking step.

        HotkeyActions.init();
    }
}
