package appeng.core.network;

import net.minecraft.world.entity.player.Player;

/**
 * Loader-neutral subset of the payload handling context offered by the loader's networking API (NeoForge's
 * {@code IPayloadContext}, Fabric's {@code ServerPlayNetworking.Context}). Only contains what AE2 packet handlers
 * actually use.
 */
public interface PacketHandlingContext {
    /**
     * The player relevant to this payload: the sending player for serverbound payloads, the receiving player for
     * clientbound payloads.
     */
    Player player();

    /**
     * Submits the given task to be run on the main thread of the game, or runs it immediately if the handler already
     * runs on the main thread.
     */
    void enqueueWork(Runnable task);
}
