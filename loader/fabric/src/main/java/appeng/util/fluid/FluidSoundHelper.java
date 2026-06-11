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

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import appeng.api.stacks.AEFluidKey;
import appeng.fabric.transfer.FabricResources;

/**
 * Helps with playing fill/empty sounds for fluids to players. The Fabric twin of the NeoForge overlay class of the same
 * name, using {@link FluidVariantAttributes} for the sounds.
 */
public final class FluidSoundHelper {

    private FluidSoundHelper() {
    }

    public static void playFillSound(Player player, @Nullable AEFluidKey fluid) {
        if (fluid == null) {
            return;
        }

        playSound(player, FluidVariantAttributes.getFillSound(FabricResources.toVariant(fluid)));
    }

    public static void playEmptySound(Player player, @Nullable AEFluidKey fluid) {
        if (fluid == null) {
            return;
        }

        playSound(player, FluidVariantAttributes.getEmptySound(FabricResources.toVariant(fluid)));
    }

    private static void playSound(Player player, SoundEvent fillSound) {
        // TODO 1.21.11: This now plays it for everyone (kept in sync with the NeoForge twin).
        player.playSound(fillSound);
    }
}
