package appeng.integration.modules.rei;

import me.shedaniel.rei.forge.REIPluginCommon;

/**
 * NeoForge REI discovery glue: REI-NeoForge scans mod classes for {@code @REIPluginCommon}; the shared
 * {@link ReiPlugin} must stay annotation-free because the annotation class only exists in the REI NeoForge artifacts.
 * (On Fabric, {@code ReiPlugin} is wired through the {@code rei_common} entrypoint in fabric.mod.json instead.)
 */
@REIPluginCommon
public class NeoForgeReiPlugin extends ReiPlugin {
}
