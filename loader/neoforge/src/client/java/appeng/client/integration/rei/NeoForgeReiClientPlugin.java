package appeng.client.integration.rei;

import me.shedaniel.rei.forge.REIPluginClient;

/**
 * NeoForge REI discovery glue: REI-NeoForge scans mod classes for {@code @REIPluginClient}; the shared
 * {@link ReiClientPlugin} must stay annotation-free because the annotation class only exists in the REI NeoForge
 * artifacts. (On Fabric, {@code ReiClientPlugin} is wired through the {@code rei_client} entrypoint in fabric.mod.json
 * instead.)
 */
@REIPluginClient
public class NeoForgeReiClientPlugin extends ReiClientPlugin {
}
