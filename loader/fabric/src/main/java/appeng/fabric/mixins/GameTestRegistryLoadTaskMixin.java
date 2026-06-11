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
 * {@code test_instance} is only listed in {@code RegistryDataLoader#WORLDGEN_REGISTRIES} (not in
 * {@code SYNCHRONIZED_REGISTRIES}), so this only ever fires for server-side datapack loads, never for the
 * network-received registry path. Registration is gated on the {@code appeng.tests} system property, exactly like the
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
        if ((Object) this.registry.key() == Registries.TEST_INSTANCE && Boolean.getBoolean("appeng.tests")) {
            @SuppressWarnings("unchecked")
            var testInstances = (WritableRegistry<GameTestInstance>) this.registry;
            GameTestPlotAdapter.registerAll((id, instance) -> Registry.register(testInstances, id, instance));
        }
    }
}
