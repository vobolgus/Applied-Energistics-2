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

package appeng.fabric.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperUnbakedModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.FabricMaterialBaker;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.client.StorageCellModels;
import appeng.client.AppEngClient;
import appeng.client.ClientLoaderHooks;
import appeng.client.Hotkeys;
import appeng.client.InitScreens;
import appeng.client.areaoverlay.AreaOverlayRenderer;
import appeng.client.gui.style.StyleManager;
import appeng.client.hooks.BlockAttackHook;
import appeng.client.hooks.RenderBlockOutlineHook;
import appeng.client.integrations.itemlists.FluidBlockPictureInPictureRenderer;
import appeng.client.model.ModelFaceMetadata;
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
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.fabric.AppEngFabric;
import appeng.fabric.FabricLoaderPlatform;
import appeng.fabric.network.SyncRecipesPayload;

/**
 * The Fabric client entrypoint (the dedicated-server side is initialized by {@code AppEngFabric} directly). Mirrors
 * {@code appeng.neoforge.client.AppEngNeoForgeClient}: wires the loader-free lifecycle and registration methods exposed
 * by {@link AppEngClient} to fabric-api client events, and carries the registrations that use Fabric-only client APIs
 * (block state model codecs, extra models, reload listeners, model loading plugin).
 * <p>
 * AE2 registers its own part models/renderers through the same addon-facing mechanism it exposes to other mods: the
 * {@code ae2:client_registration} entrypoint (see {@link AE2ClientSelfRegistration} - a separate class because Fabric
 * instantiates one object per entrypoint key, and this class must only be constructed once).
 */
