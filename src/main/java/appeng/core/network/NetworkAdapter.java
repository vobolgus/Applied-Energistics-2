package appeng.core.network;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loader-neutral seam for sending AE2's custom payloads. The loader-specific implementation (e.g.
 * {@code appeng.neoforge.network.NeoForgeNetworkAdapter}) is injected once during mod construction via {@link #init}.
 * <p>
 * Only contains the send operations actually used by AE2.
 */
public interface NetworkAdapter {
    /**
     * Sends the given payload from the client to the server. Must only be called on the client while connected.
     */
    void sendToServer(ServerboundPacket payload);

    /**
     * Sends the given payload to the given player.
     */
    void sendToPlayer(ServerPlayer player, ClientboundPacket payload);

    /**
     * Sends the given payload to all players within the given radius around the given coordinates, optionally excluding
     * one player.
     */
    void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z,
            double radius, ClientboundPacket payload);

    static NetworkAdapter get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific NetworkAdapter has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(NetworkAdapter adapter) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The NetworkAdapter has already been initialized");
        }
        Holder.INSTANCE = adapter;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile NetworkAdapter INSTANCE;

        private Holder() {
        }
    }
}
