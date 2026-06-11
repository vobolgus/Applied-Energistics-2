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

package appeng.fabric.init;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;

import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.InfiniteEnergyStorage;

import appeng.api.AECapabilities;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartApiRegistry;
import appeng.api.storage.MEStorage;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.blockentity.misc.ChargerBlockEntity;
import appeng.blockentity.misc.GrowthAcceleratorBlockEntity;
import appeng.blockentity.misc.InscriberBlockEntity;
import appeng.blockentity.powersink.AEBasePoweredBlockEntity;
import appeng.blockentity.storage.MEChestBlockEntity;
import appeng.blockentity.storage.MEChestFluidStorage;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.fabric.transfer.CondenserFluidStorage;
import appeng.fabric.transfer.FabricResources;
import appeng.fabric.transfer.GenericStackFluidStorage;
import appeng.fabric.transfer.GenericStackItemStorage;
import appeng.fabric.transfer.PoweredItemEnergyStorage;
import appeng.fabric.transfer.SkyStoneTankFluidStorage;
import appeng.fabric.transfer.TrEnergyAdapter;
import appeng.parts.crafting.PatternProviderPart;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import appeng.parts.misc.InterfacePart;
import appeng.parts.networking.EnergyAcceptorPart;
import appeng.parts.p2p.FEP2PTunnelPart;
import appeng.parts.p2p.FluidP2PTunnelPart;
import appeng.parts.p2p.ItemP2PTunnelPart;

/**
 * Registers AE2's block/item API providers with the Fabric API lookup system. The Fabric twin of
 * {@code appeng.init.InitCapabilityProviders}, including the part-API forwarding through the shared
 * {@link PartApiRegistry} (which NeoForge drives via {@code RegisterPartCapabilitiesEvent}).
 * <p>
 * Must be called after registration (block entity types resolved).
 */
public final class InitApiLookup {

    /**
     * The Fabric lookups behind the {@link AECapabilities} ids. {@code BlockApiLookup.get} is get-or-create, so these
     * are the same instances the {@code AEApiLookup} seam resolves.
     */
    public static final BlockApiLookup<MEStorage, Direction> ME_STORAGE = BlockApiLookup
            .get(AECapabilities.ME_STORAGE_ID, MEStorage.class, Direction.class);
    public static final BlockApiLookup<ICraftingMachine, Direction> CRAFTING_MACHINE = BlockApiLookup
            .get(AECapabilities.CRAFTING_MACHINE_ID, ICraftingMachine.class, Direction.class);
    public static final BlockApiLookup<GenericInternalInventory, Direction> GENERIC_INTERNAL_INV = BlockApiLookup
            .get(AECapabilities.GENERIC_INTERNAL_INV_ID, GenericInternalInventory.class, Direction.class);
    public static final BlockApiLookup<IInWorldGridNodeHost, Void> IN_WORLD_GRID_NODE_HOST = BlockApiLookup
            .get(AECapabilities.IN_WORLD_GRID_NODE_HOST_ID, IInWorldGridNodeHost.class, Void.class);
    public static final BlockApiLookup<ICrankable, Direction> CRANKABLE = BlockApiLookup
            .get(AECapabilities.CRANKABLE_ID, ICrankable.class, Direction.class);

    private InitApiLookup() {
    }

    public static void init() {
        var partRegistry = new PartApiRegistry();
        partRegistry.addHostType(AEBlockEntities.CABLE_BUS.get());
        registerPartApis(partRegistry);
        // TODO (fabric): expose the part registry for addon part-API registrations (NeoForge addons use
        // RegisterPartCapabilitiesEvent). Needs a Fabric-facing entrypoint in a later step.
        registerPartForwarding(partRegistry);

        initInterface();
        initPatternProvider();
        initCondenser();
        initMEChest();
        initMisc();
        initPoweredItems();
        initCrankable();

        for (var type : AEBlockEntities.getSubclassesOf(AEBaseInvBlockEntity.class)) {
            ItemStorage.SIDED.registerForBlockEntities(InitApiLookup::getExposedItemStorage, type);
        }
        for (var type : AEBlockEntities.getSubclassesOf(AEBasePoweredBlockEntity.class)) {
            EnergyStorage.SIDED.registerForBlockEntities(InitApiLookup::getEnergyStorage, type);
        }
        for (var type : AEBlockEntities.getImplementorsOf(IInWorldGridNodeHost.class)) {
            IN_WORLD_GRID_NODE_HOST.registerForBlockEntities(
                    (blockEntity, context) -> (IInWorldGridNodeHost) blockEntity, type);
        }

        registerGenericAdapters();
    }

