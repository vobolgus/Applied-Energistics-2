/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.integration.modules.rei;

import me.shedaniel.rei.api.common.entry.type.EntryTypeRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

import appeng.api.integrations.rei.IngredientConverters;
import appeng.core.AELog;
import appeng.integration.modules.itemlists.CompatLayerHelper;

/**
 * The common (both-sides) REI plugin: registers the {@link IngredientConverters} used to translate between AE2
 * {@link appeng.api.stacks.GenericStack}s and REI entry stacks.
 * <p>
 * Loader wiring: on Fabric this is the {@code rei_common} entrypoint (see fabric.mod.json); on NeoForge the annotated
 * subclass {@code appeng.integration.modules.rei.NeoForgeReiPlugin} in the loader overlay carries
 * {@code @REIPluginCommon}. The client-side plugin is {@code appeng.client.integration.rei.ReiClientPlugin}.
 */
// was REIServerPlugin + @me.shedaniel.rei.forge.REIPluginCommon: the interface was renamed to REICommonPlugin in
// REI 21.11 and the annotation is NeoForge-only (moved to the loader overlay subclass).
public class ReiPlugin implements REICommonPlugin {
    private boolean initialized;

    // The constructor must stay EMPTY: REI instantiates this entrypoint during ITS OWN loader
    // entrypoint, before AE2's initializer ran (LoaderPlatform not injected yet) — booting REI
    // 26.1.819 crashed here while the body lived in the ctor. registerEntryTypes is the earliest
    // REI callback and always runs post-init.
    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (CompatLayerHelper.isLoaded()) {
            return;
        }

        AELog.info("Registering AE2 REI common plugin (ingredient converters)");

        IngredientConverters.register(new ItemIngredientConverter());
        IngredientConverters.register(new FluidIngredientConverter());

        // NOTE: the ItemListMod adapter registration moved to ReiClientPlugin - REIRuntime is a client-only
        // class and this plugin is also instantiated on dedicated servers.
    }

    @Override
    public void registerEntryTypes(EntryTypeRegistry registry) {
        ensureInitialized();
    }

    @Override
    public String getPluginProviderName() {
        return "AE2";
    }

}
