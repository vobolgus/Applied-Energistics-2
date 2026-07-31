/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2025, TeamAppliedEnergistics, All rights reserved.
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

package appeng.fabric;

import appeng.api.parts.RegisterPartApiEvent;

/**
 * Fabric entrypoint through which addons hook into AE2's initialization: ordered content registration
 * ({@link #registerContent()}) and block APIs on cable-bus parts ({@link #registerPartApis}, the Fabric counterpart to
 * subscribing to NeoForge's {@code RegisterPartCapabilitiesEvent}). Declare an implementation under the
 * {@value #ENTRYPOINT} entrypoint key in your {@code fabric.mod.json}:
 *
 * <pre>{@code
 * "entrypoints": {
 *   "ae2:registration": [ "com.example.MyAddonRegistration" ]
 * }
 * }</pre>
 *
 * Fabric Loader instantiates one object per entrypoint key, so both methods below are invoked on the <em>same</em>
 * instance. Keep the constructor empty: the object is created during AE2's own init, i.e. before
 * {@link #registerContent()} has run.
 * <p>
 * This mirrors the client-side {@code ae2:client_registration} entrypoint; keep it to server-safe (common)
 * registrations only — it runs on both the client and the dedicated server.
 *
 * <h2>Ordering</h2>
 *
 * Fabric Loader does <strong>not</strong> order entrypoint invocation by mod dependencies: a {@code "depends": {"ae2":
 * "*"}} addon's own {@code ModInitializer} can (and does) run before AE2's. Worse, AE2 drives {@code AppEngFabric.init}
 * from the {@code main} entrypoint on a dedicated server but from the <em>{@code client}</em> entrypoint on a client,
 * and Fabric runs every {@code main} entrypoint before any {@code client} entrypoint — so an addon that registers
 * content from its own initializer lands <em>before</em> AE2's content on a client and <em>after</em> it on a server.
 * Static-registry raw ids are not synced on 26.1, so that asymmetry is a silent multiplayer {@code ItemStack}
 * corruption that singleplayer can never reproduce.
 * <p>
 * Both hooks below are invoked from inside {@code AppEngFabric.init(AppEngBase)}, the single static method both dists
 * funnel through, so they fire at the identical position in the identical statement sequence on the client and on the
 * dedicated server. Registering content from {@link #registerContent()} therefore guarantees the order "all AE2
 * content, then this addon's content" on both sides of a connection.
 *
 * <h2>The construction window — the implementation must be self-contained</h2>
 *
 * The same dist asymmetry cuts the other way, and it is part of the contract. On a dedicated server AE2 initializes
 * from its own {@code main} entrypoint, and Fabric Loader invokes {@code main} entrypoints in its load order, which is
 * sorted alphabetically by mod id ({@code ModPrioSorter}: root mods first, then {@code getId().compareTo}) — {@code
 * ae2} sorts before virtually every addon id. So on the server this entrypoint object's constructor and <em>both</em>
 * hooks typically run <strong>before the addon's own {@code ModInitializer}</strong>, while on a client (where AE2
 * initializes from its {@code client} entrypoint, after all {@code main} entrypoints) they run after it.
 * <p>
 * An implementation therefore must not rely on anything the addon's own initializer sets up — no config loaded by
 * {@code onInitialize}, no static caches it fills, no registrations it performs — and its class/static initializers
 * must not touch such state either; anything {@link #registerContent()} needs has to be created by this entrypoint
 * itself. The instance is created when the first hook is dispatched ({@link #registerPartApis}, from AE2's
 * {@code InitApiLookup.init()}), i.e. in the middle of AE2's init: after AE2's content has been flushed to the game
 * registries, but before {@code postRegistrationInitialization()} and before {@link #registerContent()}.
 */
public interface AE2FabricRegistration {
    /**
     * The {@code fabric.mod.json} entrypoint key addons declare their implementations under.
     */
    String ENTRYPOINT = "ae2:registration";

    /**
     * Called once at the very end of {@code AppEngFabric.init(AppEngBase)}, i.e. after AE2 has finished <em>all</em> of
     * its own initialization: config store, content flushed into the game registries, key types, networking, the
     * platform seams, {@code postRegistrationInitialization()} and the hotkey registry. Register the addon's own
     * blocks/items/block entities/data components/menus/payloads from here so their raw registry ids are assigned after
     * AE2's on both dists (see the ordering section above); this is the Fabric equivalent of a NeoForge addon's mod
     * constructor plus {@code FMLCommonSetupEvent}, which the mod bus orders after AE2's by dependency.
     * <p>
     * Note that {@link #registerPartApis} runs <em>earlier</em> (during {@code InitApiLookup.init()}, which has to
     * complete before the part-API forwarding is installed). An addon that registers a custom part host with
     * {@code RegisterPartApiEvent#addHostType} must therefore create that {@code BlockEntityType} independently of
     * {@link #registerContent()}.
     */
    default void registerContent() {
    }

    /**
     * Called once at init so the addon can register its part-API providers on the given event.
     */
    default void registerPartApis(RegisterPartApiEvent event) {
    }
}
