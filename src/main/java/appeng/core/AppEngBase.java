/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.core;

import java.util.Collection;
import java.util.Collections;

import com.mojang.brigadier.CommandDispatcher;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import appeng.api.ids.AEComponents;
import appeng.api.parts.CableRenderMode;
import appeng.api.stacks.AEKeyType;
import appeng.core.definitions.AEBlockEntities;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEEntities;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.NetworkAdapter;
import appeng.hooks.ticking.TickHandler;
import appeng.init.InitCauldronInteraction;
import appeng.init.InitDispenserBehavior;
import appeng.init.internal.InitBlockEntityMoveStrategies;
import appeng.init.internal.InitGridLinkables;
import appeng.init.internal.InitGridServices;
import appeng.init.internal.InitP2PAttunements;
import appeng.init.internal.InitStorageCells;
import appeng.init.internal.InitUpgrades;
import appeng.init.worldgen.InitStructures;
import appeng.recipes.AERecipeSerializers;
import appeng.recipes.AERecipeTypes;
import appeng.server.AECommand;
import appeng.sounds.AppEngSounds;
import appeng.util.LoaderPlatform;

/**
 * Mod functionality that is common to both dedicated server and client.
 * <p>
 * Note that a client will still have zero or more embedded servers (although only one at a time).
 * <p>
 * This class is loader-free. The loader-specific entrypoint (e.g. {@code appeng.neoforge.AppEngNeoForge}) constructs
 * the dist-specific subclass, injects the platform seams (config store, {@link LoaderPlatform},
 * {@link LoaderEventHooks}, network adapter, ...) and then drives the lifecycle methods below from its own
 * loader-specific events.
 */
public abstract class AppEngBase implements AppEng {

    private static final Logger LOG = LoggerFactory.getLogger(AppEngBase.class);

    /**
     * While we process a player-specific part placement/cable interaction packet, we need to use that player's
     * transparent-facade mode to understand whether the player can see through facades or not.
     * <p>
     * We need to use this method since the collision shape methods do not know about the player that the shape is being
     * requested for, so they will call {@link #getCableRenderMode()} below, which then will use this field to figure
     * out which player it's for.
     */
    private final ThreadLocal<Player> partInteractionPlayer = new ThreadLocal<>();

    static AppEngBase INSTANCE;

    public AppEngBase() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        INSTANCE = this;
    }

    /**
     * Loads all registration content. Must be called by the loader entrypoint during mod construction, after the config
     * store and the platform seams have been injected, and before the loader flushes the collected registration entries
     * into the game registries.
     */
    public void registerContent() {
        InitGridServices.init();
        InitBlockEntityMoveStrategies.init();

        AEParts.init();
        // Force-load the classes below in the same order as before, so that their registration entries
        // are collected into AERegistries before the loader flushes them into the game registries.
        AEBlocks.init();
        AEItems.init();
        AEBlockEntities.init();
        AEComponents.init();
        AEEntities.init();
        AERecipeTypes.init();
        AERecipeSerializers.init();
        InitStructures.register();
    }

    /**
     * The recipe types whose recipes must be synchronized to clients in addition to what vanilla syncs (i.e. for
     * GuideME and our crafting terminals). The loader entrypoint hooks this into its datapack-sync mechanism.
     */
    public RecipeType<?>[] getServerSyncedRecipeTypes() {
        return new RecipeType<?>[] {
                RecipeType.CRAFTING,
                RecipeType.STONECUTTING,
                RecipeType.SMITHING,
                RecipeType.SMELTING,
                // For GuideME
                AERecipeTypes.INSCRIBER,
                AERecipeTypes.TRANSFORM,
                AERecipeTypes.CHARGER,
                AERecipeTypes.ENTROPY,
                AERecipeTypes.MATTER_CANNON_AMMO,
                AERecipeTypes.QUARTZ_CUTTING
        };
    }

    /**
     * Runs after all mods have had time to run their registrations into registries. Called by the loader entrypoint
     * from its common-setup phase (on the main thread).
     */
    public void postRegistrationInitialization() {
        // Now that item instances are available, we can initialize registries that need item instances
        InitGridLinkables.init();
        InitStorageCells.init();

        InitP2PAttunements.init();

        InitCauldronInteraction.init();
        InitDispenserBehavior.init();

        InitUpgrades.init();
    }

    public void registerKeyTypes(Registry<AEKeyType> registry) {
        Registry.register(registry, AEKeyType.items().getId(), AEKeyType.items());
        Registry.register(registry, AEKeyType.fluids().getId(), AEKeyType.fluids());
    }

    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        new AECommand().register(dispatcher);
    }

    public void registerSounds(Registry<SoundEvent> registry) {
        AppEngSounds.register(registry);
    }

    /**
     * Called by the loader entrypoint when a server has fully stopped.
     */
    public void onServerStopped() {
        TickHandler.instance().shutdown();
    }

    public void registerCreativeTabs(Registry<CreativeModeTab> registry) {
        MainCreativeTab.init(registry);
        FacadeCreativeTab.init(registry);
    }

    @Override
    public Collection<ServerPlayer> getPlayers() {
        var server = getCurrentServer();

        if (server != null) {
            return server.getPlayerList().getPlayers();
        }

        return Collections.emptyList();
    }

    @Override
    public void sendToAllNearExcept(Player p, double x, double y, double z,
            double dist, Level level, ClientboundPacket packet) {
        if (level instanceof ServerLevel serverLevel) {
            ServerPlayer except = null;
            if (p instanceof ServerPlayer) {
                except = (ServerPlayer) p;
            }
            NetworkAdapter.get().sendToPlayersNear(serverLevel, except, x, y, z, dist, packet);
        }
    }

    @Override
    public void setPartInteractionPlayer(Player player) {
        this.partInteractionPlayer.set(player);
    }

    @Override
    public CableRenderMode getCableRenderMode() {
        return this.getCableRenderModeForPlayer(partInteractionPlayer.get());
    }

    @Nullable
    @Override
    public MinecraftServer getCurrentServer() {
        return LoaderPlatform.get().getCurrentServer();
    }

    @Override
    public void sendSystemMessage(Player player, Component text) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(text);
        }
    }

    protected final CableRenderMode getCableRenderModeForPlayer(@Nullable Player player) {
        if (player != null) {
            if (AEItems.NETWORK_TOOL.is(player.getItemInHand(InteractionHand.MAIN_HAND))
                    || AEItems.NETWORK_TOOL.is(player.getItemInHand(InteractionHand.OFF_HAND))) {
                return CableRenderMode.CABLE_VIEW;
            }
        }

        return CableRenderMode.STANDARD;
    }

    @Override
    public RecipeMap getRecipeMapForType(Level level, RecipeType<?> recipeType) {
        if (level instanceof ServerLevel serverLevel) {
            return LoaderPlatform.get().getRecipeMap(serverLevel.recipeAccess());
        } else {
            LOG.warn("Don't know how to retrieve recipe information for level type {}", level);
            return RecipeMap.EMPTY;
        }
    }
}
