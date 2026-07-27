/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.client;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.MapCodec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.HitResult;

import guideme.Guide;
import guideme.compiler.TagCompiler;
import guideme.scene.ImplicitAnnotationStrategy;
import guideme.siteexport.AdditionalResourceExporter;
import guideme.siteexport.RecipeExporter;

import appeng.api.client.StorageCellModels;
import appeng.api.parts.CableRenderMode;
import appeng.api.parts.IPart;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.util.AEColor;
import appeng.client.api.AEKeyRendering;
import appeng.client.api.model.parts.CompositePartModel;
import appeng.client.api.model.parts.PartModel;
import appeng.client.api.model.parts.StaticPartModel;
import appeng.client.api.renderer.parts.PartRenderer;
import appeng.client.block.cablebus.CableBusColor;
import appeng.client.commands.ClientCommands;
import appeng.client.gui.me.common.PendingCraftingJobs;
import appeng.client.gui.me.common.PinnedKeys;
import appeng.client.guidebook.AEAdditionalExportData;
import appeng.client.guidebook.AERecipeExporter;
import appeng.client.guidebook.ConfigValueTagExtension;
import appeng.client.guidebook.PartAnnotationStrategy;
import appeng.client.item.ColorApplicatorItemModel;
import appeng.client.item.EnergyFillLevelProperty;
import appeng.client.item.PortableCellColorTintSource;
import appeng.client.item.StorageCellStateTintSource;
import appeng.client.model.CableAnchorPartModel;
import appeng.client.model.LevelEmitterPartModel;
import appeng.client.model.LockableMonitorPartModel;
import appeng.client.model.P2PFrequencyPartModel;
import appeng.client.model.PartModels;
import appeng.client.model.PlanePartModel;
import appeng.client.model.StatusIndicatorPartModel;
import appeng.client.render.AEColorItemTintSource;
import appeng.client.render.AERenderPipelines;
import appeng.client.render.ColorableBlockEntityBlockColor;
import appeng.client.render.FacadeItemModel;
import appeng.client.render.StaticBlockColor;
import appeng.client.render.StorageCellClientTooltipComponent;
import appeng.client.render.effects.CraftingParticle;
import appeng.client.render.effects.EnergyFx;
import appeng.client.render.effects.LightningArcFX;
import appeng.client.render.effects.LightningFX;
import appeng.client.render.effects.LightningFXGroup;
import appeng.client.render.effects.MatterCannonFX;
import appeng.client.render.effects.VibrantFX;
import appeng.client.render.model.MemoryCardItemModel;
import appeng.client.render.model.MeteoriteCompassModel;
import appeng.client.renderer.blockentity.CableBusRenderer;
import appeng.client.renderer.blockentity.ChargerRenderer;
import appeng.client.renderer.blockentity.CraftingMonitorRenderer;
import appeng.client.renderer.blockentity.CrankRenderer;
import appeng.client.renderer.blockentity.DriveRenderer;
import appeng.client.renderer.blockentity.InscriberRenderer;
import appeng.client.renderer.blockentity.MEChestRenderer;
import appeng.client.renderer.blockentity.MolecularAssemblerRenderer;
import appeng.client.renderer.blockentity.SkyStoneChestModel;
import appeng.client.renderer.blockentity.SkyStoneChestRenderer;
import appeng.client.renderer.blockentity.SkyStoneTankRenderer;
import appeng.client.renderer.entity.TinyTNTPrimedRenderer;
import appeng.client.renderer.keytypes.FluidKeyRenderer;
import appeng.client.renderer.keytypes.ItemKeyRenderer;
import appeng.client.renderer.part.MonitorRenderer;
import appeng.client.renderer.parts.PartRendererDispatcher;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.AppEngBase;
import appeng.core.PlayerCtrlAttachment;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEEntities;
import appeng.core.definitions.AEItems;
import appeng.core.network.NetworkAdapter;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.MouseWheelPacket;
import appeng.core.network.serverbound.UpdateHoldingCtrlPacket;
import appeng.core.particles.ParticleTypes;
import appeng.helpers.IMouseWheelItem;
import appeng.items.storage.StorageCellTooltipComponent;
import appeng.parts.reporting.ConversionMonitorPart;
import appeng.parts.reporting.StorageMonitorPart;
import appeng.util.Platform;

