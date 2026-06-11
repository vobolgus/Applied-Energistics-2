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

package appeng.neoforge.menu;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypePlatform;

/**
 * NeoForge implementation of the {@link MenuTypePlatform} seam, based on {@link IMenuTypeExtension} and the NeoForge
 * {@code openMenu} extension with an extra-data writer.
 */
public class NeoForgeMenuTypePlatform implements MenuTypePlatform {
    @Override
    public <M extends AbstractContainerMenu> MenuType<M> createMenuType(MenuFromNetworkFactory<M> factory) {
        return IMenuTypeExtension.create(factory::create);
    }

    @Override
    public void openMenu(ServerPlayer player, Component title, MenuConstructor menuConstructor,
            Consumer<RegistryFriendlyByteBuf> initialDataWriter) {
        class AppEngMenuProvider implements MenuProvider {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int wnd, Inventory inventory, Player p) {
                return menuConstructor.createMenu(wnd, inventory, p);
            }

            @Override
            public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                // Do not send close packets when switching between AE menus
                return !(player.containerMenu instanceof AEBaseMenu);
            }
        }

        player.openMenu(new AppEngMenuProvider(), initialDataWriter);
    }
}
