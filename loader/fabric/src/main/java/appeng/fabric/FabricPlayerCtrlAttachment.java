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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.world.entity.player.Player;

import appeng.core.PlayerCtrlAttachment;

/**
 * Fabric implementation of the {@link PlayerCtrlAttachment} seam. The attachment is transient (not saved, not synced,
 * defaults to false — matching the NeoForge data attachment), so a weak map is the smallest faithful implementation.
 */
public class FabricPlayerCtrlAttachment implements PlayerCtrlAttachment {
    private final Map<Player, Boolean> holdingCtrl = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public boolean isHoldingCtrl(Player player) {
        return holdingCtrl.getOrDefault(player, false);
    }

    @Override
    public void setHoldingCtrl(Player player, boolean value) {
        holdingCtrl.put(player, value);
    }
}
