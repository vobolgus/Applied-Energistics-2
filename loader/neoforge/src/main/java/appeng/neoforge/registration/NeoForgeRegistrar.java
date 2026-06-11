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

package appeng.neoforge.registration;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import appeng.core.registration.AERegistries;
import appeng.core.registration.AERegistryEntry;

/**
 * Flushes the loader-neutral registration entries collected by {@link AERegistries} into the game registries when
 * NeoForge fires its {@link RegisterEvent}.
 * <p>
 * All classes that collect entries into {@link AERegistries} must be loaded before the event fires (i.e. during mod
 * construction).
 */
public final class NeoForgeRegistrar {
    private NeoForgeRegistrar() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeRegistrar::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        registerEntries(event, castRegistryKey(event.getRegistryKey()));
    }

    @SuppressWarnings("unchecked")
    private static <T> ResourceKey<? extends Registry<T>> castRegistryKey(
            ResourceKey<? extends Registry<?>> registryKey) {
        return (ResourceKey<? extends Registry<T>>) registryKey;
    }

    private static <T> void registerEntries(RegisterEvent event, ResourceKey<? extends Registry<T>> registryKey) {
        var entries = AERegistries.entries(registryKey);
        if (entries.isEmpty()) {
            return;
        }

        var registry = event.getRegistry(registryKey);
        for (var entry : entries) {
            @SuppressWarnings("unchecked")
            var typedEntry = (AERegistryEntry<T, T>) entry;
            var value = typedEntry.create();
            event.register(registryKey, typedEntry.getId(), () -> value);
            // Bind immediately, so that entries registered later (e.g. block items resolving their block)
            // can already access this entry. This matches DeferredRegister/DeferredHolder semantics.
            typedEntry.bind(value, registry.wrapAsHolder(value));
        }
    }
}
