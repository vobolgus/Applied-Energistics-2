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

package appeng.fabric.menu;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.menu.v1.FabricMenuProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypePlatform;

/**
 * Fabric implementation of the {@link MenuTypePlatform} seam, based on {@link ExtendedMenuType} and
 * {@link ExtendedMenuProvider}. The extra menu data stays an opaque {@link RegistryFriendlyByteBuf} (matching AE2's
 * buffer-writer menu contract), transported through a raw-bytes stream codec.
 */
public class FabricMenuTypePlatform implements MenuTypePlatform {
    /**
     * Transports the AE2 menu-opening buffer as raw bytes.
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, RegistryFriendlyByteBuf> BUFFER_CODEC = StreamCodec.of(
            (buffer, data) -> {
                buffer.writeVarInt(data.readableBytes());
                buffer.writeBytes(data, data.readerIndex(), data.readableBytes());
            },
            buffer -> {
                int length = buffer.readVarInt();
                var data = new RegistryFriendlyByteBuf(buffer.readBytes(length), buffer.registryAccess());
                return data;
            });

    @Override
    public <M extends AbstractContainerMenu> MenuType<M> createMenuType(MenuFromNetworkFactory<M> factory) {
        return new ExtendedMenuType<>(factory::create, BUFFER_CODEC);
    }

    @Override
    public void openMenu(ServerPlayer player, Component title, MenuConstructor menuConstructor,
            Consumer<RegistryFriendlyByteBuf> initialDataWriter) {
        class AppEngMenuProvider implements ExtendedMenuProvider<RegistryFriendlyByteBuf>, FabricMenuProvider {
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
            public RegistryFriendlyByteBuf getScreenOpeningData(ServerPlayer serverPlayer) {
                var data = new RegistryFriendlyByteBuf(Unpooled.buffer(), serverPlayer.registryAccess());
                initialDataWriter.accept(data);
                return data;
            }

            @Override
            public boolean shouldCloseCurrentScreen() {
                // Do not send close packets when switching between AE menus
                // (mirrors NeoForge's shouldTriggerClientSideContainerClosingOnOpen)
                return !(player.containerMenu instanceof AEBaseMenu);
            }
        }

        player.openMenu(new AppEngMenuProvider());
    }
}
