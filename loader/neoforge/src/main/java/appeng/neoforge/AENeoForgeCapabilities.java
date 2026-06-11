/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.neoforge;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;

import appeng.api.AECapabilities;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;

/**
 * The NeoForge {@link BlockCapability}s provided by AE2, built from the loader-neutral ids in {@link AECapabilities}.
 */
public final class AENeoForgeCapabilities {
    private AENeoForgeCapabilities() {
    }

    public static final BlockCapability<MEStorage, @Nullable Direction> ME_STORAGE = BlockCapability
            .createSided(AECapabilities.ME_STORAGE_ID, MEStorage.class);

    public static final BlockCapability<ICraftingMachine, @Nullable Direction> CRAFTING_MACHINE = BlockCapability
            .createSided(AECapabilities.CRAFTING_MACHINE_ID, ICraftingMachine.class);

    public static final BlockCapability<GenericInternalInventory, @Nullable Direction> GENERIC_INTERNAL_INV = BlockCapability
            .createSided(AECapabilities.GENERIC_INTERNAL_INV_ID, GenericInternalInventory.class);

    public static final BlockCapability<IInWorldGridNodeHost, Void> IN_WORLD_GRID_NODE_HOST = BlockCapability
            .createVoid(AECapabilities.IN_WORLD_GRID_NODE_HOST_ID, IInWorldGridNodeHost.class);

    public static final BlockCapability<ICrankable, @Nullable Direction> CRANKABLE = BlockCapability
            .createSided(AECapabilities.CRANKABLE_ID, ICrankable.class);
}
