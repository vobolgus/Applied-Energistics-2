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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;

import appeng.api.parts.IPartItem;
import appeng.fabric.transfer.FabricTransaction;
import appeng.fabric.transfer.VariantConversion;

/**
 * Base class for item/fluid P2P tunnels over the Fabric {@link Storage} API. The Fabric twin of the NeoForge
 * {@code ResourceHandlerP2PTunnelPart}. Transport costs are deducted in AE2-internal amounts (the
 * {@link VariantConversion} converts droplets to millibuckets for fluids).
 */
public abstract class StorageP2PTunnelPart<P extends StorageP2PTunnelPart<P, V>, V extends TransferVariant<?>>
        extends CapabilityP2PTunnelPart<P, Storage<V>> {

    private final VariantConversion<V> conversion;

    public StorageP2PTunnelPart(IPartItem<?> partItem,
            BlockApiLookup<Storage<V>, Direction> lookup,
            VariantConversion<V> conversion) {
        super(partItem, lookup);
        this.inputHandler = new InputStorage();
        this.outputHandler = new OutputStorage();
        this.emptyHandler = Storage.empty();
        this.conversion = conversion;
    }

    private class InputStorage implements InsertionOnlyStorage<V> {
        @Override
        public long insert(V resource, long maxAmount, TransactionContext tx) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            long total = 0;

            var outputs = getOutputs();
            final int outputTunnels = outputs.size();
            final long amount = maxAmount;

            if (outputTunnels == 0 || amount == 0) {
                return 0;
            }

            final long amountPerOutput = amount / outputTunnels;
            long overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

            for (var target : outputs) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final Storage<V> output = capabilityGuard.get();
                    final long toSend = amountPerOutput + overflow;

                    final long received = output.insert(resource, toSend, tx);

                    overflow = toSend - received;
                    total += received;
                }
            }

            deductTransportCost(conversion.toAeAmount(total), conversion.getKeyType(), FabricTransaction.of(tx));
            return total;
        }
    }

    private class OutputStorage implements Storage<V> {
        @Override
        public boolean supportsInsertion() {
            return false;
        }

        @Override
        public long insert(V resource, long maxAmount, TransactionContext transaction) {
            return 0; // This only allows extraction
        }

        @Override
        public long extract(V resource, long maxAmount, TransactionContext tx) {
            try (CapabilityGuard input = getInputCapability()) {
                long extracted = input.get().extract(resource, maxAmount, tx);
                deductTransportCost(conversion.toAeAmount(extracted), conversion.getKeyType(),
                        FabricTransaction.of(tx));
                return extracted;
            }
        }

        @Override
        public Iterator<StorageView<V>> iterator() {
            // Materialize the views while the recursion guard is held; the wrapped views deduct
            // transport cost on extraction like the slot-wise NeoForge twin does.
            List<StorageView<V>> views = new ArrayList<>();
            try (CapabilityGuard input = getInputCapability()) {
                for (var view : input.get()) {
                    views.add(new OutputView(view));
                }
            }
            return views.iterator();
        }

        private class OutputView implements StorageView<V> {
            private final StorageView<V> delegate;

            OutputView(StorageView<V> delegate) {
                this.delegate = delegate;
            }

            @Override
            public long extract(V resource, long maxAmount, TransactionContext tx) {
                long extracted = delegate.extract(resource, maxAmount, tx);
                deductTransportCost(conversion.toAeAmount(extracted), conversion.getKeyType(),
                        FabricTransaction.of(tx));
                return extracted;
            }

            @Override
            public boolean isResourceBlank() {
                return delegate.isResourceBlank();
            }

            @Override
            public V getResource() {
                return delegate.getResource();
            }

            @Override
            public long getAmount() {
                return delegate.getAmount();
            }

            @Override
            public long getCapacity() {
                return delegate.getCapacity();
            }

            @Override
            public StorageView<V> getUnderlyingView() {
                return delegate.getUnderlyingView();
            }
        }
    }
}
