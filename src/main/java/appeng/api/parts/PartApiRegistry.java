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

package appeng.api.parts;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Loader-neutral registry of block APIs/capabilities exposed by {@linkplain IPart parts}. The loader layer uses this
 * registry to make part hosts (i.e. the cable bus) forward block API queries to the part on the queried side.
 * <p>
 * The capability is stored as an opaque token: on NeoForge it is the {@code BlockCapability} (registrations are made
 * through {@code appeng.api.parts.RegisterPartCapabilitiesEvent}, which delegates here), on Fabric it will be the
 * {@code BlockApiLookup}.
 */
public final class PartApiRegistry {

    private final Set<BlockEntityType<? extends IPartHost>> hostTypes = new HashSet<>();

    private final Map<Object, Function<?, Direction>> contextMappers = new HashMap<>();

    private final Map<Object, Registration<?, ?>> capabilityRegistrations = new HashMap<>();

    /**
     * Provides an API instance for a part, given the lookup context.
     */
    @FunctionalInterface
    public interface PartApiProvider<P extends IPart, C, T> {
        @Nullable
        T getApi(P part, C context);
    }

    /**
     * When using capabilities with a context other than {@link Direction}, you need to register a mapping function for
     * AE2 to get the side from the context. It cannot determine which part on a part host should handle the capability
     * otherwise.
     */
    public <C> void registerContext(Object capability, Function<C, Direction> directionGetter) {
        contextMappers.put(capability, directionGetter);
    }

    /**
     * Expose a capability for a part class.
     * <p>
     * When looking for an API instance, providers are queried starting from the class of the part, and then moving up
     * to its superclass, and so on, until a provider returning a nonnull API is found.
     * <p>
     * If the context of the lookup is not {@link Direction}, you need to register a mapping function for your custom
     * context! That must be done before this function is called. Currently, the query will fail silently, but IT WILL
     * throw an exception in the future!
     */
    @SuppressWarnings("unchecked")
    public <T, C, P extends IPart> void register(Object capability,
            PartApiProvider<P, C, T> provider,
            Class<P> partClass) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(partClass, "partClass");
        Objects.requireNonNull(provider, "provider");

        if (partClass.isInterface() || Modifier.isAbstract(partClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "Capabilities can only be registered for concrete part classes: " + partClass.getCanonicalName());
        }

        var mapper = (Function<C, Direction>) contextMappers.getOrDefault(capability, c -> (Direction) c);

        var registrations = (Registration<T, C>) capabilityRegistrations
                .computeIfAbsent(capability, ignored -> new Registration<T, C>(capability, mapper));
        registrations.add(partClass, provider);
    }

    /**
     * Adds a new type of block entity that will participate in forwarding API lookups to its attached parts.
     */
    public <T extends BlockEntity & IPartHost> void addHostType(BlockEntityType<T> hostType) {
        hostTypes.add(hostType);
    }

    /**
     * The host block entity types that should forward API lookups to their parts.
     */
    public Set<BlockEntityType<? extends IPartHost>> getHostTypes() {
        return Collections.unmodifiableSet(hostTypes);
    }

    /**
     * The per-capability registrations collected so far.
     */
    public Collection<Registration<?, ?>> getRegistrations() {
        return Collections.unmodifiableCollection(capabilityRegistrations.values());
    }

    /**
     * All part-class providers registered for a single capability, along with the logic to dispatch a lookup on a part
     * host to the part on the queried side.
     */
    public static final class Registration<T, C> {
        private final Object capability;
        private final Function<C, Direction> contextToSide;
        private final Map<Class<? extends IPart>, PartApiProvider<?, C, T>> parts = new HashMap<>();

        Registration(Object capability, Function<C, Direction> contextToSide) {
            this.capability = capability;
            this.contextToSide = contextToSide;
        }

        <P extends IPart> void add(Class<P> partClass, PartApiProvider<P, C, T> provider) {
            if (parts.putIfAbsent(partClass, provider) != null) {
                throw new IllegalStateException("Cannot register an additional capability provider for part "
                        + partClass + " since there already is one for capability " + capability);
            }
        }

        /**
         * The opaque loader capability token this registration is for.
         */
        public Object capability() {
            return capability;
        }

        /**
         * Finds the API instance provided by the part on the side determined by the given context, or null.
         */
        @Nullable
        public T find(IPartHost partHost, C context) {
            // Get side from context
            var side = contextToSide.apply(context);
            var part = partHost.getPart(side);
            if (part != null) {
                return handlePart(part, context);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private <P extends IPart> T handlePart(P part, C context) {
            var partProvider = (PartApiProvider<P, C, T>) parts.get(part.getClass());
            if (partProvider != null) {
                return partProvider.getApi(part, context);
            }
            return null;
        }
    }
}
