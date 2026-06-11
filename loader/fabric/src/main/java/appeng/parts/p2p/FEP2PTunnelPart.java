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

package appeng.parts.p2p;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import team.reborn.energy.api.EnergyStorage;

import appeng.api.config.PowerUnit;
import appeng.api.parts.IPartItem;
import appeng.fabric.transfer.FabricTransaction;

/**
 * The Fabric twin of the NeoForge FE P2P tunnel, based on the Team Reborn {@link EnergyStorage} (E is treated 1:1 as FE
 * for the energy tax).
 */
public class FEP2PTunnelPart extends CapabilityP2PTunnelPart<FEP2PTunnelPart, EnergyStorage> {
    public FEP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, EnergyStorage.SIDED);
        inputHandler = new InputEnergyStorage();
        outputHandler = new OutputEnergyStorage();
        emptyHandler = EnergyStorage.EMPTY;
    }

    private class InputEnergyStorage implements EnergyStorage {
        @Override
        public long insert(long maxAmount, TransactionContext tx) {
            StoragePreconditions.notNegative(maxAmount);
            long total = 0;

            final int outputTunnels = getOutputs().size();
            final long amount = maxAmount;

            if (outputTunnels == 0 || amount == 0) {
                return 0;
            }

            final long amountPerOutput = amount / outputTunnels;
            long overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

            for (var target : getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    var output = capabilityGuard.get();
                    long toSend = amountPerOutput + overflow;

                    long received = output.insert(toSend, tx);

                    overflow = toSend - received;
                    total += received;
                }
            }

            deductEnergyCost(total, PowerUnit.FE, FabricTransaction.of(tx));

            return total;
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }

        @Override
        public long getAmount() {
            long tot = 0;
            for (var output : getOutputs()) {
                try (var capabilityGuard = output.getAdjacentCapability()) {
                    tot += capabilityGuard.get().getAmount();
                }
            }
            return tot;
        }

        @Override
        public long getCapacity() {
            long tot = 0;
            for (var output : getOutputs()) {
                try (var capabilityGuard = output.getAdjacentCapability()) {
                    tot += capabilityGuard.get().getCapacity();
                }
            }
            return tot;
        }
    }

    private class OutputEnergyStorage implements EnergyStorage {
        @Override
        public long insert(long maxAmount, TransactionContext tx) {
            return 0;
        }

        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        public long extract(long maxAmount, TransactionContext tx) {
            try (var input = getInputCapability()) {
                long extracted = input.get().extract(maxAmount, tx);
                deductEnergyCost(extracted, PowerUnit.FE, FabricTransaction.of(tx));
                return extracted;
            }
        }

        @Override
        public long getAmount() {
            try (var input = getInputCapability()) {
                return input.get().getAmount();
            }
        }

        @Override
        public long getCapacity() {
            try (var input = getInputCapability()) {
                return input.get().getCapacity();
            }
        }
    }
}
