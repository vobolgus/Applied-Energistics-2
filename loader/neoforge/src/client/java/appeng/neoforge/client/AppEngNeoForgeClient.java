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

package appeng.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InitializeClientRegistriesEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;

import appeng.api.client.StorageCellModels;
import appeng.client.AppEngClient;
import appeng.client.ClientLoaderHooks;
import appeng.client.Hotkeys;
import appeng.client.InitScreens;
import appeng.client.api.model.parts.RegisterPartModelsEvent;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;
import appeng.client.areaoverlay.AreaOverlayRenderer;
import appeng.client.block.cablebus.CableBusBlockClientExtensions;
import appeng.client.gui.style.StyleManager;
import appeng.client.hooks.BlockAttackHook;
import appeng.client.hooks.RenderBlockOutlineHook;
import appeng.client.integrations.itemlists.FluidBlockPictureInPictureRenderer;
import appeng.client.model.PaintSplotchesModel;
import appeng.client.model.QnbFormedModel;
import appeng.client.model.SpatialPylonModel;
import appeng.client.render.cablebus.CableBusModel;
import appeng.client.render.crafting.CraftingCubeModel;
import appeng.client.render.model.DriveModel;
import appeng.client.render.model.QuartzGlassModel;
import appeng.client.render.model.SingleSpinnableVariant;
import appeng.client.renderer.blockentity.CrankRenderer;
import appeng.client.renderer.parts.PartRendererDispatcher;
import appeng.client.renderer.spatialstorage.SpatialStorageCloudsRenderer;
import appeng.client.renderer.spatialstorage.SpatialStorageSkyRenderer;
import appeng.client.renderer.spatialstorage.SpatialStorageWeatherEffectsRenderer;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.neoforge.AppEngNeoForge;
import appeng.spatial.SpatialStorageDimensionIds;

/**
 * The NeoForge mod entrypoint for the client (the dedicated-server entrypoint is
 * {@code appeng.neoforge.AppEngNeoForgeServer}). Wires every NeoForge client event to the loader-free lifecycle and
 * registration methods exposed by {@link AppEngClient}, and carries the registrations that use NeoForge-only client
 * APIs (block state models, standalone models, client extensions, environment effect renderers, IMC).
 */
@Mod(value = AppEng.MOD_ID, dist = Dist.CLIENT)
public class AppEngNeoForgeClient extends AppEngClient {
    /**
     * This modifier key has to be held to activate mouse wheel items.
     */
    private static final KeyMapping MOUSE_WHEEL_ITEM_MODIFIER = new KeyMapping(
            "key.ae2.mouse_wheel_item_modifier", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_LSHIFT, Hotkeys.CATEGORY);

    private static final KeyMapping PART_PLACEMENT_OPPOSITE = new KeyMapping(
            "key.ae2.part_placement_opposite", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_LCONTROL, Hotkeys.CATEGORY);

    private SpatialStorageSkyRenderer spatialStorageSkyRenderer;

