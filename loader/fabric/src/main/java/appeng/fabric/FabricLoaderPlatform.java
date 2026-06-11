/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2025, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.fabric;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

import com.mojang.authlib.GameProfile;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import appeng.api.inventories.ItemTransfer;
import appeng.fabric.transfer.FabricResources;
import appeng.hooks.extensions.BlockCaughtFireHook;
import appeng.util.AESavedDataType;
import appeng.util.LoaderPlatform;

/**
 * Fabric implementation of the {@link LoaderPlatform} seam.
 */
public class FabricLoaderPlatform implements LoaderPlatform {
    /**
     * Fabric has no crafting-player tracking of its own (NeoForge: {@code CommonHooks}); AE2 is the only getter/setter
     * pair, so a simple thread local is fully faithful.
     */
    private static final ThreadLocal<@Nullable Player> CRAFTING_PLAYER = new ThreadLocal<>();

    @Nullable
    private volatile MinecraftServer currentServer;

    @Override
    public boolean isFakePlayer(Player player) {
        return player instanceof net.fabricmc.fabric.api.entity.FakePlayer;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    @Nullable
    public Player getCraftingPlayer() {
        return CRAFTING_PLAYER.get();
    }

    @Override
    public void setCraftingPlayer(@Nullable Player player) {
        CRAFTING_PLAYER.set(player);
    }

    @Override
    public boolean recipeInputsMatch(List<ItemStack> inputs, List<Ingredient> ingredients) {
        // Port of NeoForge's RecipeMatcher semantics: find an assignment of every input to a distinct
        // ingredient such that all ingredients are satisfied.
        if (inputs.size() != ingredients.size()) {
            return false;
        }
        return matchRecursive(inputs, ingredients, 0, new boolean[ingredients.size()]);
    }

    private static boolean matchRecursive(List<ItemStack> inputs, List<Ingredient> ingredients, int inputIndex,
            boolean[] usedIngredients) {
        if (inputIndex == inputs.size()) {
            return true;
        }
        var input = inputs.get(inputIndex);
        for (int i = 0; i < ingredients.size(); i++) {
            if (!usedIngredients[i] && ingredients.get(i).test(input)) {
                usedIngredients[i] = true;
                if (matchRecursive(inputs, ingredients, inputIndex + 1, usedIngredients)) {
                    return true;
                }
                usedIngredients[i] = false;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public ItemTransfer wrapExternalInventory(Level level, BlockPos pos, Direction side) {
        return FabricResources.wrapExternal(level, pos, side);
    }

    @Override
    @Nullable
    public Object getCachedBlockEntityRenderData(BlockAndLightGetter view, BlockPos pos) {
        if (view instanceof net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter fabricView) {
            return fabricView.getBlockEntityRenderData(pos);
        }
        return null;
    }

    @Override
    public ThreadGroup getServerThreadGroup() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            // On a dedicated server every game thread lives in the main thread group; there is no client.
            return Thread.currentThread().getThreadGroup();
        }
        // TODO (fabric, Phase 3): Fabric has no sided thread groups. On the client this placeholder group
        // makes Platform.isClient() always true / isServer() always false, which is WRONG for the integrated
        // server thread. Needs a server.isSameThread()-style rework of Platform before client support lands.
        return new ThreadGroup("ae2-server-thread-placeholder");
    }

    @Override
    public boolean hasClientClasses() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public String getModDisplayName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId);
    }

    @Override
    public Player getFakePlayer(ServerLevel level, GameProfile profile) {
        return net.fabricmc.fabric.api.entity.FakePlayer.get(level, profile);
    }

    @Override
    @Nullable
    public MinecraftServer getCurrentServer() {
        return currentServer;
    }

    /**
     * Tracked from the Fabric server lifecycle events by {@link AppEngFabric}.
     */
    public void setCurrentServer(@Nullable MinecraftServer server) {
        this.currentServer = server;
    }

    @Override
    public boolean isCustomIngredient(Ingredient ingredient) {
        // Fabric API interface-injects getCustomIngredient() into the vanilla Ingredient.
        return ingredient.getCustomIngredient() != null;
    }

