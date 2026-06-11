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

package appeng.api;

import net.minecraft.resources.Identifier;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.lookup.AEApiLookup;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;
import appeng.core.AppEng;

/**
 * Utility class that holds the loader-neutral ids and {@linkplain AEApiLookup lookups} of the block capabilities/APIs
 * provided by AE2.
 * <p>
 * The loader-specific capability objects are built from these ids:
 * <ul>
 * <li>On NeoForge, {@code appeng.neoforge.AENeoForgeCapabilities} exposes them as {@code BlockCapability} objects
 * (which back the lookups in this class).</li>
 * <li>On Fabric, the analog will be {@code BlockApiLookup}s created from the same ids.</li>
 * </ul>
 */
public final class AECapabilities {
    private AECapabilities() {
    }

    /**
     * Id of the sided block capability exposing {@link appeng.api.storage.MEStorage}.
     */
    public static final Identifier ME_STORAGE_ID = AppEng.makeId("me_storage");

    /**
     * Id of the sided block capability exposing {@link appeng.api.implementations.blockentities.ICraftingMachine}.
     */
    public static final Identifier CRAFTING_MACHINE_ID = AppEng.makeId("crafting_machine");

    /**
     * Id of the sided block capability exposing {@link appeng.api.behaviors.GenericInternalInventory}.
     */
    public static final Identifier GENERIC_INTERNAL_INV_ID = AppEng.makeId("generic_internal_inv");

    /**
     * Id of the (unsided) block capability exposing {@link appeng.api.networking.IInWorldGridNodeHost}.
     */
    public static final Identifier IN_WORLD_GRID_NODE_HOST_ID = AppEng.makeId("inworld_gridnode_host");

    /**
     * Id of the sided block capability exposing {@link appeng.api.implementations.blockentities.ICrankable}.
     */
    public static final Identifier CRANKABLE_ID = AppEng.makeId("crankable");

    /**
     * Lookup for the sided block API exposing {@link MEStorage}.
     */
    public static final AEApiLookup<MEStorage> ME_STORAGE = AEApiLookup.sided(ME_STORAGE_ID, MEStorage.class);

    /**
     * Lookup for the sided block API exposing {@link ICraftingMachine}.
     */
    public static final AEApiLookup<ICraftingMachine> CRAFTING_MACHINE = AEApiLookup.sided(CRAFTING_MACHINE_ID,
            ICraftingMachine.class);

    /**
     * Lookup for the sided block API exposing {@link GenericInternalInventory}.
     */
    public static final AEApiLookup<GenericInternalInventory> GENERIC_INTERNAL_INV = AEApiLookup
            .sided(GENERIC_INTERNAL_INV_ID, GenericInternalInventory.class);

    /**
     * Lookup for the (unsided) block API exposing {@link IInWorldGridNodeHost}.
     */
    public static final AEApiLookup<IInWorldGridNodeHost> IN_WORLD_GRID_NODE_HOST = AEApiLookup
            .unsided(IN_WORLD_GRID_NODE_HOST_ID, IInWorldGridNodeHost.class);

    /**
     * Lookup for the sided block API exposing {@link ICrankable}.
     */
    public static final AEApiLookup<ICrankable> CRANKABLE = AEApiLookup.sided(CRANKABLE_ID, ICrankable.class);
}
