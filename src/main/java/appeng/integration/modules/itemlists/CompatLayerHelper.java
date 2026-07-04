package appeng.integration.modules.itemlists;

import appeng.util.LoaderPlatform;

/**
 * We prevent most of the REI compat from loading when the JEI compat is loaded. Two exceptions:
 * <ul>
 * <li>We keep collapsible entries as these are REI only.</li>
 * <li>We change crafting transfer handler behavior because REI doesn't center shaped recipes unlike JEI.</li>
 * </ul>
 * <p>
 * Lazy on purpose: REI instantiates the {@code rei_common}/{@code rei_client} entrypoints during ITS OWN
 * loader entrypoint, which can (on the client: always does) run before AE2's initializer has called
 * {@link LoaderPlatform#init} — a static-final field here crashed plugin construction (found when REI
 * shipped 26.1 and the dormant integration was first booted). Callers must only query this from REI
 * registration callbacks (post-init), never from plugin constructors.
 */
public class CompatLayerHelper {
    private static Boolean isLoaded;

    public static boolean isLoaded() {
        var result = isLoaded;
        if (result == null) {
            result = isLoaded = LoaderPlatform.get().isModLoaded("rei_plugin_compatibilities");
        }
        return result;
    }
}