    private static Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> getExposedItemStorage(
            BlockEntity blockEntity, Direction side) {
        var exposed = ((AEBaseInvBlockEntity) blockEntity).getExposedInventory(side);
        return exposed == null ? null : FabricResources.toStorage(exposed);
    }

    private static EnergyStorage getEnergyStorage(BlockEntity blockEntity, Direction side) {
        var powered = (AEBasePoweredBlockEntity) blockEntity;
        if (!powered.isExternalPowerSide(side)) {
            return null;
        }
        return (EnergyStorage) powered.getOrCreateEnergyAdapter(TrEnergyAdapter::new);
    }

    /**
     * Exposes {@link GenericInternalInventory}s additionally as item/fluid storages. NeoForge registers these adapters
     * per-block with lowest priority; on Fabric a fallback provider covers the same blocks (it only fires when no more
     * specific provider returned an API).
     */
    private static void registerGenericAdapters() {
        ItemStorage.SIDED.registerFallback((level, pos, state, blockEntity, context) -> {
            var genericInv = GENERIC_INTERNAL_INV.find(level, pos, state, blockEntity, context);
            if (genericInv != null) {
                return new GenericStackItemStorage(genericInv);
            }
            return null;
        });
        FluidStorage.SIDED.registerFallback((level, pos, state, blockEntity, context) -> {
            var genericInv = GENERIC_INTERNAL_INV.find(level, pos, state, blockEntity, context);
            if (genericInv != null) {
                return new GenericStackFluidStorage(genericInv);
            }
            return null;
        });
    }

    private static void initInterface() {
        GENERIC_INTERNAL_INV.registerForBlockEntity(
                (blockEntity, context) -> blockEntity.getInterfaceLogic().getStorage(),
                AEBlockEntities.INTERFACE.get());

        ME_STORAGE.registerForBlockEntity(
                (blockEntity, context) -> blockEntity.getInterfaceLogic().getInventory(),
                AEBlockEntities.INTERFACE.get());
    }

    private static void initPatternProvider() {
        GENERIC_INTERNAL_INV.registerForBlockEntity(
                (blockEntity, context) -> blockEntity.getLogic().getReturnInv(),
                AEBlockEntities.PATTERN_PROVIDER.get());
    }

    private static void initCondenser() {
        // Condenser will always return its external inventory, even when context is null
        ItemStorage.SIDED.registerForBlockEntity(
                (blockEntity, context) -> FabricResources.toStorage(blockEntity.getExternalInv()),
                AEBlockEntities.CONDENSER.get());
        FluidStorage.SIDED.registerForBlockEntity(
                (blockEntity, context) -> new CondenserFluidStorage(blockEntity),
                AEBlockEntities.CONDENSER.get());
        ME_STORAGE.registerForBlockEntity(
                (blockEntity, context) -> blockEntity.getMEStorage(),
                AEBlockEntities.CONDENSER.get());
    }

    private static void initMEChest() {
        FluidStorage.SIDED.registerForBlockEntity(InitApiLookup::getMEChestFluidStorage,
                AEBlockEntities.ME_CHEST.get());
        ME_STORAGE.registerForBlockEntity(
                (blockEntity, context) -> blockEntity.getMEStorage(context),
                AEBlockEntities.ME_CHEST.get());
    }

    @SuppressWarnings("unchecked")
    private static Storage<FluidVariant> getMEChestFluidStorage(MEChestBlockEntity blockEntity, Direction side) {
        return (Storage<FluidVariant>) blockEntity.getFluidHandler(side, MEChestFluidStorage::new);
    }

    private static void initMisc() {
        CRAFTING_MACHINE.registerForBlockEntity(
                (blockEntity, context) -> blockEntity,
                AEBlockEntities.MOLECULAR_ASSEMBLER.get());
        ItemStorage.SIDED.registerForBlockEntity(
                (blockEntity, context) -> FabricResources.toStorage(blockEntity.getInventory()),
                AEBlockEntities.DEBUG_ITEM_GEN.get());
        EnergyStorage.SIDED.registerForBlockEntity(
                // The debug energy generator is an infinite source that accepts no insertion
                (blockEntity, context) -> InfiniteEnergyStorage.INSTANCE,
                AEBlockEntities.DEBUG_ENERGY_GEN.get());
        FluidStorage.SIDED.registerForBlockEntity(
                (blockEntity, context) -> SkyStoneTankFluidStorage.get(blockEntity),
                AEBlockEntities.SKY_STONE_TANK.get());
    }

