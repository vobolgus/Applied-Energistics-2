
package appeng.core.network.serverbound;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

import appeng.api.parts.IPartHost;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.ServerboundPacket;

/**
 * Packet sent when a player left-clicks on a part attached to a cable bus. This packet contains the hit position to
 * restore the part that was hit on the client.
 */
public record PartLeftClickPacket(BlockHitResult hitResult, boolean alternateUseMode) implements ServerboundPacket {
    public static final StreamCodec<RegistryFriendlyByteBuf, PartLeftClickPacket> STREAM_CODEC = StreamCodec.ofMember(
            PartLeftClickPacket::write,
            PartLeftClickPacket::decode);

    public static final Type<PartLeftClickPacket> TYPE = CustomAppEngPayload.createType("part_left_click");

    @Override
    public Type<PartLeftClickPacket> type() {
        return TYPE;
    }

    public static PartLeftClickPacket decode(RegistryFriendlyByteBuf stream) {
        var hitResult = stream.readBlockHitResult();
        var alternateUseMode = stream.readBoolean();
        return new PartLeftClickPacket(hitResult, alternateUseMode);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeBlockHitResult(hitResult);
        data.writeBoolean(alternateUseMode);
    }

    @Override
    public void handleOnServer(ServerPlayer player) {
        // Replaces NeoForge's CommonHooks.onLeftClickBlock + event post: that hook performs no validation
        // itself, it only fires PlayerInteractEvent.LeftClickBlock to give protection mods a chance to
        // cancel the interaction. We inline the equivalent vanilla server-side validation performed for
        // START_DESTROY_BLOCK in ServerPlayerGameMode.handleBlockBreakAction: reach check, spectator check
        // and ServerPlayer.mayInteract (spawn protection + world border). A loader-specific interaction
        // event hook can be re-attached at this seam later.
        var pos = hitResult.getBlockPos();
        if (player.isSpectator()
                || !player.isWithinBlockInteractionRange(pos, 1.0)
                || !player.mayInteract(player.level(), pos)) {
            return;
        }

        var localPos = hitResult.getLocation().subtract(
                hitResult.getBlockPos().getX(),
                hitResult.getBlockPos().getY(),
                hitResult.getBlockPos().getZ());

        if (player.level().getBlockEntity(hitResult.getBlockPos()) instanceof IPartHost partHost) {
            var selectedPart = partHost.selectPartLocal(localPos);
            if (selectedPart.part != null) {
                if (!alternateUseMode) {
                    selectedPart.part.onClicked(player, localPos);
                } else {
                    selectedPart.part.onShiftClicked(player, localPos);
                }
            } else if (selectedPart.facade != null) {
                selectedPart.facade.onClicked(player, localPos);
            }
        }
    }
}