public class AppEngFabricClient extends AppEngClient implements ClientModInitializer {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AppEngFabricClient.class);

    /**
     * This modifier key has to be held to activate mouse wheel items. (NeoForge attaches its IN_GAME key conflict
     * context here; Fabric key mappings have no conflict contexts.)
     */
    private static final KeyMapping MOUSE_WHEEL_ITEM_MODIFIER = new KeyMapping(
            "key.ae2.mouse_wheel_item_modifier", InputConstants.Type.KEYSYM,
            InputConstants.KEY_LSHIFT, Hotkeys.CATEGORY);

    private static final KeyMapping PART_PLACEMENT_OPPOSITE = new KeyMapping(
            "key.ae2.part_placement_opposite", InputConstants.Type.KEYSYM,
            InputConstants.KEY_LCONTROL, Hotkeys.CATEGORY);

    /**
     * Recipes received from the server but not yet applied (the type-id payload completes a sync).
     */
    private final List<RecipeHolder<?>> pendingRecipes = new ArrayList<>();

    public AppEngFabricClient() {
        super(MOUSE_WHEEL_ITEM_MODIFIER, PART_PLACEMENT_OPPOSITE);
    }

    @Override
    public void onInitializeClient() {
        ClientLoaderHooks.init(new FabricClientLoaderHooks());

        // The common initialization (seams, registration, lifecycle); the main entrypoint defers to us on the
        // client so that the dist-specific AppEngBase subclass is constructed first.
        AppEngFabric.init(this);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // AppEngClient#registerClientCommands builds on CommandSourceStack (NeoForge's client command
            // source); Fabric client commands use FabricClientCommandSource, so the (single, debug-only)
            // command is mirrored here.
            var builder = ClientCommands.literal("ae2client");
            if (AEConfig.instance().isDebugToolsEnabled()) {
                builder.then(ClientCommands.literal("highlight_gui_areas").executes(context -> {
                    var toggle = !AEConfig.instance().isShowDebugGuiOverlays();
                    AEConfig.instance().setShowDebugGuiOverlays(toggle);
                    AEConfig.instance().save();
                    context.getSource().sendFeedback(Component.literal("GUI Overlays: " + toggle));
                    return 0;
                }));
            }
            dispatcher.register(builder);
        });

        registerClientTooltipComponents(new ClientTooltipComponentRegistrar() {
            @Override
            public <T extends net.minecraft.world.inventory.tooltip.TooltipComponent> void register(Class<T> type,
                    java.util.function.Function<? super T, ? extends net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> factory) {
                ClientTooltipComponentCallback.EVENT.register(
                        component -> type.isInstance(component) ? factory.apply(type.cast(component)) : null);
            }
        });

        // Mirrors NeoForge's RegisterKeyMappingsEvent#registerCategory: the category instance is created in
        // shared code, so it is added to the (access-widened) sort order directly.
        if (!KeyMapping.Category.SORT_ORDER.contains(Hotkeys.CATEGORY)) {
            KeyMapping.Category.SORT_ORDER.add(Hotkeys.CATEGORY);
        }
        registerKeyMappings(KeyMappingHelper::registerKeyMapping);

        // MenuScreens#register is private in vanilla (NeoForge patches it for its event); access-widened here.
        InitScreens.init(MenuScreens::register);

        var clientResources = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        // NOTE: NeoForge orders this listener BEFORE the vanilla block-entity-renderer listener; Fabric runs
        // mod listeners after the vanilla ones. The dispatcher is only queried at render time, after the full
        // reload completed, so the relative order does not matter in practice (verify in Phase 3b).
        clientResources.registerReloadListener(
                new FabricReloadListenerWrapper(PartRendererDispatcher.ID, getPartRendererDispatcher()));
        clientResources.registerReloadListener(
                new FabricReloadListenerWrapper(AppEng.makeId("styles"), StyleManager.getReloadListener()));

        BlockAttackHook.install();
        initGuide();

        // NeoForge registers clientTickStart with EventPriority.LOWEST (as late as possible); Fabric events run
        // in registration order, which cannot be influenced - noted as a Phase 3b runtime risk.
        ClientTickEvents.START_CLIENT_TICK.register(minecraft -> clientTickStart());
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> clientTickEnd());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, minecraft) -> onPlayerLoggingIn());

        // The TooltipFlag#hasShiftDown silent patch (see Phase 2a notes) reads this supplier on Fabric.
        // Vanilla 26.1 exposes the modifier state on Minecraft (Screen#hasShiftDown no longer exists);
        // NeoForge's ClientTooltipFlag uses the same accessor.
        FabricLoaderPlatform.setShiftDownSupplier(() -> net.minecraft.client.Minecraft.getInstance().hasShiftDown());

        // Mouse-wheel and raw key input are wired through the client mixins
        // (appeng.fabric.mixins.client.MouseScrollMixin / KeyInputMixin); Fabric has no input events.

        FabricClientNetworkInit.init();
        ClientPlayNetworking.registerGlobalReceiver(SyncRecipesPayload.TYPE,
                (payload, context) -> handleRecipeSync(payload));

        registerEntityRenderers(EntityRendererRegistry::register);
        registerBlockEntityRenderers(new BlockEntityRendererRegistrar() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public <T extends BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> void register(
                    BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
                BlockEntityRendererRegistry.register((BlockEntityType) type, (BlockEntityRendererProvider) provider);
            }
        });
        registerEntityLayerDefinitions(
                (layerLocation, layerDefinition) -> ModelLayerRegistry.registerModelLayer(layerLocation,
                        layerDefinition::get));

        registerParticleProviders(new SpriteSetRegistrar() {
            @Override
            public <T extends net.minecraft.core.particles.ParticleOptions> void register(
                    net.minecraft.core.particles.ParticleType<T> type,
                    net.minecraft.client.particle.ParticleResources.SpriteParticleRegistration<T> registration) {
                ParticleProviderRegistry.getInstance().register(type, registration::create);
            }
        });
        registerParticleGroups(ParticleGroupRegistry::register);

        // Render pipelines are NOT pre-registered on Fabric (no API; the render backend compiles them lazily
        // on first use - GuideME precedent). registerRenderPipelines() is deliberately not called.

        // Item model properties / item models / item tint sources go into the vanilla codec mappers, which
        // fabric's transitive access wideners make accessible (GuideME precedent for item models).
        registerItemModelProperties(RangeSelectItemModelProperties.ID_MAPPER::put);
        registerItemModels(ItemModels.ID_MAPPER::put);
        registerItemTintSources(ItemTintSources.ID_MAPPER::put);
        registerBlockTintSources(BlockColorRegistry::register);

        registerBlockStateModels();
        registerExtraModels();
        registerEmissiveBlockModels();

        PictureInPictureRendererRegistry
                .register(context -> new FluidBlockPictureInPictureRenderer(context.bufferSource()));

        RenderBlockOutlineHook.install();
        new AreaOverlayRenderer().install();

        // NOTE (fabric, Phase 3b): the spatial storage dimension's custom sky/clouds/weather renderers
        // (NeoForge: RegisterCustomEnvironmentEffectRendererEvent + biome environment attributes) have no
        // fabric-api equivalent on 26.1; the dimension uses the vanilla default sky until then.

        // NOTE (fabric): NeoForge sends InterModComms to darkmodeeverywhere/framedblocks here; both are
        // NeoForge-only mods, nothing to do on Fabric.

        // NOTE (fabric): NeoForge registers CableBusBlockClientExtensions (particle/sound hooks) via
        // RegisterClientExtensionsEvent; deferred to Phase 3b (vanilla fallback particles/sounds until then).

        // Must run before models are baked; on Fabric all mod init completes before resource loading starts.
        initCustomClientRegistries();

        // The config screen extension point (NeoForge: ConfigurationScreen) has no Fabric equivalent without
        // ModMenu integration; deferred.

        // NeoForge runs clientSetup() enqueued on the main thread from FMLClientSetupEvent; on Fabric, mod
        // initialization already runs on the main thread after registration.
        clientSetup();
    }

    /**
     * Registers the custom block state model codecs (NeoForge: RegisterBlockStateModels event; Fabric: the static
     * fabric-model-loading registry).
     */
    private void registerBlockStateModels() {
        CustomUnbakedBlockStateModel.register(SingleSpinnableVariant.Unbaked.ID,
                SingleSpinnableVariant.Unbaked.MAP_CODEC);
        CustomUnbakedBlockStateModel.register(CableBusModel.Unbaked.ID, CableBusModel.Unbaked.MAP_CODEC);
        CustomUnbakedBlockStateModel.register(QuartzGlassModel.Unbaked.ID, QuartzGlassModel.Unbaked.MAP_CODEC);
        CustomUnbakedBlockStateModel.register(AppEng.makeId("drive"), DriveModel.Unbaked.MAP_CODEC);
        CustomUnbakedBlockStateModel.register(AppEng.makeId("spatial_pylon"), SpatialPylonModel.Unbaked.MAP_CODEC);
        CustomUnbakedBlockStateModel.register(AppEng.makeId("paint"), PaintSplotchesModel.Unbaked.MAP_CODEC);
        CustomUnbakedBlockStateModel.register(AppEng.makeId("qnb_formed"), QnbFormedModel.Unbaked.MAP_CODEC);
        CustomUnbakedBlockStateModel.register(CraftingCubeModel.Unbaked.ID, CraftingCubeModel.Unbaked.MAP_CODEC);
    }

    /**
     * Registers the standalone models (NeoForge: ModelEvent.RegisterStandalone; Fabric: extra models through a model
     * loading plugin). The plugin runs on every model load, so the storage-cell model registry (filled in
     * {@code initCustomClientRegistries()}) is read freshly each time.
     */
    private void registerExtraModels() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModel(CrankRenderer.HANDLE_MODEL,
                    new SimpleUnbakedExtraModel<BlockStateModelPart>(CrankRenderer.HANDLE_MODEL_ID,
                            (resolvedModel, baker) -> SimpleModelWrapper.bake(baker, CrankRenderer.HANDLE_MODEL_ID,
                                    BlockModelRotation.IDENTITY)));

            // For rendering the ME chest we require the original storage cell models as standalone models
            var standaloneModels = StorageCellModels.standaloneModels();
            for (var entry : StorageCellModels.models().entrySet()) {
                var key = standaloneModels.get(entry.getKey());
                if (key != null) {
                    pluginContext.addModel(key, SimpleUnbakedExtraModel.blockStateModel(entry.getValue()));
                }
            }
            pluginContext.addModel(StorageCellModels.getDefaultStandaloneModel(),
                    SimpleUnbakedExtraModel.blockStateModel(StorageCellModels.getDefaultModel()));
        });
    }

    private void registerEmissiveBlockModels() {
        var modelIdsByBlock = Map.of(
                AEBlocks.CONTROLLER.block(), Set.of(
                        AppEng.makeId("block/controller_block_online"),
                        AppEng.makeId("block/controller_block_conflicted"),
                        AppEng.makeId("block/controller_column_online"),
                        AppEng.makeId("block/controller_column_conflicted"),
                        AppEng.makeId("block/controllerinside_a_conflicted"),
                        AppEng.makeId("block/controllerinside_b_conflicted")),
                AEBlocks.MOLECULAR_ASSEMBLER.block(), Set.of(AppEng.makeId("block/molecular_assembler_lights")),
                AEBlocks.MYSTERIOUS_CUBE.block(), Set.of(AppEng.makeId("block/mysterious_cube")),
                AEBlocks.NOT_SO_MYSTERIOUS_CUBE.block(), Set.of(AppEng.makeId("block/mysterious_cube")));
        var modelIds = modelIdsByBlock.values().stream().flatMap(Set::stream)
                .collect(java.util.stream.Collectors.toSet());

        PreparableModelLoadingPlugin.register((sharedState, executor) -> java.util.concurrent.CompletableFuture
                .supplyAsync(() -> ModelFaceMetadata.loadEmissiveTextures(sharedState.resourceManager(),
                        modelIds::contains, LOG, "emissive block"), executor),
                (emissiveTexturesByModel, context) -> {
                    LOG.info("Loaded emissive face metadata for {} block models", emissiveTexturesByModel.size());
                    context.modifyModelOnLoad().register(ModelModifier.WRAP_PHASE, (model, modifierContext) -> {
                        var emissiveTextures = emissiveTexturesByModel.get(modifierContext.id());
                        if (emissiveTextures == null || emissiveTextures.isEmpty()) {
                            return model;
                        }
                        return new EmissiveUnbakedModel(model, emissiveTextures);
                    });
                    context.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, modifierContext) -> {
                        var modelIdsForBlock = modelIdsByBlock.get(modifierContext.state().getBlock());
                        if (modelIdsForBlock == null) {
                            return model;
                        }
                        var emissiveTextures = new HashSet<Identifier>();
                        for (var modelId : modelIdsForBlock) {
                            emissiveTextures.addAll(emissiveTexturesByModel.getOrDefault(modelId, Set.of()));
                        }
                        if (emissiveTextures.isEmpty()) {
                            return model;
                        }
                        var spriteFinder = ((FabricMaterialBaker) modifierContext.baker().materials())
                                .spriteFinder(QuadAtlas.BLOCK);
                        return new EmissiveBlockStateModel(model, Set.copyOf(emissiveTextures), spriteFinder);
                    });
                });
    }

    private static final class EmissiveUnbakedModel extends WrapperUnbakedModel {
        private final Set<Identifier> emissiveTextures;

        private EmissiveUnbakedModel(UnbakedModel wrapped, Set<Identifier> emissiveTextures) {
            super(wrapped);
            this.emissiveTextures = emissiveTextures;
        }

        @Override
        public UnbakedGeometry geometry() {
            var geometry = wrapped.geometry();
            if (geometry == null) {
                return null;
            }
            return (textureSlots, baker, modelState, modelName) -> {
                var baked = geometry.bake(textureSlots, baker, modelState, modelName);
                var result = new QuadCollection.Builder();
                for (var direction : Direction.values()) {
                    for (var quad : baked.getQuads(direction)) {
                        result.addCulledFace(direction, applyEmission(quad));
                    }
                }
                for (var quad : baked.getQuads(null)) {
                    result.addUnculledFace(applyEmission(quad));
                }
                return result.build();
            };
        }

        private net.minecraft.client.resources.model.geometry.BakedQuad applyEmission(
                net.minecraft.client.resources.model.geometry.BakedQuad quad) {
            if (!emissiveTextures.contains(quad.materialInfo().sprite().contents().name())) {
                return quad;
            }
            var material = quad.materialInfo();
            return new net.minecraft.client.resources.model.geometry.BakedQuad(
                    quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                    quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
                    new net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo(
                            material.sprite(), material.layer(), material.itemRenderType(), material.tintIndex(),
                            material.shade(), 15));
        }
    }

    private record EmissiveBlockStateModel(BlockStateModel delegate, Set<Identifier> emissiveTextures,
            SpriteFinder spriteFinder)
            implements
                BlockStateModel,
                FabricBlockStateModel {
        @Override
        public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
            delegate.collectParts(random, parts);
        }

        @Override
        public net.minecraft.client.resources.model.sprite.Material.Baked particleMaterial() {
            return delegate.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return delegate.materialFlags();
        }

        @Override
        public void emitQuads(QuadEmitter emitter,
                BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
                java.util.function.Predicate<@org.jspecify.annotations.Nullable Direction> cullTest) {
            emitter.pushTransform(quad -> {
                if (emissiveTextures.contains(spriteFinder.find(quad).contents().name())) {
                    quad.emissive(true);
                }
                return true;
            });
            try {
                ((FabricBlockStateModel) delegate).emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
        }
    }

    private void handleRecipeSync(SyncRecipesPayload payload) {
        if (payload.clear()) {
            pendingRecipes.clear();
        }
        pendingRecipes.addAll(payload.recipes());

        if (payload.availableRecipeTypes().isPresent()) {
            Set<RecipeType<?>> recipeTypes = new HashSet<>();
            for (var typeId : payload.availableRecipeTypes().get()) {
                var recipeType = BuiltInRegistries.RECIPE_TYPE.getValue(typeId);
                if (recipeType != null) {
                    recipeTypes.add(recipeType);
                }
            }
            receiveRecipes(RecipeMap.create(List.copyOf(pendingRecipes)), recipeTypes);
            pendingRecipes.clear();
        }
    }
}
