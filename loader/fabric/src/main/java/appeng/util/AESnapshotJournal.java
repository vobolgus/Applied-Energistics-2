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

import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.behaviors.AETransaction;
import appeng.fabric.transfer.FabricTransaction;

/**
 * Base class for shared AE2 state that participates in loader transfer-API transactions through the opaque
 * {@link AETransaction} handle.
 * <p>
 * This is a <strong>loader-duplicated</strong> class: on NeoForge it simply extends the NeoForge
 * {@code SnapshotJournal}; this Fabric version ports the exact same snapshot algorithm (including
 * {@link #onRootCommit(Object)} receiving the original state) onto the Fabric transaction close callbacks, since
 * Fabric's own {@code SnapshotParticipant} releases the root snapshot before its {@code onFinalCommit} runs.
 * <p>
 * Contract for subclasses (identical on both loaders, mirroring NeoForge's {@code SnapshotJournal}):
 * <ul>
 * <li>Call {@link #updateSnapshots(AETransaction)} right before modifying state in a transaction.</li>
 * <li>Override {@link #createSnapshot()} to capture the current state in a <strong>nonnull</strong> object.</li>
 * <li>Override {@link #revertToSnapshot(Object)} to roll back to a previously captured state.</li>
 * <li>Optionally override {@link #onRootCommit(Object)}: called after the root transaction was committed, with the
 * state from before the transactional operations.</li>
 * </ul>
 */
public abstract class AESnapshotJournal<T>
        implements TransactionContext.CloseCallback, TransactionContext.OuterCloseCallback {
    /**
     * Marker for entries of {@link #snapshots} that do not correspond to a snapshot. {@code null} is not used as the
     * marker so that the {@link #originalState} null-check below remains sound (snapshots must be nonnull).
     */
    private static final Object NO_SNAPSHOT = new Object();

    private final ArrayList<T> snapshots = new ArrayList<>();

    @Nullable
    private T originalState = null;

    /**
     * Return a new <b>nonnull</b> object containing the current state of this journal.
     */
    protected abstract T createSnapshot();

    /**
     * Roll back to a state previously created by {@link #createSnapshot()}.
     */
    protected abstract void revertToSnapshot(T snapshot);

    /**
     * Signals that the snapshot will not be used anymore, and is safe to cache or discard.
     */
    protected void releaseSnapshot(T snapshot) {
    }

    /**
     * Called after the root transaction was successfully committed, to perform irreversible actions such as
     * {@code setChanged()} or neighbor updates.
     *
     * @param originalState state of this journal before the transactional operations.
     */
    protected void onRootCommit(T originalState) {
    }

    /**
     * Updates the stored snapshots so that the changes happening as part of the passed transaction can be correctly
     * committed or rolled back. Call this every time the journal is about to change its state as part of a transaction.
     */
    public final void updateSnapshots(AETransaction transaction) {
        updateSnapshots(FabricTransaction.unwrap(transaction));
    }

    /**
     * @see #updateSnapshots(AETransaction)
     */
    @SuppressWarnings("unchecked")
    public void updateSnapshots(TransactionContext transaction) {
        int currentDepth = transaction.nestingDepth();

        snapshots.ensureCapacity(currentDepth + 1);
        for (int i = snapshots.size(); i <= currentDepth; i++) {
            snapshots.add((T) NO_SNAPSHOT);
        }

        if (snapshots.get(currentDepth) == NO_SNAPSHOT) {
            snapshots.set(currentDepth, createSnapshot());
            transaction.addCloseCallback(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClose(TransactionContext transaction, TransactionContext.Result result) {
        int currentDepth = transaction.nestingDepth();

        // Get and clear the relevant snapshot.
        T snapshot = snapshots.set(currentDepth, (T) NO_SNAPSHOT);

        if (result.wasAborted()) {
            // If the transaction was aborted, we just revert to the state of the snapshot.
            revertToSnapshot(snapshot);
            releaseSnapshot(snapshot);
        } else if (currentDepth <= 0) {
            // The transaction is the root.
            if (originalState == null) {
                originalState = snapshot;
                transaction.addOuterCloseCallback(this);
            } else {
                // An onRootCommit callback is already scheduled: this journal got modified again in a
                // transaction opened from some outer close callback. Just wait for the registered callback.
                releaseSnapshot(snapshot);
            }
        } else if (snapshots.get(currentDepth - 1) == NO_SNAPSHOT) {
            // No snapshot yet, so move the snapshot one depth up.
            snapshots.set(currentDepth - 1, snapshot);
            // This is the first snapshot at this level: register with the parent transaction.
            transaction.getOpenTransaction(currentDepth - 1).addCloseCallback(this);
        } else {
            // There is already an older snapshot at the depth above, just release the newer one.
            releaseSnapshot(snapshot);
        }
    }

    @Override
    public void afterOuterClose(TransactionContext.Result result) {
        // Only scheduled during onClose() when the root transaction is successful.
        T originalState = this.originalState;
        // Clear immediately: onRootCommit might trigger new transactions which re-schedule this journal.
        this.originalState = null;
        onRootCommit(originalState);
        releaseSnapshot(originalState);
    }
}
