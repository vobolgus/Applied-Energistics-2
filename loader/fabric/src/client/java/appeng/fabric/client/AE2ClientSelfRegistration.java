package appeng.fabric.client;

import appeng.client.AppEngClient;
import appeng.client.api.model.parts.RegisterPartModelsEvent;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;

/**
 * AE2's own registrations into its addon-facing {@code ae2:client_registration} entrypoint, mirroring how the NeoForge
 * entrypoint subscribes to its own addon events.
 * <p>
 * This is deliberately a separate class from {@link AppEngFabricClient}: Fabric Loader instantiates a fresh object per
 * entrypoint key, so listing the client entrypoint class here as well would construct the {@code AppEngClient}
 * singleton twice and trip the {@code AppEngBase} single-instance guard (Phase 3b boot incident #2). The entrypoint is
 * only ever invoked after the {@code client} entrypoint has constructed the singleton.
 */
public class AE2ClientSelfRegistration implements AE2FabricClientRegistration {
    @Override
    public void registerPartModels(RegisterPartModelsEvent event) {
        AppEngClient.instance().registerPartModelTypes(event::registerModelType);
    }

    @Override
    public void registerPartRenderers(RegisterPartRendererEvent event) {
        AppEngClient.instance().registerPartRenderers(event::register);
    }
}
