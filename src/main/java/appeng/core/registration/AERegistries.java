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

package appeng.core.registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Loader-neutral collector for registry entries, replacing NeoForge's {@code DeferredRegister} pattern in the shared
 * sources.
 * <p>
 * Entries are collected by the static initializers of the various definition classes (in deterministic order) and are
 * flushed into the actual game registries by loader-specific code (e.g. {@code NeoForgeRegistrar}). Insertion order is
 * preserved both for the registries themselves and for the entries within each registry, since registration order
 * determines raw registry ids.
 */
public final class AERegistries {
    private static final Map<ResourceKey<? extends Registry<?>>, List<AERegistryEntry<?, ?>>> ENTRIES = new LinkedHashMap<>();

    private AERegistries() {
    }

    public static synchronized <R, T extends R> AERegistryEntry<R, T> register(
            ResourceKey<? extends Registry<R>> registryKey, Identifier id, Supplier<? extends T> factory) {
        var entry = new AERegistryEntry<R, T>(registryKey, id, factory);
        addEntry(entry);
        return entry;
    }

    /**
     * Registers an item whose factory receives an {@link Item.Properties} with the item id already applied. This
     * mirrors the semantics of NeoForge's {@code DeferredRegister.Items#registerItem}.
     */
    public static <T extends Item> AEItemEntry<T> registerItem(Identifier id, Function<Item.Properties, T> factory) {
        return registerItem(id,
                () -> factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    }

    public static synchronized <T extends Item> AEItemEntry<T> registerItem(Identifier id,
            Supplier<? extends T> factory) {
        var entry = new AEItemEntry<T>(id, factory);
        addEntry(entry);
        return entry;
    }

    /**
     * Registers a block whose factory receives a {@link BlockBehaviour.Properties} with the block id already applied.
     * This mirrors the semantics of NeoForge's {@code DeferredRegister.Blocks#registerBlock}.
     */
    public static synchronized <T extends Block> AEBlockEntry<T> registerBlock(Identifier id,
            Function<BlockBehaviour.Properties, T> factory) {
        var entry = new AEBlockEntry<T>(id,
                () -> factory.apply(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id))));
        addEntry(entry);
        return entry;
    }

    private static synchronized void addEntry(AERegistryEntry<?, ?> entry) {
        ENTRIES.computeIfAbsent(entry.getRegistryKey(), key -> new ArrayList<>()).add(entry);
    }

    /**
     * @return The keys of all registries for which entries were collected, in the order the registries were first used.
     */
    public static synchronized Set<ResourceKey<? extends Registry<?>>> registryKeys() {
        return Collections.unmodifiableSet(ENTRIES.keySet());
    }

    /**
     * @return The entries collected for the given registry, in registration order.
     */
    public static synchronized List<AERegistryEntry<?, ?>> entries(ResourceKey<? extends Registry<?>> registryKey) {
        return Collections.unmodifiableList(ENTRIES.getOrDefault(registryKey, List.of()));
    }
}
