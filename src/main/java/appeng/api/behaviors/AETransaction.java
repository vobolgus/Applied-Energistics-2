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

package appeng.api.behaviors;

/**
 * Opaque handle for an open transaction of the loader transfer API.
 * <p>
 * Shared AE2 code never opens transactions itself; it only participates in transactions started by the loader transfer
 * API (see {@code appeng.util.AESnapshotJournal}). Instances are created at the loader boundary: on NeoForge,
 * {@code appeng.neoforge.transfer.NeoForgeTransaction} wraps a NeoForge {@code TransactionContext}; on Fabric, the
 * analog will wrap a Fabric {@code TransactionContext}.
 */
public interface AETransaction {
}