    @Override
    public boolean isSimpleIngredient(Ingredient ingredient) {
        // Fabric API interface-injects requiresTesting(); its inverse matches NeoForge's Ingredient#isSimple.
        return !ingredient.requiresTesting();
    }

    @Override
    public RecipeMap getRecipeMap(RecipeManager recipeManager) {
        // The vanilla field is private; widened via ae2.accesswidener (NeoForge instead patches in recipeMap()).
        return recipeManager.recipes;
    }

    @Override
    public int getBurnTime(ItemStack stack, FuelValues fuelValues) {
        // Fabric's FuelRegistryEvents feed directly into FuelValues, so the vanilla lookup is authoritative here.
        return fuelValues.burnDuration(stack);
    }

    @Override
    public boolean onCaughtFire(Block block, BlockState state, Level level, BlockPos pos, @Nullable Direction face,
            @Nullable LivingEntity igniter) {
        if (block instanceof BlockCaughtFireHook hook) {
            return hook.onCaughtFire(state, level, pos, face, igniter);
        }
        if (block instanceof TntBlock) {
            // Replicates NeoForge's TntBlock#onCaughtFire override; the private igniter-aware prime overload is
            // widened via ae2.accesswidener.
            return TntBlock.prime(level, pos, igniter);
        }
        return true; // NeoForge's IBlockExtension#onCaughtFire default
    }

    @Override
    public float getExplosionResistance(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        // NeoForge's contextual default delegates to the context-free value; no AE2 block overrides the contextual
        // overload, so this is faithful for everything AE2 explodes itself.
        return state.getBlock().getExplosionResistance();
    }

    @Override
    public SoundType getSoundType(BlockState state, Level level, BlockPos pos, @Nullable Entity entity) {
        // NeoForge's contextual default delegates to the context-free value; no AE2 block overrides the contextual
        // overload.
        return state.getSoundType();
    }

    /**
     * Injected by the client entrypoint (Phase 3) to report the Shift key state for tooltips; lives here so the main
     * source set stays free of client classes.
     */
    @Nullable
    private static volatile BooleanSupplier shiftDownSupplier;

    public static void setShiftDownSupplier(BooleanSupplier supplier) {
        shiftDownSupplier = supplier;
    }

    @Override
    public boolean tooltipHasShiftDown(TooltipFlag flags) {
        // TODO (fabric, Phase 3): the client entrypoint must call setShiftDownSupplier(Screen::hasShiftDown).
        var supplier = shiftDownSupplier;
        return supplier != null && supplier.getAsBoolean();
    }

    @Override
    public void orderCreativeTabAfter(CreativeModeTab.Builder builder, ResourceKey<CreativeModeTab> precedingTab) {
        // No-op: vanilla/Fabric sorts creative tabs by registration order, and AE2 registers its tabs in the
        // desired order (main before facades).
    }

    /**
     * Vanilla {@code SavedDataType} has no level-sensitive factories/codecs (the NeoForge patch AE2 relied on), so a
     * type instance is materialized per level, with the level captured in the closures. Memoized per level because the
     * storage cache is keyed by type instance; WeakHashMap so closed levels can be collected.
     */
    private final Map<ServerLevel, Map<Identifier, SavedDataType<?>>> savedDataTypes = new WeakHashMap<>();

    @Override
    public synchronized <T extends SavedData> T computeSavedDataIfAbsent(ServerLevel level, AESavedDataType<T> type) {
        var levelTypes = savedDataTypes.computeIfAbsent(level, l -> new java.util.HashMap<>());
        @SuppressWarnings("unchecked")
        var savedDataType = (SavedDataType<T>) levelTypes.computeIfAbsent(type.id(),
                // TODO (fabric, Phase 2b): vanilla requires a non-null DataFixTypes and runs its fixers on load.
                // SAVED_DATA_COMMAND_STORAGE is the most shape-agnostic reference (arbitrary NBT); verify on the
                // runtime gate that loading pre-existing AE2 data is unaffected.
                id -> new SavedDataType<>(id,
                        () -> type.factory().apply(level),
                        type.codecFactory().apply(level),
                        DataFixTypes.SAVED_DATA_COMMAND_STORAGE));
        return level.getDataStorage().computeIfAbsent(savedDataType);
    }
}
