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

package appeng.neoforge;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import appeng.api.inventories.ItemTransfer;
import appeng.neoforge.render.NeoForgeRenderData;
import appeng.neoforge.transfer.NeoForgeResources;
import appeng.util.AESavedDataType;
import appeng.util.LoaderPlatform;

/**
 * NeoForge implementation of the {@link LoaderPlatform} seam.
 */
public class NeoForgeLoaderPlatform implements LoaderPlatform {
    @Override
    public boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    @Nullable
    public Player getCraftingPlayer() {
        return CommonHooks.getCraftingPlayer();
    }

    @Override
    public void setCraftingPlayer(@Nullable Player player) {
        CommonHooks.setCraftingPlayer(player);
    }

    @Override
    public boolean recipeInputsMatch(List<ItemStack> inputs, List<Ingredient> ingredients) {
        return RecipeMatcher.findMatches(inputs, ingredients) != null;
    }

    @Override
    @Nullable
    public ItemTransfer wrapExternalInventory(Level level, BlockPos pos, Direction side) {
        return NeoForgeResources.wrapExternal(level, pos, side);
    }

    @Override
    @Nullable
    public Object getCachedBlockEntityRenderData(BlockAndLightGetter view, BlockPos pos) {
        return NeoForgeRenderData.unwrap(view.getModelData(pos));
    }

    @Override
    public ThreadGroup getServerThreadGroup() {
        return SidedThreadGroups.SERVER;
    }

    @Override
    public boolean hasClientClasses() {
        // The null check is for tests
        var loader = FMLLoader.getCurrentOrNull();
        return loader == null || loader.getDist().isClient();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        var loader = FMLLoader.getCurrentOrNull();
        return loader == null || !loader.isProduction();
    }

    @Override
    public String getModDisplayName(String modId) {
        return ModList.get().getModContainerById(modId).map(mc -> mc.getModInfo().getDisplayName())
                .orElse(modId);
    }

    @Override
    public Player getFakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayerFactory.get(level, profile);
    }

    @Override
    @Nullable
    public MinecraftServer getCurrentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public boolean isCustomIngredient(Ingredient ingredient) {
        return ingredient.isCustom();
    }

    @Override
    public boolean isSimpleIngredient(Ingredient ingredient) {
        return ingredient.isSimple();
    }

    @Override
    public RecipeMap getRecipeMap(RecipeManager recipeManager) {
        return recipeManager.recipeMap();
    }

    @Override
    public int getBurnTime(ItemStack stack, FuelValues fuelValues) {
        return stack.getBurnTime(null, fuelValues);
    }

    @Override
    public boolean onCaughtFire(Block block, BlockState state, Level level, BlockPos pos, @Nullable Direction face,
            @Nullable LivingEntity igniter) {
        return block.onCaughtFire(state, level, pos, face, igniter);
    }

    @Override
    public float getExplosionResistance(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        return state.getBlock().getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public SoundType getSoundType(BlockState state, Level level, BlockPos pos, @Nullable Entity entity) {
        return state.getSoundType(level, pos, entity);
    }

    @Override
    public boolean tooltipHasShiftDown(TooltipFlag flags) {
        return flags.hasShiftDown();
    }

    @Override
    public void orderCreativeTabAfter(CreativeModeTab.Builder builder, ResourceKey<CreativeModeTab> precedingTab) {
        builder.withTabsBefore(precedingTab);
    }

    /**
     * Bridges {@link AESavedDataType} onto NeoForge's level-sensitive {@code SavedDataType} patch. Memoized so that the
     * data storage cache (keyed by type instance) sees a stable key.
     */
    private final Map<AESavedDataType<?>, SavedDataType<?>> savedDataTypes = new ConcurrentHashMap<>();

    @Override
    public <T extends SavedData> T computeSavedDataIfAbsent(ServerLevel level, AESavedDataType<T> type) {
        @SuppressWarnings("unchecked")
        var savedDataType = (SavedDataType<T>) savedDataTypes.computeIfAbsent(type,
                NeoForgeLoaderPlatform::toSavedDataType);
        return level.getDataStorage().computeIfAbsent(savedDataType);
    }

    private static <T extends SavedData> SavedDataType<T> toSavedDataType(AESavedDataType<T> type) {
        // NeoForge's level-sensitive Factory-based constructor (null dataFixType), exactly what the shared code
        // passed before this seam existed.
        return new SavedDataType<>(type.id(),
                level -> type.factory().apply(level),
                level -> type.codecFactory().apply(level));
    }
}
