package appeng.client.api.renderer.parts;

import org.jetbrains.annotations.ApiStatus;

import appeng.api.parts.IPart;

/**
 * Fabric twin of the NeoForge event with the same FQN. On Fabric this is a plain callback object passed to the
 * {@code ae2:client_registration} entrypoint ({@code appeng.fabric.client.AE2FabricClientRegistration}) instead of
 * being dispatched as a NeoForge {@code ParallelDispatchEvent} (registration runs sequentially on Fabric).
 */
public class RegisterPartRendererEvent {
    private final PartRegistrationSink delegate;

    @ApiStatus.Internal
    public RegisterPartRendererEvent(PartRegistrationSink delegate) {
        this.delegate = delegate;
    }

    public <T extends IPart> void register(Class<T> partClass, PartRenderer<? super T, ?> renderer) {
        delegate.register(partClass, renderer);
    }

    @FunctionalInterface
    @ApiStatus.Internal
    public interface PartRegistrationSink {
        <T extends IPart> void register(Class<T> partClass, PartRenderer<? super T, ?> factory);
    }
}
