package appeng.neoforge.network;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.NetworkAdapter;
import appeng.core.network.ServerboundPacket;

/**
 * {@link NetworkAdapter} implementation backed by NeoForge's {@link PacketDistributor} and
 * {@link ClientPacketDistributor}.
 */
public final class NeoForgeNetworkAdapter implements NetworkAdapter {
    @Override
    public void sendToServer(ServerboundPacket payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ClientboundPacket payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z,
            double radius, ClientboundPacket payload) {
        PacketDistributor.sendToPlayersNear(level, excluded, x, y, z, radius, payload);
    }
}
