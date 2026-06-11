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

package appeng.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import appeng.client.AEClientboundPacketHandler;
import appeng.core.network.ClientboundPacket;

/**
 * Registers the shared {@link AEClientboundPacketHandler} handlers with NeoForge's client payload handler registry
 * (which invokes them on the client main thread).
 */
public final class NeoForgeClientNetworkInit {
    private NeoForgeClientNetworkInit() {
    }

    public static void init(RegisterClientPayloadHandlersEvent event) {
        new AEClientboundPacketHandler().registerAll(new AEClientboundPacketHandler.Registrar() {
            @Override
            public <T extends ClientboundPacket> void register(CustomPacketPayload.Type<T> type,
                    AEClientboundPacketHandler.ClientPacketHandler<T> handler) {
                event.register(type,
                        (payload, context) -> handler.handle(payload, Minecraft.getInstance(), context.player()));
            }
        });
    }
}
