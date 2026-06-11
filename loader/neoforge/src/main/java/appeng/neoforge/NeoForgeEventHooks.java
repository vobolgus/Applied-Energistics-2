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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import appeng.core.LoaderEventHooks;

/**
 * NeoForge implementation of the {@link LoaderEventHooks} seam.
 */
public class NeoForgeEventHooks implements LoaderEventHooks {
    @Override
    public void firePlayerCraftingEvent(Player player, ItemStack craftedItem, Container container) {
        EventHooks.firePlayerCraftingEvent(player, craftedItem, container);
    }

    @Override
    public void firePlayerDestroyItem(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        NeoForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, stack, hand));
    }

    @Override
    public boolean isBlockPlaceCanceled(Player player, Level level, BlockPos pos, Direction direction) {
        return EventHooks.onBlockPlace(player, BlockSnapshot.create(player.level().dimension(), level, pos),
                direction);
    }

    @Override
    public boolean isBlockBreakCanceled(Level level, BlockPos pos, BlockState state, Player player) {
        var event = new BreakBlockEvent(level, pos, state, player);
        return NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    @Override
    public void fireExplosionDetonate(Level level, ServerExplosion explosion, List<Entity> affectedEntities,
            List<BlockPos> affectedBlocks) {
        EventHooks.onExplosionDetonate(level, explosion, affectedEntities, affectedBlocks);
    }

    @Override
    public void fireLevelLoad(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }
}