    private static void initPoweredItems() {
        registerPowerStorageItem(AEItems.ENTROPY_MANIPULATOR);
        registerPowerStorageItem(AEItems.CHARGED_STAFF);
        registerPowerStorageItem(AEItems.COLOR_APPLICATOR);
        registerPowerStorageItem(AEItems.PORTABLE_ITEM_CELL1K);
        registerPowerStorageItem(AEItems.PORTABLE_ITEM_CELL4K);
        registerPowerStorageItem(AEItems.PORTABLE_ITEM_CELL16K);
        registerPowerStorageItem(AEItems.PORTABLE_ITEM_CELL64K);
        registerPowerStorageItem(AEItems.PORTABLE_ITEM_CELL256K);
        registerPowerStorageItem(AEItems.PORTABLE_FLUID_CELL1K);
        registerPowerStorageItem(AEItems.PORTABLE_FLUID_CELL4K);
        registerPowerStorageItem(AEItems.PORTABLE_FLUID_CELL16K);
        registerPowerStorageItem(AEItems.PORTABLE_FLUID_CELL64K);
        registerPowerStorageItem(AEItems.PORTABLE_FLUID_CELL256K);
        registerPowerStorageItem(AEItems.MATTER_CANNON);
        registerPowerStorageItem(AEItems.WIRELESS_TERMINAL);
        registerPowerStorageItem(AEItems.WIRELESS_CRAFTING_TERMINAL);
    }

    private static <T extends Item & IAEItemPowerStorage> void registerPowerStorageItem(ItemDefinition<T> definition) {
        IAEItemPowerStorage powerStorage = definition.get();

        EnergyStorage.ITEM.registerForItems(
                (stack, context) -> new PoweredItemEnergyStorage(context, definition.asItem(), powerStorage),
                definition);
    }

    private static void initCrankable() {
        CRANKABLE.registerForBlockEntity(ChargerBlockEntity::getCrankable, AEBlockEntities.CHARGER.get());
        CRANKABLE.registerForBlockEntity(InscriberBlockEntity::getCrankable, AEBlockEntities.INSCRIBER.get());
        CRANKABLE.registerForBlockEntity(GrowthAcceleratorBlockEntity::getCrankable,
                AEBlockEntities.GROWTH_ACCELERATOR.get());
    }

    private static void registerPartApis(PartApiRegistry registry) {
        registry.register(ItemStorage.SIDED,
                (PatternEncodingTerminalPart part, Direction direction) -> FabricResources
                        .toStorage(part.getLogic().getBlankPatternInv()),
                PatternEncodingTerminalPart.class);
        registry.register(GENERIC_INTERNAL_INV,
                (PatternProviderPart part, Direction context) -> part.getLogic().getReturnInv(),
                PatternProviderPart.class);
        registry.register(GENERIC_INTERNAL_INV,
                (InterfacePart part, Direction context) -> part.getInterfaceLogic().getStorage(),
                InterfacePart.class);
        registry.register(ME_STORAGE,
                (InterfacePart part, Direction context) -> part.getInterfaceLogic().getInventory(),
                InterfacePart.class);

        registry.register(ItemStorage.SIDED,
                (ItemP2PTunnelPart part, Direction context) -> part.getExposedApi(),
                ItemP2PTunnelPart.class);
        registry.register(EnergyStorage.SIDED,
                (FEP2PTunnelPart part, Direction context) -> part.getExposedApi(),
                FEP2PTunnelPart.class);
        registry.register(FluidStorage.SIDED,
                (FluidP2PTunnelPart part, Direction context) -> part.getExposedApi(),
                FluidP2PTunnelPart.class);

        registry.register(EnergyStorage.SIDED,
                (EnergyAcceptorPart part, Direction context) -> (EnergyStorage) part
                        .getOrCreateEnergyAdapter(TrEnergyAdapter::new),
                EnergyAcceptorPart.class);
    }

    /**
     * Registers every part-API registration with the Fabric lookups for all part host types. The Fabric twin of
     * {@code RegisterPartCapabilitiesEventInternal}.
     */
    private static void registerPartForwarding(PartApiRegistry registry) {
        for (var registration : registry.getRegistrations()) {
            register(registration, registry);
        }
    }

    @SuppressWarnings("unchecked")
    private static <A, C> void register(PartApiRegistry.Registration<A, C> registration, PartApiRegistry registry) {
        var lookup = (BlockApiLookup<A, C>) registration.capability();
        for (var hostType : registry.getHostTypes()) {
            lookup.registerForBlockEntities(
                    (blockEntity, context) -> blockEntity instanceof IPartHost partHost
                            ? registration.find(partHost, context)
                            : null,
                    hostType);
        }
    }
}
