package appeng.integration.modules.itemlists;

import appeng.util.LoaderPlatform;

/**
 * We prevent most of the REI compat from loading when the JEI compat is loaded. Two exceptions:
 * <ul>
 * <li>We keep collapsible entries as these are REI only.</li>
 * <li>We change crafting transfer handler behavior because REI doesn't center shaped recipes unlike JEI.</li>
 * </ul>
 */
public class CompatLayerHelper {
    public static final boolean IS_LOADED = LoaderPlatform.get().isModLoaded("rei_plugin_compatibilities");
}