/**
 * Client-specific functionality. This class is loader-free; the loader-specific client entrypoint (NeoForge:
 * {@code appeng.neoforge.client.AppEngNeoForgeClient}) extends it and wires the lifecycle/registration methods exposed
 * here to its own event system.
 */
public class AppEngClient extends AppEngBase {
    private static final Logger LOG = LoggerFactory.getLogger(AppEngClient.class);
    public static final Identifier MODEL_CELL_ITEMS_1K = Identifier.parse(
            "ae2:block/drive_1k_item_cell");
    public static final Identifier MODEL_CELL_ITEMS_4K = Identifier.parse(
            "ae2:block/drive_4k_item_cell");
    public static final Identifier MODEL_CELL_ITEMS_16K = Identifier.parse(
            "ae2:block/drive_16k_item_cell");
    public static final Identifier MODEL_CELL_ITEMS_64K = Identifier.parse(
            "ae2:block/drive_64k_item_cell");
    public static final Identifier MODEL_CELL_ITEMS_256K = Identifier.parse(
            "ae2:block/drive_256k_item_cell");
    public static final Identifier MODEL_CELL_FLUIDS_1K = Identifier.parse(
            "ae2:block/drive_1k_fluid_cell");
    public static final Identifier MODEL_CELL_FLUIDS_4K = Identifier.parse(
            "ae2:block/drive_4k_fluid_cell");
    public static final Identifier MODEL_CELL_FLUIDS_16K = Identifier.parse(
            "ae2:block/drive_16k_fluid_cell");
    public static final Identifier MODEL_CELL_FLUIDS_64K = Identifier.parse(
            "ae2:block/drive_64k_fluid_cell");
    public static final Identifier MODEL_CELL_FLUIDS_256K = Identifier.parse(
            "ae2:block/drive_256k_fluid_cell");
    public static final Identifier MODEL_CELL_CREATIVE = Identifier.parse(
            "ae2:block/drive_creative_cell");

    private static AppEngClient INSTANCE;

    /**
     * This modifier key has to be held to activate mouse wheel items. Created by the loader-specific subclass (e.g.
     * with NeoForge's key conflict context attached).
     */
    protected final KeyMapping mouseWheelItemModifier;

    protected final KeyMapping partPlacementOpposite;

    /**
     * Last known cable render mode. Used to update all rendered blocks once at the end of the tick when the mode is
     * changed.
     */
    private CableRenderMode prevCableRenderMode = CableRenderMode.STANDARD;

    private final PartRendererDispatcher partRendererDispatcher = new PartRendererDispatcher();

    private PartModels partModels;

    // Recipes synchronized from the server
    private RecipeMap recipeMap = RecipeMap.EMPTY;
    private final Set<RecipeType<?>> knownRecipeTypes = Collections.newSetFromMap(new IdentityHashMap<>());

    private Guide guide;

    public AppEngClient(KeyMapping mouseWheelItemModifier, KeyMapping partPlacementOpposite) {
        this.mouseWheelItemModifier = mouseWheelItemModifier;
        this.partPlacementOpposite = partPlacementOpposite;

        INSTANCE = this;
    }

