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

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;

/**
 * Loader-neutral seam for the transient (not persisted, not synced) per-player "is holding ctrl" flag used for
 * opposite-side part placement. The loader-specific implementation (e.g.
 * {@code appeng.neoforge.NeoForgePlayerCtrlAttachment}, which is backed by a NeoForge data attachment) is injected once
 * during mod construction via {@link #init}. Defaults to false for players that never had the flag set.
 */
public interface PlayerCtrlAttachment {
    boolean isHoldingCtrl(Player player);

    void setHoldingCtrl(Player player, boolean holdingCtrl);

    static PlayerCtrlAttachment get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific PlayerCtrlAttachment has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(PlayerCtrlAttachment attachment) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The PlayerCtrlAttachment has already been initialized");
        }
        Holder.INSTANCE = attachment;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile PlayerCtrlAttachment INSTANCE;

        private Holder() {
        }
    }
}
