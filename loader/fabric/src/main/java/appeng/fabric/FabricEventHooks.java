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

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
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

import appeng.core.LoaderEventHooks;

/**
 * Fabric implementation of the {@link LoaderEventHooks} seam.
 * <p>
 * <strong>Behavior notes (vs. NeoForge):</strong> Fabric has no equivalents for several of these outbound
 * notifications. Where noted, the operation is a no-op; the impact is limited to OTHER mods not being notified of the
 * respective AE2 action (AE2 itself does not consume any of these events):
 * <ul>
 * <li>{@link #firePlayerCraftingEvent}: no Fabric item-crafted event — no-op (advancement triggers etc. still work
 * through vanilla code paths AE2 calls directly).</li>
 * <li>{@link #firePlayerDestroyItem}: no Fabric equivalent — no-op.</li>
 * <li>{@link #isBlockPlaceCanceled}: no Fabric block-place protection event — always returns false (the matter cannon
 * paint placement cannot be blocked by protection mods on Fabric).</li>
 * <li>{@link #fireExplosionDetonate}: no Fabric explosion event — no-op (informational on NeoForge too).</li>
 * </ul>
 */
public class FabricEventHooks implements LoaderEventHooks {
    @Override
    public void firePlayerCraftingEvent(Player player, ItemStack craftedItem, Container container) {
        // No Fabric equivalent, see class javadoc.
    }

    @Override
    public void firePlayerDestroyItem(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        // No Fabric equivalent, see class javadoc.
    }

    @Override
    public boolean isBlockPlaceCanceled(Player player, Level level, BlockPos pos, Direction direction) {
        // No Fabric equivalent, see class javadoc.
        return false;
    }

    @Override
    public boolean isBlockBreakCanceled(Level level, BlockPos pos, BlockState state, Player player) {
        return !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state,
                level.getBlockEntity(pos));
    }

    @Override
    public void fireExplosionDetonate(Level level, ServerExplosion explosion, List<Entity> affectedEntities,
            List<BlockPos> affectedBlocks) {
        // No Fabric equivalent, see class javadoc.
    }

    @Override
    public void fireLevelLoad(ServerLevel level) {
        // AE2 injects the spatial storage level outside the vanilla level-creation loop, so Fabric's own
        // lifecycle mixin does not fire for it. Mirror NeoForge by posting the load event ourselves.
        ServerLevelEvents.LOAD.invoker().onLevelLoad(level.getServer(), level);
    }
}
