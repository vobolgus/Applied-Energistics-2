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

package appeng.util;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

import appeng.api.behaviors.AETransaction;
import appeng.neoforge.transfer.NeoForgeTransaction;

/**
 * Base class for shared AE2 state that participates in loader transfer-API transactions through the opaque
 * {@link AETransaction} handle.
 * <p>
 * This is a <strong>loader-duplicated</strong> class: on NeoForge it simply extends the NeoForge
 * {@link SnapshotJournal} (subclasses additionally inherit the {@code updateSnapshots(TransactionContext)} overload);
 * the Fabric version will provide the same contract on top of {@code SnapshotParticipant}.
 * <p>
 * Contract for subclasses (identical on both loaders, mirroring NeoForge's {@link SnapshotJournal}):
 * <ul>
 * <li>Call {@link #updateSnapshots(AETransaction)} right before modifying state in a transaction.</li>
 * <li>Override {@code createSnapshot()} to capture the current state in a nonnull object.</li>
 * <li>Override {@code revertToSnapshot(T)} to roll back to a previously captured state.</li>
 * <li>Optionally override {@code onRootCommit(T originalState)}: called after the root transaction was committed, with
 * the state from before the transactional operations.</li>
 * </ul>
 */
public abstract class AESnapshotJournal<T> extends SnapshotJournal<T> {

    /**
     * Updates the stored snapshots so that the changes happening as part of the passed transaction can be correctly
     * committed or rolled back. Call this every time the journal is about to change its state as part of a transaction.
     */
    public final void updateSnapshots(AETransaction transaction) {
        updateSnapshots(NeoForgeTransaction.unwrap(transaction));
    }
}
