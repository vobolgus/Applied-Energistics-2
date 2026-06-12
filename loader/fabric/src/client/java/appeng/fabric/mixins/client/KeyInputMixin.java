package appeng.fabric.mixins.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

import appeng.client.AppEngClient;

/**
 * Fabric has no raw key input event; this mirrors NeoForge's {@code InputEvent.Key} firing point at the end of
 * {@code KeyboardHandler#keyPress} (the fall-through path; screen-consumed keys early-return before it, matching
 * NeoForge). The window-handle guard is replicated in the handler since the injection point sits after the guarded
 * block.
 */
@Mixin(KeyboardHandler.class)
public class KeyInputMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("TAIL"))
    private void ae2$onKeyInput(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (handle != this.minecraft.getWindow().handle()) {
            return;
        }
        AppEngClient.instance().onKeyInput(event.key(), action);
    }
}
