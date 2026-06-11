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

package appeng.fabric.registration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import appeng.core.registration.AERegistries;
import appeng.core.registration.AERegistryEntry;

/**
 * Flushes the loader-neutral registration entries collected by {@link AERegistries} into the game registries. The
 * Fabric twin of {@code appeng.neoforge.registration.NeoForgeRegistrar}: entries are registered in insertion order
 * within each registry, so raw registry ids are deterministic.
 * <p>
 * Registries are visited in the order NeoForge fires its per-registry {@code RegisterEvent} (see
 * {@code GameData#getRegistrationOrder}): attributes, data component types and particle types first (items/blocks
 * depend on them at construction time), then the vanilla registries in root-registry registration order — this is what
 * guarantees e.g. blocks are registered before the block items whose factories resolve them — and modded registries
 * last.
 * <p>
 * All classes that collect entries into {@link AERegistries} must be loaded before this is called (i.e.
 * {@code registerContent()} must have run).
 */
public final class FabricRegistrar {
    private FabricRegistrar() {
    }

    public static void registerAll() {
        var registryKeys = new ArrayList<>(AERegistries.registryKeys());
        registryKeys.sort(Comparator
                .<ResourceKey<? extends Registry<?>>>comparingInt(FabricRegistrar::pinnedPriority)
                .thenComparingInt(FabricRegistrar::rootRegistryRawId));
        for (var registryKey : registryKeys) {
            registerEntries(castRegistryKey(registryKey));
        }
    }

    /**
     * The registries NeoForge pins to the front because vanilla's bootstrap order under-orders them.
     */
    private static int pinnedPriority(ResourceKey<? extends Registry<?>> registryKey) {
        var id = registryKey.identifier();
        if (id.equals(Registries.ATTRIBUTE.identifier())) {
            return 0;
        }
        if (id.equals(Registries.DATA_COMPONENT_TYPE.identifier())) {
            return 1;
        }
        if (id.equals(Registries.PARTICLE_TYPE.identifier())) {
            return 2;
        }
        return 3;
    }

    /**
     * Raw ids in the root registry reflect the order the registries were created in (vanilla bootstrap order for
     * vanilla registries, mod-init order for modded ones, which always come later).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static int rootRegistryRawId(ResourceKey<? extends Registry<?>> registryKey) {
        var registry = BuiltInRegistries.REGISTRY.getValue(registryKey.identifier());
        return registry != null ? ((Registry) BuiltInRegistries.REGISTRY).getId(registry) : Integer.MAX_VALUE;
    }

    @SuppressWarnings("unchecked")
    private static <T> ResourceKey<? extends Registry<T>> castRegistryKey(
            ResourceKey<? extends Registry<?>> registryKey) {
        return (ResourceKey<? extends Registry<T>>) registryKey;
    }

    @SuppressWarnings("unchecked")
    private static <T> void registerEntries(ResourceKey<? extends Registry<T>> registryKey) {
        var entries = AERegistries.entries(registryKey);
        if (entries.isEmpty()) {
            return;
        }

        var registry = (Registry<T>) Objects.requireNonNull(
                BuiltInRegistries.REGISTRY.getValue(registryKey.identifier()),
                () -> "Unknown registry " + registryKey);

        for (var entry : entries) {
            var typedEntry = (AERegistryEntry<T, T>) entry;
            var value = typedEntry.create();
            Registry.register(registry, typedEntry.getId(), value);
            // Bind immediately, so that entries registered later (e.g. block items resolving their block)
            // can already access this entry. This matches DeferredRegister/DeferredHolder semantics.
            typedEntry.bind(value, registry.wrapAsHolder(value));
        }
    }
}
