package appeng.neoforge.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import appeng.core.AppEng;
import appeng.core.network.InitNetwork;
import appeng.core.network.PacketHandlingContext;
import appeng.core.network.ServerboundPacket;

/**
 * Registers the payloads enumerated by the loader-neutral manifest in {@link InitNetwork} with NeoForge's networking
 * API.
 * <p>
 * Clientbound payload handlers are registered separately via NeoForge's {@code RegisterClientPayloadHandlersEvent} (see
 * {@code appeng.client.AEClientboundPacketHandler}).
 */
public final class NeoForgeNetworkInit {
    private NeoForgeNetworkInit() {
    }

    public static void init(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(AppEng.MOD_ID);

        InitNetwork.forEachPayload(entry -> register(registrar, entry));
    }

    private static <T extends CustomPacketPayload> void register(PayloadRegistrar registrar,
            InitNetwork.PayloadEntry<T> entry) {
        switch (entry.direction()) {
            case CLIENTBOUND -> registrar.playToClient(entry.type(), entry.codec());
            case SERVERBOUND -> registrar.playToServer(entry.type(), entry.codec(),
                    NeoForgeNetworkInit::handleOnServer);
            case BIDIRECTIONAL -> registrar.playBidirectional(entry.type(), entry.codec(),
                    NeoForgeNetworkInit::handleOnServer);
        }
    }

    private static void handleOnServer(CustomPacketPayload payload, IPayloadContext context) {
        ((ServerboundPacket) payload).handleOnServer(adapt(context));
    }

    /**
     * Adapts NeoForge's {@link IPayloadContext} to the loader-neutral {@link PacketHandlingContext}.
     */
    private static PacketHandlingContext adapt(IPayloadContext context) {
        return new PacketHandlingContext() {
            @Override
            public Player player() {
                return context.player();
            }

            @Override
            public void enqueueWork(Runnable task) {
                context.enqueueWork(task);
            }
        };
    }
}
