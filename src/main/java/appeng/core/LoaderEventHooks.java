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

package appeng.core;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loader-neutral seam for game events that AE2 <em>posts</em> for the benefit of other mods (protection mods,
 * crafting-trackers, etc.). These have no vanilla equivalent; each loader forwards them to its own event system. The
 * loader-specific implementation (e.g. {@code appeng.neoforge.NeoForgeEventHooks}) is injected once during mod
 * construction via {@link #init}.
 */
public interface LoaderEventHooks {
    /**
     * Notifies other mods that the given player crafted an item (NeoForge: {@code PlayerEvent.ItemCraftedEvent}).
     */
    void firePlayerCraftingEvent(Player player, ItemStack craftedItem, Container container);

    /**
     * Notifies other mods that the given player destroyed (broke) the given item (NeoForge:
     * {@code PlayerDestroyItemEvent}).
     */
    void firePlayerDestroyItem(Player player, ItemStack stack, @Nullable InteractionHand hand);

    /**
     * Asks protection mods whether the given player may place a block at the given position (NeoForge:
     * {@code EventHooks.onBlockPlace}).
     *
     * @return true if the placement was <em>canceled</em> by an event listener.
     */
    boolean isBlockPlaceCanceled(Player player, Level level, BlockPos pos, Direction direction);

    /**
     * Asks protection mods whether the given player may break the block at the given position (NeoForge:
     * {@code BreakBlockEvent}).
     *
     * @return true if breaking the block was <em>canceled</em> by an event listener.
     */
    boolean isBlockBreakCanceled(Level level, BlockPos pos, BlockState state, Player player);

    /**
     * Notifies other mods about the lists of entities and blocks affected by an explosion AE2 simulated itself
     * (NeoForge: {@code EventHooks.onExplosionDetonate}). AE2 has already applied the effects when this is fired.
     */
    void fireExplosionDetonate(Level level, ServerExplosion explosion, List<Entity> affectedEntities,
            List<BlockPos> affectedBlocks);

    /**
     * Notifies other mods that AE2 injected the spatial storage level into the running server (NeoForge:
     * {@code LevelEvent.Load}, emulating the event the loader fires for regular levels).
     */
    void fireLevelLoad(ServerLevel level);

    static LoaderEventHooks get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific LoaderEventHooks have not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(LoaderEventHooks hooks) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The LoaderEventHooks have already been initialized");
        }
        Holder.INSTANCE = hooks;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile LoaderEventHooks INSTANCE;

        private Holder() {
        }
    }
}
