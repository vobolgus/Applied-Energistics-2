package appeng.fabric.mixins.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.material.FogType;

import appeng.client.renderer.spatialstorage.SpatialStorageSkyRenderer;
import appeng.spatial.SpatialStorageDimensionIds;

/**
 * Replaces the vanilla sky frame pass with AE2's spatial storage skybox (pitch black + sparkles) while inside the
 * spatial storage dimension. NeoForge does this via {@code RegisterCustomEnvironmentEffectRendererEvent} + a
 * {@code CUSTOM_SKYBOX} environment attribute on the spatial storage biome; fabric-api 26.1 has no equivalent
 * registry, so this mixin cancels {@code LevelRenderer#addSkyPass} for the spatial dimension and schedules an
 * equivalent pass. Clouds and weather need no twin: vanilla already skips clouds (the {@code CLOUD_COLOR} attribute
 * defaults to alpha 0) and precipitation visuals (the biome has no precipitation).
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererSpatialSkyMixin {
    @Shadow
    private ClientLevel level;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void ae2$renderSpatialStorageSky(FrameGraphBuilder frame, CameraRenderState cameraState,
            GpuBufferSlice skyFog, CallbackInfo ci) {
        if (this.level == null || this.level.dimension() != SpatialStorageDimensionIds.WORLD_ID) {
            return;
        }
        ci.cancel();

        // Mirror the vanilla gating for camera states that fully hide the sky
        FogType fogType = cameraState.fogType;
        if (fogType == FogType.POWDER_SNOW || fogType == FogType.LAVA
                || cameraState.entityRenderState.doesMobEffectBlockSky) {
            return;
        }

        var pass = frame.addPass("sky");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        // NeoForge's custom skybox hook doesn't apply the vanilla sky fog either (the renderer opts in via a
        // callback it never invokes), so skyFog is intentionally unused.
        pass.executes(() -> SpatialStorageSkyRenderer.getInstance().render(RenderSystem.getModelViewMatrix()));
    }
}
