package appeng.fabric.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import appeng.core.AppEngBase;
import appeng.core.registration.AERegistries;

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

        registerFabricApiSyncedSerializers();

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

    /**
     * Opts every AE2 recipe serializer into <em>fabric-api's</em> recipe synchronization
     * ({@link RecipeSynchronization}), which is an entirely separate channel from the AE2 payload above.
     * <p>
     * ⚠ This is what recipe viewers read. Since 1.21.2 vanilla no longer ships recipes to the client, and fabric-api's
     * replacement is opt-in <em>per recipe serializer</em>: {@code RecipeSyncImpl} only groups (server side) and only
     * requests (client side) serializers that were passed to {@link RecipeSynchronization#synchronizeRecipeSerializer}.
     * JEI's Fabric entrypoint opts in every {@code minecraft:}-namespace serializer and nothing else, then feeds
     * {@code SynchronizedRecipes#recipes()} into the {@code RecipeMap} backing its vanilla categories — so a modded
     * crafting-table recipe with a custom serializer is simply absent from JEI unless its own mod opts in. Live report
     * 2026-07-30: {@code ae2:cable_anchor} (the only {@code ae2:quartz_cutting} recipe, a {@code RecipeType.CRAFTING}
     * recipe) showed no recipe in JEI on Fabric while working on NeoForge, where AE2 syncs all of
     * {@code RecipeType.CRAFTING} through {@code OnDatapackSyncEvent}.
     * <p>
     * The AE2 payload above cannot cover this: it delivers into AE2's <em>own</em> client-side recipe map (what the AE2
     * JEI categories and the crafting terminals read), which no third-party viewer knows about.
     * <p>
     * Registering the whole {@link Registries#RECIPE_SERIALIZER} collector rather than a hand-written list keeps future
     * serializers covered automatically; {@code RecipeSerializerSyncTestPlots} asserts that census. Must run on both
     * dists (the server needs the set to group recipes, the client to request them) — {@link #init} is the shared
     * funnel for that.
     */
    private static void registerFabricApiSyncedSerializers() {
        for (var entry : AERegistries.entries(Registries.RECIPE_SERIALIZER)) {
            RecipeSynchronization.synchronizeRecipeSerializer((RecipeSerializer<?>) entry.get());
        }
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
