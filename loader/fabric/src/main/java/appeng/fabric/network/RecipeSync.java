package appeng.fabric.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

import appeng.core.AppEngBase;

/**
 * Server-side part of the Fabric recipe synchronization (GuideME precedent). See {@link SyncRecipesPayload}. Sends on
 * player join and after successful datapack reloads, mirroring NeoForge's {@code OnDatapackSyncEvent} timing.
 */
public final class RecipeSync {
    /**
     * Number of recipes per chunk payload, to stay well below the custom payload size limit.
     */
    private static final int CHUNK_SIZE = 250;

    private RecipeSync() {
    }

    public static void init(AppEngBase base) {
        PayloadTypeRegistry.clientboundPlay().register(SyncRecipesPayload.TYPE, SyncRecipesPayload.STREAM_CODEC);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendRecipes(base, server,
                handler.player));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                for (var player : server.getPlayerList().getPlayers()) {
                    sendRecipes(base, server, player);
                }
            }
        });
    }

    private static void sendRecipes(AppEngBase base, MinecraftServer server, ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, SyncRecipesPayload.TYPE)) {
            return; // Client doesn't have AE2 installed (e.g. a vanilla client)
        }

        // Vanilla offers no accessor for the server's RecipeMap; rebuild one from the recipe collection
        var recipeMap = RecipeMap.create(server.getRecipeManager().getRecipes());

        List<RecipeHolder<?>> recipes = new ArrayList<>();
        List<Identifier> typeIds = new ArrayList<>();
        for (var recipeType : base.getServerSyncedRecipeTypes()) {
            recipes.addAll(recipesOfType(recipeMap, recipeType));
            typeIds.add(BuiltInRegistries.RECIPE_TYPE.getKey(recipeType));
        }

        ServerPlayNetworking.send(player, new SyncRecipesPayload(true, List.of(), Optional.empty()));
        for (int i = 0; i < recipes.size(); i += CHUNK_SIZE) {
            var chunk = recipes.subList(i, Math.min(recipes.size(), i + CHUNK_SIZE));
            ServerPlayNetworking.send(player, new SyncRecipesPayload(false, chunk, Optional.empty()));
        }
        ServerPlayNetworking.send(player, new SyncRecipesPayload(false, List.of(), Optional.of(typeIds)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Collection<RecipeHolder<?>> recipesOfType(RecipeMap recipeMap, RecipeType<?> recipeType) {
        return (Collection) recipeMap.byType((RecipeType) recipeType);
    }
}
