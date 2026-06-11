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

package appeng.menu.implementations;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;

/**
 * Loader-neutral seam for menus that transmit additional data from server to client when they are opened (NeoForge:
 * {@code IMenuTypeExtension}/{@code IPlayerExtension#openMenu}; Fabric: extended screen handlers). The loader-specific
 * implementation (e.g. {@code appeng.neoforge.menu.NeoForgeMenuTypePlatform}) is injected once during mod construction
 * via {@link #init}.
 */
public interface MenuTypePlatform {
    /**
     * Creates a menu type whose client-side menus are constructed from additional data sent by the server.
     */
    <M extends AbstractContainerMenu> MenuType<M> createMenuType(MenuFromNetworkFactory<M> factory);

    /**
     * Opens a menu for the given player, sending the data written by {@code initialDataWriter} along to the client
     * (where it is consumed by the {@link MenuFromNetworkFactory} of the menu type). When the player is currently in an
     * AE2 menu, no client-side container close is triggered (to allow seamless switching between AE2 menus).
     */
    void openMenu(ServerPlayer player, Component title, MenuConstructor menuConstructor,
            Consumer<RegistryFriendlyByteBuf> initialDataWriter);

    /**
     * Constructs the client-side menu from the additional data sent by the server.
     */
    @FunctionalInterface
    interface MenuFromNetworkFactory<M extends AbstractContainerMenu> {
        M create(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData);
    }

    static MenuTypePlatform get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific MenuTypePlatform has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(MenuTypePlatform platform) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The MenuTypePlatform has already been initialized");
        }
        Holder.INSTANCE = platform;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile MenuTypePlatform INSTANCE;

        private Holder() {
        }
    }
}
