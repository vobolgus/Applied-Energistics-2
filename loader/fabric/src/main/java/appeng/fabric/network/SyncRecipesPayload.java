package appeng.fabric.network;

import java.util.List;
import java.util.Optional;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import appeng.core.AppEng;

/**
 * Fabric replacement for NeoForge's built-in recipe synchronization ({@code OnDatapackSyncEvent#sendRecipes} /
 * {@code RecipesReceivedEvent}), mirroring GuideME's Fabric recipe-sync approach. The server sends the recipes of
 * {@code AppEngBase#getServerSyncedRecipeTypes()} in chunks to stay below the payload size limit: first a {@code clear}
 * payload, then recipe chunks, and finally a payload carrying the synced recipe type ids, which signals the client to
 * build its recipe map.
 */
public record SyncRecipesPayload(boolean clear,
        List<RecipeHolder<?>> recipes,
        Optional<List<Identifier>> availableRecipeTypes) implements CustomPacketPayload {

    public static final Type<SyncRecipesPayload> TYPE = new Type<>(AppEng.makeId("sync_recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRecipesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncRecipesPayload::clear,
            RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncRecipesPayload::recipes,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list())),
            SyncRecipesPayload::availableRecipeTypes,
            SyncRecipesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
