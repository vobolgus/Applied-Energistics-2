package appeng.core.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.core.network.clientbound.BlockTransitionEffectPacket;
import appeng.core.network.clientbound.ClearPatternAccessTerminalPacket;
import appeng.core.network.clientbound.CompassResponsePacket;
import appeng.core.network.clientbound.CraftConfirmPlanPacket;
import appeng.core.network.clientbound.CraftingJobStatusPacket;
import appeng.core.network.clientbound.CraftingStatusPacket;
import appeng.core.network.clientbound.ExportedGridContent;
import appeng.core.network.clientbound.GuiDataSyncPacket;
import appeng.core.network.clientbound.ItemTransitionEffectPacket;
import appeng.core.network.clientbound.MEInventoryUpdatePacket;
import appeng.core.network.clientbound.MatterCannonPacket;
import appeng.core.network.clientbound.MockExplosionPacket;
import appeng.core.network.clientbound.MolecularAssemblerAnimationPacket;
import appeng.core.network.clientbound.NetworkStatusPacket;
import appeng.core.network.clientbound.PatternAccessTerminalPacket;
import appeng.core.network.clientbound.SetLinkStatusPacket;
import appeng.core.network.serverbound.ColorApplicatorSelectColorPacket;
import appeng.core.network.serverbound.ConfigButtonPacket;
import appeng.core.network.serverbound.ConfirmAutoCraftPacket;
import appeng.core.network.serverbound.FillCraftingGridFromRecipePacket;
import appeng.core.network.serverbound.GuiActionPacket;
import appeng.core.network.serverbound.HotkeyPacket;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.core.network.serverbound.MEInteractionPacket;
import appeng.core.network.serverbound.MouseWheelPacket;
import appeng.core.network.serverbound.PartLeftClickPacket;
import appeng.core.network.serverbound.QuickMovePatternPacket;
import appeng.core.network.serverbound.RequestClosestMeteoritePacket;
import appeng.core.network.serverbound.SelectKeyTypePacket;
import appeng.core.network.serverbound.SwapSlotsPacket;
import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.core.network.serverbound.UpdateHoldingCtrlPacket;

/**
 * Loader-neutral manifest of all custom payloads registered by AE2. The actual registration with the loader's
 * networking API is performed by loader-specific code (e.g. {@code appeng.neoforge.network.NeoForgeNetworkInit}), which
 * iterates this manifest via {@link #forEachPayload}.
 */
public class InitNetwork {
    /**
     * The direction a payload is sent in.
     */
    public enum PayloadDirection {
        /**
         * Server -> client. Handled by the client-side payload handler registration.
         */
        CLIENTBOUND,
        /**
         * Client -> server. Handled via {@link ServerboundPacket#handleOnServer(PacketHandlingContext)}.
         */
        SERVERBOUND,
        /**
         * Both directions. The payload implements both {@link ClientboundPacket} and {@link ServerboundPacket}.
         */
        BIDIRECTIONAL
    }

    /**
     * A single payload registration: direction, vanilla payload type and vanilla stream codec.
     */
    public record PayloadEntry<T extends CustomPacketPayload>(
            PayloadDirection direction,
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
    }

    private static final List<PayloadEntry<?>> PAYLOADS = buildPayloads();

