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

package appeng.fabric.fluids;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;

import appeng.blockentity.storage.SkyStoneTankBlockEntity;
import appeng.fabric.transfer.SkyStoneTankFluidStorage;
import appeng.util.fluid.FluidPlatform;

/**
 * Fabric implementation of the {@link FluidPlatform} seam, based on {@link FluidVariantAttributes}.
 */
public class FabricFluidPlatform implements FluidPlatform {
    @Override
    public Component getFluidDisplayName(Fluid fluid, DataComponentPatch components) {
        return FluidVariantAttributes.getName(FluidVariant.of(fluid, components));
    }

    @Override
    public boolean interactWithTank(Player player, InteractionHand hand, SkyStoneTankBlockEntity tank) {
        return FluidStorageUtil.interactWithFluidStorage(SkyStoneTankFluidStorage.get(tank), player, hand);
    }

    @Override
    @Nullable
    public SoundEvent getBucketFillSound(Fluid fluid) {
        return FluidVariantAttributes.getFillSound(FluidVariant.of(fluid));
    }
}