    public AppEngNeoForgeClient(IEventBus modEventBus, ModContainer container) {
        super(MOUSE_WHEEL_ITEM_MODIFIER, PART_PLACEMENT_OPPOSITE);

        ClientLoaderHooks.init(new NeoForgeClientLoaderHooks());

        AppEngNeoForge.init(this, modEventBus, container);

        NeoForge.EVENT_BUS.addListener(
                (RegisterClientCommandsEvent evt) -> registerClientCommands(evt.getDispatcher()));

        modEventBus.addListener(
                (RegisterClientTooltipComponentFactoriesEvent e) -> registerClientTooltipComponents(e::register));
        modEventBus.addListener(this::registerHotkeys);
        modEventBus.addListener((RegisterMenuScreensEvent e) -> InitScreens.init(e::register));
        modEventBus.addListener(this::registerReloadListeners);

        BlockAttackHook.install();
        initGuide();

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ClientTickEvent.Pre e) -> {
            clientTickStart();
        });

        modEventBus.addListener(this::onClientSetup);

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn evt) -> onPlayerLoggingIn());

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> clientTickEnd());

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> new ConfigurationScreen(container, parent));

        modEventBus.addListener(NeoForgeClientNetworkInit::init);
        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            registerEntityRenderers(e::registerEntityRenderer);
            registerBlockEntityRenderers(e::registerBlockEntityRenderer);
        });
        modEventBus.addListener(
                (EntityRenderersEvent.RegisterLayerDefinitions e) -> registerEntityLayerDefinitions(
                        e::registerLayerDefinition));
        modEventBus.addListener(this::registerClientExtensions);
        modEventBus.addListener(this::enqueueImcMessages);
        modEventBus.addListener((RegisterParticleProvidersEvent e) -> registerParticleProviders(e::registerSpriteSet));
        modEventBus.addListener((RegisterParticleGroupsEvent e) -> registerParticleGroups(e::register));
        modEventBus.addListener(this::registerBlockStateModels);
        modEventBus.addListener(this::registerStandaloneModels);
        modEventBus.addListener((RegisterRenderPipelinesEvent e) -> registerRenderPipelines(e::registerPipeline));
        modEventBus.addListener((RegisterRangeSelectItemModelPropertyEvent e) -> registerItemModelProperties(
                e::register));
        modEventBus.addListener((RegisterItemModelsEvent e) -> registerItemModels(e::register));
        modEventBus.addListener(this::registerEnvironmentalEffectRenderers);
        modEventBus.addListener((RegisterColorHandlersEvent.ItemTintSources e) -> registerItemTintSources(e::register));
        modEventBus
                .addListener((RegisterColorHandlersEvent.BlockTintSources e) -> registerBlockTintSources(e::register));
        modEventBus.addListener(this::registerPipRenderers);

        RenderBlockOutlineHook.install();

        var areaOverlayRenderer = new AreaOverlayRenderer();
        NeoForge.EVENT_BUS.register(areaOverlayRenderer);

        modEventBus.addListener((RegisterPartRendererEvent e) -> registerPartRenderers(e::register));
        modEventBus.addListener((RegisterPartModelsEvent e) -> registerPartModelTypes(e::registerModelType));
        modEventBus.addListener((InitializeClientRegistriesEvent e) -> initCustomClientRegistries());
        NeoForge.EVENT_BUS.addListener(
                (RecipesReceivedEvent e) -> receiveRecipes(e.getRecipeMap(), e.getRecipeTypes()));
    }

    private void registerHotkeys(RegisterKeyMappingsEvent e) {
        e.registerCategory(Hotkeys.CATEGORY);
        registerKeyMappings(e::register);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> clientSetup());

        NeoForge.EVENT_BUS.addListener(this::wheelEvent);
        NeoForge.EVENT_BUS.addListener(this::ctrlEvent);
    }

    private void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(PartRendererDispatcher.ID, getPartRendererDispatcher());
        // The block entity render for parts needs access to the formed PartRendererDispatcher
        event.addDependency(VanillaClientListeners.BLOCK_ENTITY_RENDERER, PartRendererDispatcher.ID);

        event.addListener(AppEng.makeId("styles"), StyleManager.getReloadListener());
    }

    private void wheelEvent(InputEvent.MouseScrollingEvent me) {
        if (onMouseWheel(me.getScrollDeltaY())) {
            me.setCanceled(true);
        }
    }

    private void ctrlEvent(InputEvent.Key event) {
        onKeyInput(event.getKey(), event.getAction());
    }

    private void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(SingleSpinnableVariant.Unbaked.ID, SingleSpinnableVariant.Unbaked.MAP_CODEC);
        event.registerModel(CableBusModel.Unbaked.ID, CableBusModel.Unbaked.MAP_CODEC);
        event.registerModel(QuartzGlassModel.Unbaked.ID, QuartzGlassModel.Unbaked.MAP_CODEC);
        event.registerModel(AppEng.makeId("drive"), DriveModel.Unbaked.MAP_CODEC);
        event.registerModel(AppEng.makeId("spatial_pylon"), SpatialPylonModel.Unbaked.MAP_CODEC);
        event.registerModel(AppEng.makeId("paint"), PaintSplotchesModel.Unbaked.MAP_CODEC);
        event.registerModel(AppEng.makeId("qnb_formed"), QnbFormedModel.Unbaked.MAP_CODEC);
        event.registerModel(CraftingCubeModel.Unbaked.ID, CraftingCubeModel.Unbaked.MAP_CODEC);
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(new CableBusBlockClientExtensions(AEBlocks.CABLE_BUS.block()), AEBlocks.CABLE_BUS.block());
    }

    private void enqueueImcMessages(InterModEnqueueEvent event) {
        // Our new light-mode UI doesn't play nice with darkmodeeverywhere
        InterModComms.sendTo("darkmodeeverywhere", "dme-shaderblacklist", () -> "appeng.");
        InterModComms.sendTo("framedblocks", "add_ct_property", () -> QuartzGlassModel.GLASS_STATE);
    }

    private void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        event.register(CrankRenderer.HANDLE_MODEL,
                SimpleUnbakedStandaloneModel.simpleModelWrapper(CrankRenderer.HANDLE_MODEL_ID));

        // For rendering the ME chest we require the original storage cell models as standalone models
        for (var cellModelKey : StorageCellModels.standaloneModels().values()) {
            // TODO 1.21.8 Investigate what model debug name vs. model key vs. resource location is
            event.register(cellModelKey,
                    SimpleUnbakedStandaloneModel.blockStateModel(Identifier.parse(cellModelKey.getName())));
        }
        event.register(StorageCellModels.getDefaultStandaloneModel(), SimpleUnbakedStandaloneModel
                .blockStateModel(Identifier.parse(StorageCellModels.getDefaultStandaloneModel().getName())));
    }

    private void registerEnvironmentalEffectRenderers(RegisterCustomEnvironmentEffectRendererEvent event) {
        if (spatialStorageSkyRenderer == null) {
            spatialStorageSkyRenderer = new SpatialStorageSkyRenderer();
        }

        event.registerCloudRenderer(SpatialStorageDimensionIds.CUSTOM_RENDERER_ID, new SpatialStorageCloudsRenderer());
        event.registerSkyboxRenderer(SpatialStorageDimensionIds.CUSTOM_RENDERER_ID, spatialStorageSkyRenderer);
        event.registerWeatherEffectRenderer(SpatialStorageDimensionIds.CUSTOM_RENDERER_ID,
                new SpatialStorageWeatherEffectsRenderer());
    }

    private void registerPipRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(FluidBlockPictureInPictureRenderer.State.class, FluidBlockPictureInPictureRenderer::new);
    }
}
