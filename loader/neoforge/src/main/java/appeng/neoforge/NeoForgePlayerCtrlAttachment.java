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

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;

import appeng.core.PlayerCtrlAttachment;
import appeng.core.definitions.AEAttachmentTypes;

/**
 * {@link PlayerCtrlAttachment} backed by the transient {@link AEAttachmentTypes#HOLDING_CTRL} NeoForge data attachment.
 */
public class NeoForgePlayerCtrlAttachment implements PlayerCtrlAttachment {
    public NeoForgePlayerCtrlAttachment(IEventBus modEventBus) {
        AEAttachmentTypes.register(modEventBus);
    }

    @Override
    public boolean isHoldingCtrl(Player player) {
        return player.getData(AEAttachmentTypes.HOLDING_CTRL);
    }

    @Override
    public void setHoldingCtrl(Player player, boolean holdingCtrl) {
        player.setData(AEAttachmentTypes.HOLDING_CTRL, holdingCtrl);
    }
}
