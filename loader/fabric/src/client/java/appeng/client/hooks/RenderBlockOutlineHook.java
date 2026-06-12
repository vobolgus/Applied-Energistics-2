package appeng.client.hooks;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import appeng.api.implementations.items.IFacadeItem;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.client.render.AERenderTypes;
import appeng.core.AEConfig;
import appeng.core.definitions.AEParts;
import appeng.items.parts.FacadeItem;
import appeng.parts.BusCollisionHelper;
import appeng.parts.PartPlacement;

/**
 * Fabric twin of the NeoForge class with the same FQN. NeoForge extracts the custom renderers in
 * {@code ExtractBlockOutlineRenderStateEvent} and runs them at outline-render time; on Fabric both steps run inside
 * {@link LevelRenderEvents#BEFORE_BLOCK_OUTLINE} (returning {@code false} suppresses the vanilla outline, mirroring a
 * NeoForge custom renderer returning {@code true}).
 */
public class RenderBlockOutlineHook {
    private RenderBlockOutlineHook() {
    }

    public static void install() {
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineState) -> {
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            var level = minecraft.level;
            if (player == null || level == null || outlineState == null) {
                return true;
            }

            if (!(minecraft.hitResult instanceof BlockHitResult blockHitResult)) {
                return true;
            }

            var poseStack = context.poseStack();
            var bufferSource = context.bufferSource();
            var cameraPos = context.levelState().cameraRenderState.pos;
            var pos = outlineState.pos();

            var itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            boolean suppressVanillaOutline = false;

            if (AEConfig.instance().isPlacementPreviewEnabled()) {
                if (!itemInHand.isEmpty() && itemInHand.getItem() instanceof IPartItem<?> partItem) {
                    var part = partItem.createPart();
                    var placement = PartPlacement.getPartPlacement(player,
                            player.level(),
                            itemInHand,
                            pos,
                            blockHitResult.getDirection(),
                            blockHitResult.getLocation());
                    if (placement != null) {
                        var cameraRelativePos = new Vec3(
                                placement.pos().getX() - cameraPos.x,
                                placement.pos().getY() - cameraPos.y,
                                placement.pos().getZ() - cameraPos.z);
                        // Render without depth test to also have a preview for parts inside blocks.
                        renderPart(poseStack, bufferSource, cameraRelativePos, part, placement.side(), true, true);
                        renderPart(poseStack, bufferSource, cameraRelativePos, part, placement.side(), true, false);
                    }
                }
            }

            // Hit test against all attached parts to highlight the part that is relevant
            if (level.getBlockEntity(pos) instanceof IPartHost partHost) {
                var cameraRelativePos = new Vec3(
                        pos.getX() - cameraPos.x,
                        pos.getY() - cameraPos.y,
                        pos.getZ() - cameraPos.z);

                // Rendering a preview of what is currently in hand has priority
                // If the item in hand is a facade and a block is hit, attempt facade placement
                if (AEConfig.instance().isPlacementPreviewEnabled()
                        && itemInHand.getItem() instanceof IFacadeItem facadeItem) {
                    var side = blockHitResult.getDirection();
                    var facade = facadeItem.createPartFromItemStack(itemInHand, side);
                    if (facade != null && FacadeItem.canPlaceFacade(partHost, facade)) {
                        // Maybe a bit hacky, but if there's no part on the side to support the facade
                        // We would render a cable anchor implicitly
                        boolean renderAnchor = partHost.getPart(side) == null;
                        for (var insideBlock : new boolean[] { true, false }) {
                            if (renderAnchor) {
                                var cableAnchor = AEParts.CABLE_ANCHOR.get().createPart();
                                renderPart(poseStack, bufferSource, cameraRelativePos, cableAnchor, side, true,
                                        insideBlock);
                            }
                            renderFacade(poseStack, bufferSource, cameraRelativePos, facade, side, true, insideBlock);
                        }
                    }
                }

                var selectedPart = partHost.selectPartWorld(blockHitResult.getLocation());
                if (selectedPart.facade != null) {
                    renderFacade(poseStack, bufferSource, cameraRelativePos, selectedPart.facade, selectedPart.side,
                            false, false);
                    suppressVanillaOutline = true;
                } else if (selectedPart.part != null) {
                    renderPart(poseStack, bufferSource, cameraRelativePos, selectedPart.part, selectedPart.side, false,
                            false);
                    suppressVanillaOutline = true;
                }
            }

            return !suppressVanillaOutline;
        });
    }

    private static void renderPart(PoseStack poseStack,
            MultiBufferSource buffers,
            Vec3 cameraRelativePos,
            IPart part,
            Direction side,
            boolean preview,
            boolean insideBlock) {
        var boxes = new ArrayList<AABB>();
        var helper = new BusCollisionHelper(boxes, side, true);
        part.getBoxes(helper);
        renderBoxes(poseStack, buffers, cameraRelativePos, boxes, preview, insideBlock);
    }

    private static void renderFacade(PoseStack poseStack,
            MultiBufferSource buffers,
            Vec3 cameraRelativePos,
            IFacadePart facade,
            Direction side,
            boolean preview,
            boolean insideBlock) {
        var boxes = new ArrayList<AABB>();
        var helper = new BusCollisionHelper(boxes, side, true);
        facade.getBoxes(helper, false);
        renderBoxes(poseStack, buffers, cameraRelativePos, boxes, preview, insideBlock);
    }

    private static void renderBoxes(PoseStack poseStack,
            MultiBufferSource buffers,
            Vec3 cameraRelativePos,
            List<AABB> boxes,
            boolean preview,
            boolean insideBlock) {
        RenderType renderType = insideBlock ? AERenderTypes.LINES_BEHIND_BLOCK : RenderTypes.lines();
        var buffer = buffers.getBuffer(renderType);
        float alpha = insideBlock ? 0.2f : preview ? 0.6f : 0.4f;

        for (var box : boxes) {
            var shape = Shapes.create(box);

            ShapeRenderer.renderShape(
                    poseStack,
                    buffer,
                    shape,
                    cameraRelativePos.x,
                    cameraRelativePos.y,
                    cameraRelativePos.z,
                    ARGB.colorFromFloat(alpha,
                            preview ? 1 : 0,
                            preview ? 1 : 0,
                            preview ? 1 : 0),
                    7 /* line width */);
        }
    }
}
