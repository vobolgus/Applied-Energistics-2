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

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import appeng.core.network.InitNetwork;
import appeng.core.network.PacketHandlingContext;
import appeng.core.network.ServerboundPacket;

/**
 * Registers AE2's payload types and the serverbound payload receivers with the Fabric networking API. The Fabric twin
 * of {@code appeng.neoforge.network.NeoForgeNetworkInit}. Clientbound payload receivers are registered by the client
 * entrypoint (Phase 3).
 */
public final class FabricNetworkInit {
    private FabricNetworkInit() {
    }

    public static void init() {
        InitNetwork.forEachPayload(FabricNetworkInit::register);
    }

    private static <T extends CustomPacketPayload> void register(InitNetwork.PayloadEntry<T> entry) {
        switch (entry.direction()) {
            case CLIENTBOUND -> PayloadTypeRegistry.clientboundPlay().register(entry.type(), entry.codec());
            case SERVERBOUND -> {
                PayloadTypeRegistry.serverboundPlay().register(entry.type(), entry.codec());
                registerServerReceiver(entry.type());
            }
            case BIDIRECTIONAL -> {
                PayloadTypeRegistry.clientboundPlay().register(entry.type(), entry.codec());
                PayloadTypeRegistry.serverboundPlay().register(entry.type(), entry.codec());
                registerServerReceiver(entry.type());
            }
        }
    }

    private static <T extends CustomPacketPayload> void registerServerReceiver(CustomPacketPayload.Type<T> type) {
        ServerPlayNetworking.registerGlobalReceiver(type,
                (payload, context) -> ((ServerboundPacket) payload).handleOnServer(adapt(context)));
    }

    /**
     * Adapts Fabric's networking context to the loader-neutral {@link PacketHandlingContext}. Fabric play payload
     * handlers already run on the server thread, so {@code enqueueWork} can run the task directly (mirroring NeoForge's
     * main-thread {@code IPayloadContext.enqueueWork}).
     */
    private static PacketHandlingContext adapt(ServerPlayNetworking.Context context) {
        return new PacketHandlingContext() {
            @Override
            public Player player() {
                return context.player();
            }

            @Override
            public void enqueueWork(Runnable task) {
                if (context.server().isSameThread()) {
                    task.run();
                } else {
                    context.server().execute(task);
                }
            }
        };
    }
}