    private static List<PayloadEntry<?>> buildPayloads() {
        var payloads = new ArrayList<PayloadEntry<?>>();

        // Clientbound
        clientbound(payloads, MolecularAssemblerAnimationPacket.TYPE, MolecularAssemblerAnimationPacket.STREAM_CODEC);
        clientbound(payloads, BlockTransitionEffectPacket.TYPE, BlockTransitionEffectPacket.STREAM_CODEC);
        clientbound(payloads, ClearPatternAccessTerminalPacket.TYPE, ClearPatternAccessTerminalPacket.STREAM_CODEC);
        clientbound(payloads, CompassResponsePacket.TYPE, CompassResponsePacket.STREAM_CODEC);
        clientbound(payloads, CraftConfirmPlanPacket.TYPE, CraftConfirmPlanPacket.STREAM_CODEC);
        clientbound(payloads, CraftingJobStatusPacket.TYPE, CraftingJobStatusPacket.STREAM_CODEC);
        clientbound(payloads, CraftingStatusPacket.TYPE, CraftingStatusPacket.STREAM_CODEC);
        clientbound(payloads, GuiDataSyncPacket.TYPE, GuiDataSyncPacket.STREAM_CODEC);
        clientbound(payloads, ItemTransitionEffectPacket.TYPE, ItemTransitionEffectPacket.STREAM_CODEC);
        clientbound(payloads, MatterCannonPacket.TYPE, MatterCannonPacket.STREAM_CODEC);
        clientbound(payloads, MEInventoryUpdatePacket.TYPE, MEInventoryUpdatePacket.STREAM_CODEC);
        clientbound(payloads, MockExplosionPacket.TYPE, MockExplosionPacket.STREAM_CODEC);
        clientbound(payloads, NetworkStatusPacket.TYPE, NetworkStatusPacket.STREAM_CODEC);
        clientbound(payloads, PatternAccessTerminalPacket.TYPE, PatternAccessTerminalPacket.STREAM_CODEC);
        clientbound(payloads, SetLinkStatusPacket.TYPE, SetLinkStatusPacket.STREAM_CODEC);
        clientbound(payloads, ExportedGridContent.TYPE, ExportedGridContent.STREAM_CODEC);

        // Serverbound
        serverbound(payloads, ColorApplicatorSelectColorPacket.TYPE, ColorApplicatorSelectColorPacket.STREAM_CODEC);
        serverbound(payloads, RequestClosestMeteoritePacket.TYPE, RequestClosestMeteoritePacket.STREAM_CODEC);
        serverbound(payloads, ConfigButtonPacket.TYPE, ConfigButtonPacket.STREAM_CODEC);
        serverbound(payloads, ConfirmAutoCraftPacket.TYPE, ConfirmAutoCraftPacket.STREAM_CODEC);
        serverbound(payloads, FillCraftingGridFromRecipePacket.TYPE, FillCraftingGridFromRecipePacket.STREAM_CODEC);
        serverbound(payloads, GuiActionPacket.TYPE, GuiActionPacket.STREAM_CODEC);
        serverbound(payloads, HotkeyPacket.TYPE, HotkeyPacket.STREAM_CODEC);
        serverbound(payloads, InventoryActionPacket.TYPE, InventoryActionPacket.STREAM_CODEC);
        serverbound(payloads, MEInteractionPacket.TYPE, MEInteractionPacket.STREAM_CODEC);
        serverbound(payloads, MouseWheelPacket.TYPE, MouseWheelPacket.STREAM_CODEC);
        serverbound(payloads, PartLeftClickPacket.TYPE, PartLeftClickPacket.STREAM_CODEC);
        serverbound(payloads, QuickMovePatternPacket.TYPE, QuickMovePatternPacket.STREAM_CODEC);
        serverbound(payloads, SelectKeyTypePacket.TYPE, SelectKeyTypePacket.STREAM_CODEC);
        serverbound(payloads, SwapSlotsPacket.TYPE, SwapSlotsPacket.STREAM_CODEC);
        serverbound(payloads, SwitchGuisPacket.TYPE, SwitchGuisPacket.STREAM_CODEC);
        serverbound(payloads, UpdateHoldingCtrlPacket.TYPE, UpdateHoldingCtrlPacket.STREAM_CODEC);

        // Bidirectional
        bidirectional(payloads, ConfigValuePacket.TYPE, ConfigValuePacket.STREAM_CODEC);

        return Collections.unmodifiableList(payloads);
    }

    /**
     * Visits all payloads to be registered, in registration order.
     */
    public static void forEachPayload(Consumer<PayloadEntry<?>> visitor) {
        PAYLOADS.forEach(visitor);
    }

    private static <T extends ClientboundPacket> void clientbound(List<PayloadEntry<?>> payloads,
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        payloads.add(new PayloadEntry<>(PayloadDirection.CLIENTBOUND, type, codec));
    }

    private static <T extends ServerboundPacket> void serverbound(List<PayloadEntry<?>> payloads,
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        payloads.add(new PayloadEntry<>(PayloadDirection.SERVERBOUND, type, codec));
    }

    private static <T extends ServerboundPacket & ClientboundPacket> void bidirectional(List<PayloadEntry<?>> payloads,
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        payloads.add(new PayloadEntry<>(PayloadDirection.BIDIRECTIONAL, type, codec));
    }
}
