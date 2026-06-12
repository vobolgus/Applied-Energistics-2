/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import appeng.api.client.StorageCellModels;
import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.client.render.AERenderTypes;

/**
 * The block entity renderer for ME chests takes care of rendering the right model for the inserted cell, as well as the
 * LED.
 * <p>
 * Fabric twin of the NeoForge class with the same FQN; NeoForge's standalone-model lookup becomes Fabric's
 * {@code FabricModelManager#getModel(ExtraModelKey)} (the keys are registered by the AE2 Fabric client entrypoint's
 * {@code ModelLoadingPlugin}). The dead {@code rotateQuadCullFaces} helper (commented out upstream, built on NeoForge
 * {@code QuadTransforms}) is omitted.
 */
public class MEChestRenderer implements BlockEntityRenderer<MEChestBlockEntity, MEChestRenderState> {

    private static final Matrix4fc IDENTITY = new Matrix4f();

    private final ModelManager modelManager;

    public MEChestRenderer(BlockEntityRendererProvider.Context context) {
        Minecraft client = Minecraft.getInstance();
        modelManager = client.getModelManager();
    }

    @Override
    public MEChestRenderState createRenderState() {
        return new MEChestRenderState();
    }

    @Override
    public void extractRenderState(MEChestBlockEntity be, MEChestRenderState state, float partialTicks, Vec3 cameraPos,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPos, crumblingOverlay);

        // Calculate the lightlevel in front of the drive for lighting the exposed cell model.
        if (be.getLevel() != null) {
            var frontPos = be.getBlockPos().relative(be.getFront());
            state.frontLightCoords = LevelRenderer.getLightCoords(be.getLevel(), frontPos);
        } else {
            state.frontLightCoords = LightCoordsUtil.FULL_BRIGHT;
        }

        var blockOrientation = BlockOrientation.get(be);
        state.extract(blockOrientation, be, partialTicks);

        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        var cellItem = be.getCellItem(0);
        if (cellItem == null) {
            state.cellModel.clear();
            return; // No cell inserted into chest
        }

        // Try to get the right cell chassis model from the drive model since it already
        // loads them all
        var cellModelKey = StorageCellModels.standaloneModel(cellItem);
        if (cellModelKey == null) {
            cellModelKey = StorageCellModels.getDefaultStandaloneModel();
        }
        var model = modelManager.getModel(cellModelKey);
        var modelParts = state.cellModel.setupModel(IDENTITY,
                model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT));
        model.collectParts(state.cellModel.scratchRandomSource(42L), modelParts);
    }

    @Override
    public void submit(MEChestRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
            CameraRenderState cameraRenderState) {

        var cellModel = state.cellModel;
        if (cellModel.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(state.blockOrientation.getQuaternion());
        poseStack.translate(-0.5, -0.5, -0.5);

        // The models are created for the top-left slot of the drive model,
        // we need to move them into place for the slot on the ME chest
        poseStack.translate(5 / 16.0, 4 / 16.0, 0);

        // TODO 26.1: Might not be necessary anymore, check lighting on cell
        // (NeoForge keeps a commented-out rotateQuadCullFaces helper here)
        state.cellModel.submit(
                poseStack,
                nodes,
                state.frontLightCoords,
                OverlayTexture.NO_OVERLAY,
                0);

        nodes.submitCustomGeometry(
                poseStack,
                AERenderTypes.STORAGE_CELL_LEDS,
                (pose, consumer) -> CellLedRenderer.renderLed(state.cellColors[0], consumer, pose));

        poseStack.popPose();
    }

}
