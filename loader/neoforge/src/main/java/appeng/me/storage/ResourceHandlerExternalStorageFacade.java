package appeng.me.storage;

import java.util.Set;

import javax.annotation.Nullable;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.neoforge.transfer.NeoForgeResources;

/**
 * Adapts a NeoForge {@link ResourceHandler} to an {@link ExternalStorageFacade}. This used to be a set of nested
 * classes within {@link ExternalStorageFacade} before the shared base class was decoupled from loader types.
 */
public abstract class ResourceHandlerExternalStorageFacade<R extends Resource, K extends AEKey>
        extends ExternalStorageFacade {
    protected final ResourceHandler<R> handler;

    public ResourceHandlerExternalStorageFacade(ResourceHandler<R> handler) {
        this.handler = handler;
    }

    public static ExternalStorageFacade ofFluidHandler(ResourceHandler<FluidResource> handler) {
        return new FluidHandlerFacade(handler);
    }

    public static ExternalStorageFacade ofItemHandler(ResourceHandler<ItemResource> handler) {
        return new ItemHandlerFacade(handler);
    }

    @Override
    public int getSlots() {
        return handler.size();
    }

    @Nullable
    @Override
    public GenericStack getStackInSlot(int slot) {
        K key = toKey(handler.getResource(slot));
        return key == null ? null : new GenericStack(key, handler.getAmountAsLong(slot));
    }

    @Override
    public int insertExternal(AEKey what, int amount, Actionable mode) {
        var resource = toResource(what);
        if (resource == null) {
            return 0;
        }

        try (var tx = Transaction.openRoot()) {
            var inserted = handler.insert(resource, amount, tx);
            if (!mode.isSimulate()) {
                tx.commit();
            }
            return inserted;
        }
    }

    @Override
    public int extractExternal(AEKey what, int amount, Actionable mode) {
        var resource = toResource(what);
        if (resource == null) {
            return 0;
        }

        try (var tx = Transaction.openRoot()) {
            var extracted = handler.extract(resource, amount, tx);
            if (!mode.isSimulate()) {
                tx.commit();
            }
            return extracted;
        }
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (int i = 0; i < handler.size(); i++) {
            // Skip resources that cannot be extracted if that filter was enabled
            var stack = handler.getResource(i);
            if (stack.isEmpty()) {
                continue;
            }

            long amount = handler.getAmountAsLong(i);

            if (extractableOnly) {
                // Try to determine whether the resource is extractable

                try (var tx = Transaction.openRoot()) {
                    var extracted = handler.extract(i, stack, 1, tx);
                    // Try again in case the handler only allows extracting the resource in its entirety (i.e.
                    // cauldrons)
                    if (extracted == 0) {
                        extracted = handler.extract(i, stack, 1, tx);
                    }
                    if (extracted == 0) {
                        continue; // Skip unextractable slots
                    }
                }
            }

            out.add(toKey(stack), amount);
        }
    }

    @Override
    public boolean containsAnyFuzzy(Set<AEKey> keys) {
        for (int i = 0; i < handler.size(); i++) {
            var what = toKey(handler.getResource(i));
            if (what != null) {
                if (keys.contains(what.dropSecondary())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    protected abstract K toKey(R resource);

    @Nullable
    protected abstract R toResource(AEKey key);

    private static class ItemHandlerFacade extends ResourceHandlerExternalStorageFacade<ItemResource, AEItemKey> {
        public ItemHandlerFacade(ResourceHandler<ItemResource> handler) {
            super(handler);
        }

        @Override
        public AEKeyType getKeyType() {
            return AEKeyType.items();
        }

        @Override
        protected @org.jspecify.annotations.Nullable AEItemKey toKey(ItemResource resource) {
            return NeoForgeResources.of(resource);
        }

        @Override
        protected @org.jspecify.annotations.Nullable ItemResource toResource(AEKey key) {
            return (key instanceof AEItemKey itemKey) ? NeoForgeResources.toResource(itemKey) : null;
        }
    }

    private static class FluidHandlerFacade extends ResourceHandlerExternalStorageFacade<FluidResource, AEFluidKey> {
        public FluidHandlerFacade(ResourceHandler<FluidResource> handler) {
            super(handler);
        }

        @Override
        public AEKeyType getKeyType() {
            return AEKeyType.fluids();
        }

        @Override
        protected @org.jspecify.annotations.Nullable AEFluidKey toKey(FluidResource resource) {
            return NeoForgeResources.of(resource);
        }

        @Override
        protected @org.jspecify.annotations.Nullable FluidResource toResource(AEKey key) {
            return (key instanceof AEFluidKey fluidKey) ? NeoForgeResources.toResource(fluidKey) : null;
        }
    }
}
