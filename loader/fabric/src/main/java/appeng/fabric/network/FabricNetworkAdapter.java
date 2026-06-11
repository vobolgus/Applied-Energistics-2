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

package appeng.fabric.network;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.NetworkAdapter;
import appeng.core.network.ServerboundPacket;

/**
 * Fabric implementation of the {@link NetworkAdapter} seam.
 * <p>
 * Server→client sends go through {@link ServerPlayNetworking}. Client→server sends must go through Fabric's client-only
 * {@code ClientPlayNetworking} class, which cannot be referenced from the main (server-safe) source set; the client
 * entrypoint injects a sender via {@link #setClientPacketSender} in Phase 3.
 */
public class FabricNetworkAdapter implements NetworkAdapter {
    @Nullable
    private static volatile Consumer<ServerboundPacket> clientPacketSender;

    /**
     * Injected by the client entrypoint (Phase 3); lives here so the main source set stays free of client classes.
     */
    public static void setClientPacketSender(Consumer<ServerboundPacket> sender) {
        clientPacketSender = sender;
    }

    @Override
    public void sendToServer(ServerboundPacket payload) {
        var sender = clientPacketSender;
        if (sender == null) {
            throw new IllegalStateException(
                    "Cannot send a serverbound payload: the AE2 Fabric client networking has not been initialized");
        }
        sender.accept(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ClientboundPacket payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z,
            double radius, ClientboundPacket payload) {
        // Mirrors NeoForge's PacketDistributor.sendToPlayersNear.
        var radiusSquared = radius * radius;
        for (var player : level.players()) {
            if (player != excluded && player.distanceToSqr(x, y, z) < radiusSquared) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}
