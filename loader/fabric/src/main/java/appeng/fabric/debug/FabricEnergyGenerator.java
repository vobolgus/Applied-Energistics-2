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

package appeng.fabric.debug;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import team.reborn.energy.api.EnergyStorage;

import appeng.debug.EnergyGeneratorPlatform;

/**
 * Fabric implementation of the {@link EnergyGeneratorPlatform} seam, pushing energy into adjacent Team Reborn energy
 * consumers.
 */
public class FabricEnergyGenerator implements EnergyGeneratorPlatform {
    @Override
    public void pushEnergy(Level level, BlockPos targetPos, Direction targetSide, int amount) {
        var consumer = EnergyStorage.SIDED.find(level, targetPos, targetSide);
        if (consumer != null) {
            try (var tx = Transaction.openOuter()) {
                consumer.insert(amount, tx);
                tx.commit();
            }
        }
    }
}
