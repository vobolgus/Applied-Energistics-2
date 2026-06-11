package appeng.client;

import net.minecraft.client.KeyMapping;

import appeng.core.network.NetworkAdapter;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.HotkeyPacket;

public record Hotkey(String name, KeyMapping mapping) {
    public void check() {
        while (mapping().consumeClick()) {
            ServerboundPacket message = new HotkeyPacket(this);
            NetworkAdapter.get().sendToServer(message);
        }
    }
}
