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

package appeng.fabric.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.behaviors.AETransaction;

/**
 * The Fabric implementation of the opaque {@link AETransaction} handle: a simple wrapper around the Fabric
 * {@link TransactionContext}.
 */
public record FabricTransaction(TransactionContext context) implements AETransaction {

    public static AETransaction of(TransactionContext context) {
        return new FabricTransaction(context);
    }

    public static TransactionContext unwrap(AETransaction transaction) {
        return ((FabricTransaction) transaction).context;
    }
}
