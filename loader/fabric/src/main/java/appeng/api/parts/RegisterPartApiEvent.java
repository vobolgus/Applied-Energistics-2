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

import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import appeng.api.parts.PartApiRegistry.PartApiProvider;

/**
 * Fabric counterpart to NeoForge's {@link RegisterPartCapabilitiesEvent}: it lets addons expose a
 * Fabric {@link BlockApiLookup} on {@linkplain IPart parts} so that a query against a part host (the
 * cable bus) is forwarded to the part on the queried side.
 * <p>
 * AE2 constructs an instance over the shared {@link PartApiRegistry} and hands it to every
 * {@code ae2:registration} entrypoint (see {@code appeng.fabric.AE2FabricRegistration}). AE2 registers
 * its own part APIs <em>before</em> the entrypoints run, so a duplicate {@code (part, lookup)}
 * registration is rejected by the registry (first registration wins), mirroring NeoForge's
 * register-then-post ordering.
 * <p>
 * Typical addon usage:
 *
 * <pre>{@code
 * public final class MyAddonRegistration implements AE2FabricRegistration {
 *     @Override
 *     public void registerPartApis(RegisterPartApiEvent event) {
 *         event.register(ItemStorage.SIDED,
 *                 (MyPart part, Direction side) -> part.getItemStorage(),
 *                 MyPart.class);
 *     }
 * }
 * }</pre>
 */
public final class RegisterPartApiEvent {
    private final PartApiRegistry registry;

    @ApiStatus.Internal
    public RegisterPartApiEvent(PartApiRegistry registry) {
        this.registry = registry;
    }

    /**
     * Register a mapping from a lookup's context to a {@link Direction}, required when the lookup's
     * context type is not {@link Direction} itself (AE2 needs the side to pick the part). Must be called
     * before {@link #register} for the same lookup.
     */
    public <T, C> void registerContext(BlockApiLookup<T, C> apiLookup, Function<C, Direction> directionGetter) {
        Objects.requireNonNull(apiLookup, "apiLookup");
        Objects.requireNonNull(directionGetter, "directionGetter");
        registry.registerContext(apiLookup, directionGetter);
    }

    /**
     * Expose {@code apiLookup} on a concrete {@code partClass}: when the lookup is queried on a part
     * host, it is forwarded to the part on the queried side via {@code provider} (which may return
     * {@code null} to decline). Providers are matched from the part's class up its superclasses.
     *
     * @throws IllegalArgumentException if {@code partClass} is abstract or an interface
     * @throws IllegalStateException    if another provider is already registered for this
     *                                  {@code (part, lookup)} pair (e.g. one of AE2's own)
     */
    public <T, C, P extends IPart> void register(BlockApiLookup<T, C> apiLookup,
            PartApiProvider<P, C, T> provider,
            Class<P> partClass) {
        Objects.requireNonNull(apiLookup, "apiLookup");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(partClass, "partClass");
        registry.register(apiLookup, provider, partClass);
    }

    /**
     * Register an additional part-host block-entity type that should forward API lookups to its parts.
     * AE2 already adds the cable bus; addons only need this for their own custom part hosts.
     */
    public <T extends BlockEntity & IPartHost> void addHostType(BlockEntityType<T> hostType) {
        registry.addHostType(hostType);
    }
}
