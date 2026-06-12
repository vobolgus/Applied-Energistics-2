package appeng.fabric.mixins.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import appeng.client.AppEngClient;

/**
 * Fabric has no mouse-scroll input event; this mirrors NeoForge's {@code InputEvent.MouseScrollingEvent} firing point
 * in {@code MouseHandler#onScroll}: right after the scroll-wheel accumulation, before the spectator/hotbar handling
 * (verified via javap against the NeoForge-patched sources). Cancelling consumes the scroll like NeoForge does when the
 * event is canceled. The scaled offset is recomputed with the exact vanilla formula since it only exists as a local at
 * the injection point.
 */
@Mixin(MouseHandler.class)
public class MouseScrollMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll(JDD)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;", shift = At.Shift.AFTER), cancellable = true)
    private void ae2$onMouseWheel(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        boolean discreteScroll = this.minecraft.options.discreteMouseScroll().get();
        double scrollSensitivity = this.minecraft.options.mouseWheelSensitivity().get();
        double scaledYOffset = (discreteScroll ? Math.signum(yoffset) : yoffset) * scrollSensitivity;
        if (AppEngClient.instance().onMouseWheel(scaledYOffset)) {
            ci.cancel();
        }
    }
}
