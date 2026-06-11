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

package appeng.util;

import java.util.List;

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

import appeng.api.inventories.ItemTransfer;

/**
 * Loader-neutral seam for small loader-environment queries that have no vanilla equivalent. The loader-specific
 * implementation (e.g. {@code appeng.neoforge.NeoForgeLoaderPlatform}) is injected once during mod construction via
 * {@link #init}.
 */
public interface LoaderPlatform {
    /**
     * @return true if the given player is a loader-provided fake player (i.e. an automation block impersonating a
     *         player).
     */
    boolean isFakePlayer(Player player);

    /**
     * @return true if a mod with the given id is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * @return The player currently crafting (as tracked by the loader's crafting hooks), or null if unknown.
     */
    @Nullable
    Player getCraftingPlayer();

    /**
     * Sets the player currently crafting for the loader's crafting hooks (pass null to clear).
     *
     * @see #getCraftingPlayer()
     */
    void setCraftingPlayer(@Nullable Player player);

    /**
     * Checks whether the given crafting inputs can be assigned one-to-one to the given ingredients (the shapeless
     * matching algorithm the loader uses for recipes with non-simple ingredients).
     */
    boolean recipeInputsMatch(List<ItemStack> inputs, List<Ingredient> ingredients);

    /**
     * Exposes the item handler of the block adjacent at the given position/side through the loader's transfer API as an
     * {@link ItemTransfer}, or null if there is none. (Was {@code InternalInventory.wrapExternal}.)
     */
    @Nullable
    ItemTransfer wrapExternalInventory(Level level, BlockPos pos, Direction side);

    /**
     * Returns the loader's cached client-side render data (see {@code AEBaseBlockEntityHooks#getRenderData()}) for the
     * block entity at the given position of a client-side render view, or null if there is none.
     */
    @Nullable
    Object getCachedBlockEntityRenderData(BlockAndLightGetter view, BlockPos pos);

    /**
     * @return The thread group that all server threads belong to.
     */
    ThreadGroup getServerThreadGroup();

    /**
     * @return True if client-side classes (such as Renderers) are available.
     */
    boolean hasClientClasses();

    /**
     * @return True if AE2 is being run within a dev environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * @return The display name of the mod with the given id, or the id itself if the mod is unknown.
     */
    String getModDisplayName(String modId);

    /**
     * @return The loader's fake player for the given level and profile (i.e. for automation acting as a player).
     */
    Player getFakePlayer(ServerLevel level, GameProfile profile);

    /**
     * @return The currently running server, or null if no server is running.
     */
    @Nullable
    MinecraftServer getCurrentServer();

    /**
     * @return true if the given ingredient uses loader-specific custom matching logic (NeoForge:
     *         {@code Ingredient#isCustom}, Fabric: a Fabric API custom ingredient).
     */
    boolean isCustomIngredient(Ingredient ingredient);

    /**
     * @return true if the given ingredient matches purely by item identity, so item-keyed (fuzzy) lookups are
     *         sufficient (NeoForge: {@code Ingredient#isSimple}, Fabric: {@code !requiresTesting()}).
     */
    boolean isSimpleIngredient(Ingredient ingredient);

    /**
     * Returns the recipe map of the given recipe manager (NeoForge exposes it via a patch; vanilla keeps it private).
     */
    RecipeMap getRecipeMap(RecipeManager recipeManager);

    /**
     * Returns the burn duration of the given stack in ticks, including loader-specific item overrides (was NeoForge's
     * {@code ItemStack#getBurnTime(null, fuelValues)}; vanilla/Fabric: {@code FuelValues#burnDuration}).
     */
    int getBurnTime(ItemStack stack, FuelValues fuelValues);

    /**
     * Notifies the given block that it caught fire and returns whether it reacted (was NeoForge's
     * {@code IBlockExtension#onCaughtFire}). On Fabric this dispatches to
     * {@link appeng.hooks.extensions.BlockCaughtFireHook} and replicates the vanilla TNT behavior.
     */
    boolean onCaughtFire(Block block, BlockState state, Level level, BlockPos pos, @Nullable Direction face,
            @Nullable LivingEntity igniter);

    /**
     * Returns the position-aware explosion resistance of the given block (was NeoForge's contextual
     * {@code IBlockExtension#getExplosionResistance} overload; vanilla/Fabric falls back to the context-free value).
     */
    float getExplosionResistance(BlockState state, Level level, BlockPos pos, Explosion explosion);

    /**
     * Returns the position/entity-aware sound type of the given state (was NeoForge's
     * {@code IBlockStateExtension#getSoundType} overload; vanilla/Fabric falls back to the context-free value).
     */
    SoundType getSoundType(BlockState state, Level level, BlockPos pos, @Nullable Entity entity);

    /**
     * @return true if the shift key is held during tooltip rendering (was NeoForge's {@code TooltipFlag#hasShiftDown}).
     *         Always false on a dedicated server.
     */
    boolean tooltipHasShiftDown(TooltipFlag flags);

    /**
     * Requests that the given creative tab is sorted after the given other tab (was NeoForge's
     * {@code CreativeModeTab.Builder#withTabsBefore}). No-op on Fabric, where tabs are sorted in registration order.
     */
    void orderCreativeTabAfter(CreativeModeTab.Builder builder, ResourceKey<CreativeModeTab> precedingTab);

    /**
     * Loads or creates the given level-sensitive saved data for the given level (was
     * {@code level.getDataStorage().computeIfAbsent(SavedDataType)} with NeoForge's level-aware {@code SavedDataType}
     * patch).
     */
    <T extends SavedData> T computeSavedDataIfAbsent(ServerLevel level, AESavedDataType<T> type);

    static LoaderPlatform get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific LoaderPlatform has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(LoaderPlatform platform) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The LoaderPlatform has already been initialized");
        }
        Holder.INSTANCE = platform;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile LoaderPlatform INSTANCE;

        private Holder() {
        }
    }
}
