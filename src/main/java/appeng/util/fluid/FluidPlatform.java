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

package appeng.util.fluid;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;

import appeng.blockentity.storage.SkyStoneTankBlockEntity;

/**
 * Loader-neutral seam for the few fluid operations that have no vanilla equivalent (display names come from the
 * NeoForge fluid type / Fabric fluid variant attributes, bucket-interaction comes from the loader transfer API). The
 * loader-specific implementation (e.g. {@code appeng.neoforge.fluids.NeoForgeFluidPlatform}) is injected once during
 * mod construction via {@link #init}.
 */
public interface FluidPlatform {
    /**
     * @return The display name for the given fluid with the given data component changes applied.
     */
    Component getFluidDisplayName(Fluid fluid, DataComponentPatch components);

    /**
     * Lets the given player interact with the given tank using the fluid container item they are holding (e.g. fill or
     * empty a bucket).
     *
     * @return true if an interaction took place
     */
    boolean interactWithTank(Player player, InteractionHand hand, SkyStoneTankBlockEntity tank);

    /**
     * @return The sound that filling a bucket with the given fluid makes (NeoForge:
     *         {@code FluidType.getSound(SoundActions.BUCKET_FILL)}), or null if the fluid defines none (callers fall
     *         back to the vanilla bucket sounds).
     */
    @Nullable
    SoundEvent getBucketFillSound(Fluid fluid);

    static FluidPlatform get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific FluidPlatform has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(FluidPlatform platform) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The FluidPlatform has already been initialized");
        }
        Holder.INSTANCE = platform;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile FluidPlatform INSTANCE;

        private Holder() {
        }
    }
}
