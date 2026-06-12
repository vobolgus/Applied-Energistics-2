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

package appeng.fabric.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;

import appeng.server.testworld.GameTestPlotAdapter;

/**
 * Registers AE2's dynamically generated plot-based game tests on Fabric (the twin of
 * {@code appeng.neoforge.gametest.AENeoForgeGameTests}, which uses NeoForge's {@code RegisterGameTestsEvent}).
 * <p>
 * On 26.1 game tests are entries of the data-driven {@code minecraft:test_instance} registry. Vanilla offers no code
 * hook to add dynamic entries; Fabric API's gametest module solves this for its {@code @GameTest}-annotated entrypoint
 * methods with its own {@code RegistryDataLoaderMixin} that registers extra entries into the in-flight
 * {@link WritableRegistry} before it is frozen. AE2's plots are not annotated methods (they are generated
 * {@link GameTestInstance}s), so this mixin does the equivalent one level lower: just before
 * {@link RegistryLoadTask#freezeRegistry} freezes the {@code test_instance} registry, all plot adapters are registered
 * into it.
 * <p>
 * {@code test_instance} IS part of {@code RegistryDataLoader#SYNCHRONIZED_REGISTRIES} on 26.1 (the server syncs the
 * registered plots to the connecting client), so this must only fire for the server-side datapack load
 * ({@link ResourceManagerRegistryLoadTask}) and never for the network-received path ({@code NetworkRegistryLoadTask}),
 * where the plot entries already arrive over the wire - registering them again throws a duplicate-key error and aborts
 * the client's configuration phase (Phase 3b boot incident #4). NeoForge gates its equivalent
 * {@code RegisterGameTestsEvent} on the same distinction (the {@code fromResources} parameter of
 * {@code RegistryDataLoader#load}). Registration is gated on the {@code appeng.tests} system property, exactly like the
 * NeoForge twin. The {@code ae2:plot_adapter} test-instance type codec is registered by
 * {@code AppEngFabric#onInitialize}.
 */
@Mixin(RegistryLoadTask.class)
public abstract class GameTestRegistryLoadTaskMixin<T> {

    @Shadow
    @Final
    private WritableRegistry<T> registry;

    @Inject(method = "freezeRegistry", at = @At("HEAD"))
    private void ae2$registerPlotGameTests(Map<ResourceKey<?>, Exception> loadingErrors,
            CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ResourceManagerRegistryLoadTask)) {
            return; // Network-received registries already carry the plot entries synced from the server
        }
        if ((Object) this.registry.key() == Registries.TEST_INSTANCE && Boolean.getBoolean("appeng.tests")) {
            @SuppressWarnings("unchecked")
            var testInstances = (WritableRegistry<GameTestInstance>) this.registry;
            GameTestPlotAdapter.registerAll((id, instance) -> Registry.register(testInstances, id, instance));
        }
    }
}
