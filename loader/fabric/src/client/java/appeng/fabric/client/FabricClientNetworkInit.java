package appeng.fabric.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import appeng.client.AEClientboundPacketHandler;
import appeng.core.network.ClientboundPacket;

/**
 * Registers the shared {@link AEClientboundPacketHandler} handlers with Fabric's client play networking (which invokes
 * them on the client main thread, like NeoForge's client payload handler registry). Fabric twin of
 * {@code appeng.neoforge.client.NeoForgeClientNetworkInit}.
 */
public final class FabricClientNetworkInit {
    private FabricClientNetworkInit() {
    }

    public static void init() {
        new AEClientboundPacketHandler().registerAll(new AEClientboundPacketHandler.Registrar() {
            @Override
            public <T extends ClientboundPacket> void register(CustomPacketPayload.Type<T> type,
                    AEClientboundPacketHandler.ClientPacketHandler<T> handler) {
                ClientPlayNetworking.registerGlobalReceiver(type,
                        (payload, context) -> handler.handle(payload, Minecraft.getInstance(), context.player()));
            }
        });
    }
}
