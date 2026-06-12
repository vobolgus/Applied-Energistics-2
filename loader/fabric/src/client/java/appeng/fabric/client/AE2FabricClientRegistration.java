package appeng.fabric.client;

import appeng.client.api.model.parts.RegisterPartModelsEvent;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;

/**
 * Addon-facing entrypoint for AE2 client registrations on Fabric. Declare an implementation under the
 * {@code ae2:client_registration} entrypoint key in your {@code fabric.mod.json} to register part model types and part
 * renderers (the Fabric analog of subscribing to AE2's
 * {@code RegisterPartModelsEvent}/{@code RegisterPartRendererEvent} on the NeoForge mod event bus).
 */
public interface AE2FabricClientRegistration {
    /**
     * Register part model types. Called when AE2's part model registry is built (before models load).
     */
    default void registerPartModels(RegisterPartModelsEvent event) {
    }

    /**
     * Register part renderers. Called when the part renderer dispatcher (re-)loads.
     */
    default void registerPartRenderers(RegisterPartRendererEvent event) {
    }
}
