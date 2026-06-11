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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.me.storage.ExternalStorageFacade;

/**
 * Adapts a Fabric {@link Storage} to an {@link ExternalStorageFacade}. The Fabric twin of
 * {@code appeng.me.storage.ResourceHandlerExternalStorageFacade}. All amounts crossing this boundary are converted
 * between AE2-internal and platform amounts through the {@link VariantConversion}.
 */
public class FabricStorageFacade<V extends TransferVariant<?>> extends ExternalStorageFacade {
    private final Storage<V> storage;
    private final VariantConversion<V> conversion;

    public FabricStorageFacade(Storage<V> storage, VariantConversion<V> conversion) {
        this.storage = storage;
        this.conversion = conversion;
    }

    @Override
    public AEKeyType getKeyType() {
        return conversion.getKeyType();
    }

    @Override
    public int getSlots() {
        return currentViews().size();
    }

    @Nullable
    @Override
    public GenericStack getStackInSlot(int slot) {
        var views = currentViews();
        if (slot >= views.size()) {
            return null;
        }
        var view = views.get(slot);
        var key = conversion.getKey(view.getResource());
        return key == null ? null : new GenericStack(key, conversion.toAeAmount(view.getAmount()));
    }

    @Override
    protected int insertExternal(AEKey what, int amount, Actionable mode) {
        var variant = conversion.getVariant(what);
        if (variant.isBlank()) {
            return 0;
        }

        try (var tx = Transaction.openOuter()) {
            var inserted = storage.insert(variant, conversion.toPlatformAmount(amount), tx);
            if (!mode.isSimulate()) {
                tx.commit();
            }
            return (int) conversion.toAeAmount(inserted);
        }
    }

    @Override
    protected int extractExternal(AEKey what, int amount, Actionable mode) {
        var variant = conversion.getVariant(what);
        if (variant.isBlank()) {
            return 0;
        }

        try (var tx = Transaction.openOuter()) {
            var extracted = storage.extract(variant, conversion.toPlatformAmount(amount), tx);
            if (!mode.isSimulate()) {
                tx.commit();
            }
            return (int) conversion.toAeAmount(extracted);
        }
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (var view : storage.nonEmptyViews()) {
            var resource = view.getResource();
            long amount = view.getAmount();

            if (extractableOnly) {
                // Try to determine whether the resource is extractable. A separate transaction per slot
                // (mirroring the NeoForge twin) that is never committed, so the probe has no visible effect.
                try (var tx = Transaction.openOuter()) {
                    var extracted = view.extract(resource, conversion.toPlatformAmount(1), tx);
                    if (extracted == 0) {
                        continue; // Skip unextractable slots
                    }
                }
            }

            var key = conversion.getKey(resource);
            if (key != null) {
                out.add(key, conversion.toAeAmount(amount));
            }
        }
    }

    @Override
    public boolean containsAnyFuzzy(Set<AEKey> keys) {
        for (var view : storage.nonEmptyViews()) {
            var what = conversion.getKey(view.getResource());
            if (what != null && keys.contains(what.dropSecondary())) {
                return true;
            }
        }
        return false;
    }

    private List<StorageView<V>> currentViews() {
        var result = new ArrayList<StorageView<V>>();
        for (var view : storage) {
            result.add(view);
        }
        return result;
    }
}