    /**
     * Registers the AE2 client debug commands with the given client command dispatcher.
     */
    public void registerClientCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("ae2client");
        if (AEConfig.instance().isDebugToolsEnabled()) {
            for (var commandBuilder : ClientCommands.DEBUG_COMMANDS) {
                commandBuilder.build(builder);
            }
        }
        dispatcher.register(builder);
    }

    /**
     * Creates the in-game guide. Must be called once by the loader-specific entrypoint during mod construction.
     */
    protected final void initGuide() {
        this.guide = createGuide();
    }

    private Guide createGuide() {

        return Guide.builder(AppEng.makeId("guide"))
                .folder("ae2guide")
                .extension(ImplicitAnnotationStrategy.EXTENSION_POINT, new PartAnnotationStrategy())
                .extension(TagCompiler.EXTENSION_POINT, new ConfigValueTagExtension())
                .extension(RecipeExporter.EXTENSION_POINT, new AERecipeExporter())
                .extension(AdditionalResourceExporter.EXTENSION_POINT, new AEAdditionalExportData())
                .build();
    }

    private void tickPinnedKeys(Minecraft minecraft) {
        // Only prune pinned keys when no screen is currently open
        if (minecraft.screen == null) {
            PinnedKeys.prune();
        }
    }

    @Override
    public Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public void registerHotkey(String id) {
        Hotkeys.registerHotkey(id);
    }

    public static AppEngClient instance() {
        return Objects.requireNonNull(INSTANCE, "AppEngClient is not initialized");
    }

    /**
     * Registers the client-side counterparts of AE2's tooltip components.
     */
    public void registerClientTooltipComponents(ClientTooltipComponentRegistrar registrar) {
        registrar.register(StorageCellTooltipComponent.class, StorageCellClientTooltipComponent::new);
    }

    /**
     * Registers AE2's key mappings (including the dynamically created hotkeys, whose registration this finalizes).
     */
    public void registerKeyMappings(Consumer<KeyMapping> registrar) {
        registrar.accept(mouseWheelItemModifier);
        registrar.accept(partPlacementOpposite);
        Hotkeys.finalizeRegistration(registrar);
    }

    /**
     * Client setup that must run on the client main thread after registration completes.
     */
    public void clientSetup() {
        try {
            AEKeyRendering.register(AEKeyType.items(), AEItemKey.class, new ItemKeyRenderer());
            AEKeyRendering.register(AEKeyType.fluids(), AEFluidKey.class, new FluidKeyRenderer());
        } catch (Throwable e) {
            LOG.error("AE2 failed postClientSetup", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles mouse-wheel input for {@link IMouseWheelItem}s.
     *
     * @return true if the scroll was consumed and the loader must cancel further processing of the input event.
     */
    public boolean onMouseWheel(double scrollDeltaY) {
        if (scrollDeltaY == 0) {
            return false;
        }

        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;
        if (mouseWheelItemModifier.isDown()) {
            var mainHand = player.getItemInHand(InteractionHand.MAIN_HAND)
                    .getItem() instanceof IMouseWheelItem;
            var offHand = player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof IMouseWheelItem;

            if (mainHand || offHand) {
                ServerboundPacket message = new MouseWheelPacket(scrollDeltaY > 0);
                NetworkAdapter.get().sendToServer(message);
                return true;
            }
        }

        return false;
    }

    /**
     * Handles raw keyboard input to track the part-placement modifier key.
     *
     * @param action a GLFW input action ({@link InputConstants#PRESS}, {@link InputConstants#REPEAT} or release)
     */
    public void onKeyInput(int key, int action) {
        // getBoundKey: KeyMapping#getKey() is a NeoForge patch; routed through the loader seam
        if (key == ClientLoaderHooks.get().getBoundKey(partPlacementOpposite).getValue()) {
            var player = Minecraft.getInstance().player;

            if (player != null) {
                var isDown = action == InputConstants.PRESS || action == InputConstants.REPEAT;
                syncHoldingCtrl(player, isDown);
            }
        }
    }

    /**
     * Key events alone are not stuck-proof: both loaders fire the raw key event from the tail of
     * {@code KeyboardHandler#keyPress}, which screen-consumed keys never reach — press Ctrl in-world (Ctrl is also
     * SPRINT), release it with chat/inventory open, and the release is lost, leaving the flag stuck and EVERY part
     * placement mirrored ("кабель ставится с противоположной стороны", live report 2026-07-27). Poll the physical key
     * state once per tick as the authoritative source; the event path above just makes updates same-frame.
     */
    private void pollHoldingCtrl() {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }
        var boundKey = ClientLoaderHooks.get().getBoundKey(partPlacementOpposite);
        if (boundKey.getType() != InputConstants.Type.KEYSYM || boundKey.getValue() == InputConstants.UNKNOWN.getValue()) {
            return; // mouse-bound or unbound: leave the event path in charge
        }
        var isDown = InputConstants.isKeyDown(minecraft.getWindow(), boundKey.getValue());
        syncHoldingCtrl(player, isDown);
    }

    private void syncHoldingCtrl(Player player, boolean isDown) {
        var previousIsDown = PlayerCtrlAttachment.get().isHoldingCtrl(player);
        if (previousIsDown != isDown) {
            PlayerCtrlAttachment.get().setHoldingCtrl(player, isDown);
            NetworkAdapter.get().sendToServer(new UpdateHoldingCtrlPacket(isDown));
        }
    }

    /**
     * Called at the start of every client tick (the loader should call this as late as possible within the tick start).
     */
    public void clientTickStart() {
        updateCableRenderMode();
        pollHoldingCtrl();
    }

    /**
     * Called at the end of every client tick.
     */
    public void clientTickEnd() {
        tickPinnedKeys(Minecraft.getInstance());
        Hotkeys.checkHotkeys();
    }

    /**
     * Called when the client player logs into a (local or remote) server.
     */
    public void onPlayerLoggingIn() {
        PendingCraftingJobs.clearPendingJobs();
        PinnedKeys.clearPinnedKeys();
    }

    @Override
    public HitResult getCurrentMouseOver() {
        return Minecraft.getInstance().hitResult;
    }

    private void updateCableRenderMode() {
        var currentMode = getCableRenderMode();

        // Handle changes to the cable-rendering mode
        if (currentMode == this.prevCableRenderMode) {
            return;
        }

        this.prevCableRenderMode = currentMode;

        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Invalidate all sections that contain a cable bus within view distance
        // This should asynchronously update the chunk meshes and as part of that use the new facade render mode
        var viewDistance = (int) Math.ceil(mc.levelRenderer.getLastViewDistance());
        ChunkPos.rangeClosed(mc.player.chunkPosition(), viewDistance).forEach(chunkPos -> {
            var chunk = mc.level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (chunk != null) {
                for (var i = 0; i < chunk.getSectionsCount(); i++) {
                    var section = chunk.getSection(i);
                    if (section.maybeHas(state -> state.is(AEBlocks.CABLE_BUS.block()))) {
                        mc.levelRenderer.setSectionDirty(chunkPos.x(), chunk.getSectionYFromSectionIndex(i),
                                chunkPos.z());
                    }
                }
            }
        });
    }

    @Override
    public CableRenderMode getCableRenderMode() {
        if (Platform.isServer()) {
            return super.getCableRenderMode();
        }

        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            return CableRenderMode.STANDARD;
        }

        return this.getCableRenderModeForPlayer(mc.player);
    }

    public Guide getGuide() {
        return Objects.requireNonNull(guide, "The guide has not been initialized yet");
    }

    @Override
    public void sendSystemMessage(Player player, Component text) {
        if (player == Minecraft.getInstance().player) {
            Minecraft.getInstance().gui.getChat().addServerSystemMessage(text);
        }
        super.sendSystemMessage(player, text);
    }

    @Override
    public RecipeMap getRecipeMapForType(Level level, RecipeType<?> recipeType) {
        if (level instanceof ClientLevel) {
            if (!knownRecipeTypes.contains(recipeType)) {
                LOG.warn("Haven't received recipes of type {} from server yet.", recipeType);
                return RecipeMap.EMPTY;
            }

            return recipeMap;
        }

        return super.getRecipeMapForType(level, recipeType);
    }

    /**
     * Receives the recipes synchronized from the server.
     */
    public void receiveRecipes(RecipeMap recipeMap, Collection<RecipeType<?>> recipeTypes) {
        this.recipeMap = recipeMap;
        knownRecipeTypes.clear();
        knownRecipeTypes.addAll(recipeTypes);
    }

    /**
     * Registers AE2's own part model types. The loader forwards this through the same addon-facing mechanism it exposes
     * via {@link ClientLoaderHooks#postRegisterPartModels}.
     */
    public void registerPartModelTypes(BiConsumer<Identifier, MapCodec<? extends PartModel.Unbaked>> registrar) {
        registrar.accept(StaticPartModel.Unbaked.ID, StaticPartModel.Unbaked.MAP_CODEC);
        registrar.accept(CompositePartModel.Unbaked.ID, CompositePartModel.Unbaked.MAP_CODEC);
        registrar.accept(P2PFrequencyPartModel.Unbaked.ID, P2PFrequencyPartModel.Unbaked.MAP_CODEC);
        registrar.accept(StatusIndicatorPartModel.Unbaked.ID, StatusIndicatorPartModel.Unbaked.MAP_CODEC);
        registrar.accept(LevelEmitterPartModel.Unbaked.ID, LevelEmitterPartModel.Unbaked.MAP_CODEC);
        registrar.accept(CableAnchorPartModel.Unbaked.ID, CableAnchorPartModel.Unbaked.MAP_CODEC);
        registrar.accept(LockableMonitorPartModel.Unbaked.ID, LockableMonitorPartModel.Unbaked.MAP_CODEC);
        registrar.accept(PlanePartModel.Unbaked.ID, PlanePartModel.Unbaked.MAP_CODEC);
    }

    /**
     * Initializes the custom client-side registries (part models, storage cell models). Must run before models are
     * baked.
     */
    public void initCustomClientRegistries() {
        partModels = new PartModels();

        StorageCellModels.registerModel(AEItems.ITEM_CELL_1K, AppEngClient.MODEL_CELL_ITEMS_1K);
        StorageCellModels.registerModel(AEItems.ITEM_CELL_4K, AppEngClient.MODEL_CELL_ITEMS_4K);
        StorageCellModels.registerModel(AEItems.ITEM_CELL_16K, AppEngClient.MODEL_CELL_ITEMS_16K);
        StorageCellModels.registerModel(AEItems.ITEM_CELL_64K, AppEngClient.MODEL_CELL_ITEMS_64K);
        StorageCellModels.registerModel(AEItems.ITEM_CELL_256K, AppEngClient.MODEL_CELL_ITEMS_256K);
        StorageCellModels.registerModel(AEItems.FLUID_CELL_1K, AppEngClient.MODEL_CELL_FLUIDS_1K);
        StorageCellModels.registerModel(AEItems.FLUID_CELL_4K, AppEngClient.MODEL_CELL_FLUIDS_4K);
        StorageCellModels.registerModel(AEItems.FLUID_CELL_16K, AppEngClient.MODEL_CELL_FLUIDS_16K);
        StorageCellModels.registerModel(AEItems.FLUID_CELL_64K, AppEngClient.MODEL_CELL_FLUIDS_64K);
        StorageCellModels.registerModel(AEItems.FLUID_CELL_256K, AppEngClient.MODEL_CELL_FLUIDS_256K);
        StorageCellModels.registerModel(AEItems.CREATIVE_CELL, AppEngClient.MODEL_CELL_CREATIVE);

        StorageCellModels.registerModel(AEItems.PORTABLE_ITEM_CELL1K, AppEngClient.MODEL_CELL_ITEMS_1K);
        StorageCellModels.registerModel(AEItems.PORTABLE_ITEM_CELL4K, AppEngClient.MODEL_CELL_ITEMS_4K);
        StorageCellModels.registerModel(AEItems.PORTABLE_ITEM_CELL16K, AppEngClient.MODEL_CELL_ITEMS_16K);
        StorageCellModels.registerModel(AEItems.PORTABLE_ITEM_CELL64K, AppEngClient.MODEL_CELL_ITEMS_64K);
        StorageCellModels.registerModel(AEItems.PORTABLE_ITEM_CELL256K, AppEngClient.MODEL_CELL_ITEMS_256K);
        StorageCellModels.registerModel(AEItems.PORTABLE_FLUID_CELL1K, AppEngClient.MODEL_CELL_FLUIDS_1K);
        StorageCellModels.registerModel(AEItems.PORTABLE_FLUID_CELL4K, AppEngClient.MODEL_CELL_FLUIDS_4K);
        StorageCellModels.registerModel(AEItems.PORTABLE_FLUID_CELL16K, AppEngClient.MODEL_CELL_FLUIDS_16K);
        StorageCellModels.registerModel(AEItems.PORTABLE_FLUID_CELL64K, AppEngClient.MODEL_CELL_FLUIDS_64K);
        StorageCellModels.registerModel(AEItems.PORTABLE_FLUID_CELL256K, AppEngClient.MODEL_CELL_FLUIDS_256K);
    }

    /**
     * Registers AE2's own part renderers. The loader forwards this through the same addon-facing mechanism it exposes
     * via {@link ClientLoaderHooks#collectPartRenderers}.
     */
    public void registerPartRenderers(PartRendererRegistrar registrar) {
        registrar.register(ConversionMonitorPart.class, new MonitorRenderer());
        registrar.register(StorageMonitorPart.class, new MonitorRenderer());
    }

    public PartModels getPartModels() {
        if (partModels == null) {
            throw new IllegalStateException("Client registries have not been initialized yet");
        }
        return partModels;
    }

    public PartRendererDispatcher getPartRendererDispatcher() {
        return partRendererDispatcher;
    }

    /**
     * Registers AE2's custom render pipelines.
     */
    public void registerRenderPipelines(Consumer<RenderPipeline> registrar) {
        registrar.accept(AERenderPipelines.LINES_BEHIND_BLOCK);
        registrar.accept(AERenderPipelines.LIGHTNING_FX);
    }

    /**
     * Registers AE2's entity renderers.
     */
    public void registerEntityRenderers(EntityRendererRegistrar registrar) {
        registrar.register(AEEntities.TINY_TNT_PRIMED.get(), TinyTNTPrimedRenderer::new);
    }

    /**
     * Registers AE2's block entity renderers.
     */
    public void registerBlockEntityRenderers(BlockEntityRendererRegistrar registrar) {
        registrar.register(AEBlockEntities.CRANK.get(), CrankRenderer::new);
        registrar.register(AEBlockEntities.INSCRIBER.get(), InscriberRenderer::new);
        registrar.register(AEBlockEntities.SKY_CHEST.get(), SkyStoneChestRenderer::new);
        registrar.register(AEBlockEntities.CHARGER.get(), ChargerRenderer::new);
        registrar.register(AEBlockEntities.DRIVE.get(), DriveRenderer::new);
        registrar.register(AEBlockEntities.ME_CHEST.get(), MEChestRenderer::new);
        registrar.register(AEBlockEntities.CRAFTING_MONITOR.get(), CraftingMonitorRenderer::new);
        registrar.register(AEBlockEntities.MOLECULAR_ASSEMBLER.get(), MolecularAssemblerRenderer::new);
        registrar.register(AEBlockEntities.CABLE_BUS.get(), CableBusRenderer::new);
        registrar.register(AEBlockEntities.SKY_STONE_TANK.get(), SkyStoneTankRenderer::new);
    }

    /**
     * Registers AE2's entity model layer definitions.
     */
    public void registerEntityLayerDefinitions(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> registrar) {
        registrar.accept(SkyStoneChestRenderer.MODEL_LAYER, SkyStoneChestModel::createSingleBodyLayer);
    }

    /**
     * Registers AE2's sprite-set particle providers.
     */
    public void registerParticleProviders(SpriteSetRegistrar registrar) {
        registrar.register(ParticleTypes.CRAFTING, CraftingParticle.Factory::new);
        registrar.register(ParticleTypes.ENERGY, EnergyFx.Factory::new);
        registrar.register(ParticleTypes.LIGHTNING_ARC, LightningArcFX.Factory::new);
        registrar.register(ParticleTypes.LIGHTNING, LightningFX.Factory::new);
        registrar.register(ParticleTypes.MATTER_CANNON, MatterCannonFX.Factory::new);
        registrar.register(ParticleTypes.VIBRANT, VibrantFX.Factory::new);
    }

    /**
     * Registers AE2's particle groups.
     */
    public void registerParticleGroups(
            BiConsumer<ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>> registrar) {
        registrar.accept(LightningFXGroup.GROUP, LightningFXGroup::new);
    }

    /**
     * Registers AE2's range-select item model properties.
     */
    public void registerItemModelProperties(
            BiConsumer<Identifier, MapCodec<? extends RangeSelectItemModelProperty>> registrar) {
        registrar.accept(EnergyFillLevelProperty.ID, EnergyFillLevelProperty.CODEC);
    }

    /**
     * Registers AE2's custom item model types.
     */
    public void registerItemModels(BiConsumer<Identifier, MapCodec<? extends ItemModel.Unbaked>> registrar) {
        registrar.accept(ColorApplicatorItemModel.Unbaked.ID, ColorApplicatorItemModel.Unbaked.MAP_CODEC);
        registrar.accept(MemoryCardItemModel.Unbaked.ID, MemoryCardItemModel.Unbaked.MAP_CODEC);
        registrar.accept(FacadeItemModel.Unbaked.ID, FacadeItemModel.Unbaked.MAP_CODEC);
        registrar.accept(MeteoriteCompassModel.Unbaked.ID, MeteoriteCompassModel.Unbaked.MAP_CODEC);
    }

    /**
     * Registers AE2's item tint sources.
     */
    public void registerItemTintSources(BiConsumer<Identifier, MapCodec<? extends ItemTintSource>> registrar) {
        registrar.accept(PortableCellColorTintSource.ID, PortableCellColorTintSource.MAP_CODEC);
        registrar.accept(StorageCellStateTintSource.ID, StorageCellStateTintSource.MAP_CODEC);
        registrar.accept(AEColorItemTintSource.ID, AEColorItemTintSource.MAP_CODEC);
    }

    /**
     * Registers AE2's block tint sources.
     */
    public void registerBlockTintSources(BlockTintSourceRegistrar registrar) {
        registrar.register(StaticBlockColor.createTintSources(AEColor.TRANSPARENT),
                AEBlocks.WIRELESS_ACCESS_POINT.block());
        registrar.register(CableBusColor.TINT_SOURCES, AEBlocks.CABLE_BUS.block());
        registrar.register(ColorableBlockEntityBlockColor.TINT_SOURCES, AEBlocks.ME_CHEST.block());
    }

    /**
     * Loader-neutral target for the client tooltip component registrations.
     */
    @FunctionalInterface
    public interface ClientTooltipComponentRegistrar {
        <T extends TooltipComponent> void register(Class<T> type,
                Function<? super T, ? extends ClientTooltipComponent> factory);
    }

    /**
     * Loader-neutral target for the entity renderer registrations.
     */
    @FunctionalInterface
    public interface EntityRendererRegistrar {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    /**
     * Loader-neutral target for the block entity renderer registrations.
     */
    @FunctionalInterface
    public interface BlockEntityRendererRegistrar {
        <T extends BlockEntity, S extends BlockEntityRenderState> void register(BlockEntityType<? extends T> type,
                BlockEntityRendererProvider<T, S> provider);
    }

    /**
     * Loader-neutral target for the sprite-set particle provider registrations.
     */
    @FunctionalInterface
    public interface SpriteSetRegistrar {
        <T extends ParticleOptions> void register(ParticleType<T> type,
                ParticleResources.SpriteParticleRegistration<T> registration);
    }

    /**
     * Loader-neutral target for the block tint source registrations.
     */
    @FunctionalInterface
    public interface BlockTintSourceRegistrar {
        void register(List<BlockTintSource> tintSources, Block... blocks);
    }

    /**
     * Loader-neutral target for the part renderer registrations.
     */
    @FunctionalInterface
    public interface PartRendererRegistrar {
        <T extends IPart> void register(Class<T> partClass, PartRenderer<? super T, ?> renderer);
    }
}
