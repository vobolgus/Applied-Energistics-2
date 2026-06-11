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

import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A loader-neutral handle for an object that will be registered into a {@link Registry} at a later point in time.
 * <p>
 * Entries are collected by {@link AERegistries} and flushed into the actual registries by loader-specific code. Until
 * that has happened, {@link #get()} and {@link #getHolder()} will throw.
 */
public class AERegistryEntry<R, T extends R> implements Supplier<T> {
    private final ResourceKey<? extends Registry<R>> registryKey;
    private final Identifier id;
    private final Supplier<? extends T> factory;
    private T value;
    private Holder<R> holder;

    protected AERegistryEntry(ResourceKey<? extends Registry<R>> registryKey, Identifier id,
            Supplier<? extends T> factory) {
        this.registryKey = Objects.requireNonNull(registryKey, "registryKey");
        this.id = Objects.requireNonNull(id, "id");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public final ResourceKey<? extends Registry<R>> getRegistryKey() {
        return registryKey;
    }

    public final Identifier getId() {
        return id;
    }

    /**
     * @return The resource key of the registered element.
     */
    public final ResourceKey<R> getKey() {
        return ResourceKey.create(registryKey, id);
    }

    /**
     * @return The registered object.
     * @throws IllegalStateException If the object has not been registered yet.
     */
    @Override
    public final T get() {
        var value = this.value;
        if (value == null) {
            throw new IllegalStateException("Registry entry " + id + " for registry " + registryKey.identifier()
                    + " has not been registered yet");
        }
        return value;
    }

    /**
     * @return The holder for the registered object.
     * @throws IllegalStateException If the object has not been registered yet.
     */
    public final Holder<R> getHolder() {
        var holder = this.holder;
        if (holder == null) {
            throw new IllegalStateException("Registry entry " + id + " for registry " + registryKey.identifier()
                    + " has not been registered yet");
        }
        return holder;
    }

    /**
     * Invokes the factory to create the object to be registered. Called by loader-specific registration code.
     */
    public final T create() {
        return Objects.requireNonNull(factory.get(), "factory returned null for " + id);
    }

    /**
     * Binds this entry to the registered object. Called by loader-specific registration code immediately after the
     * object was registered, so that subsequently registered objects can already resolve this entry.
     */
    public final void bind(T value, Holder<R> holder) {
        this.value = Objects.requireNonNull(value, "value");
        this.holder = Objects.requireNonNull(holder, "holder");
    }
}
