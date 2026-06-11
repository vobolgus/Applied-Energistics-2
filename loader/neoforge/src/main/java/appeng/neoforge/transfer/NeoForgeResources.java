package appeng.neoforge.transfer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.EmptyResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.CarriedSlotWrapper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;

import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.inventories.InternalInventoryResourceHandler;
import appeng.api.inventories.ItemTransfer;
import appeng.api.inventories.PlatformInventoryWrapper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigMenuInventory;
import appeng.util.inv.CarriedItemInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import appeng.util.inv.SupplierInternalInventory;

/**
 * Static bridges between AE2's loader-neutral key/inventory abstractions and the NeoForge transfer API. These used to
 * be instance methods on {@link AEItemKey}, {@link AEFluidKey}, {@link GenericStack} and {@link InternalInventory}
 * before the core API was decoupled from loader types.
 */
public final class NeoForgeResources {
    private NeoForgeResources() {
    }

    public static ItemResource toResource(AEItemKey key) {
        return ItemResource.of(key.getReadOnlyStack());
    }

    public static FluidResource toResource(AEFluidKey key) {
        return FluidResource.of(toFluidStack(key, 1));
    }

    /**
     * Converts the given fluid key into a {@link FluidStack} of the given amount. Used to be
     * {@code AEFluidKey.toStack(int)}.
     */
    public static FluidStack toFluidStack(AEFluidKey key, int amount) {
        return new FluidStack(key.getFluid().builtInRegistryHolder(), amount, key.getComponentsPatch());
    }

    /**
     * Creates a fluid key from the given fluid stack, ignoring its amount. Returns null if the stack is empty. Used to
     * be {@code AEFluidKey.of(FluidStack)}.
     */
    @Nullable
    public static AEFluidKey of(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return AEFluidKey.of(stack.getFluid(), stack.getComponentsPatch());
    }

    /**
     * Checks if the given key is a fluid key matching the given stack's fluid and components, ignoring the amount. Used
     * to be {@code AEFluidKey.matches(AEKey, FluidStack)}.
     */
    public static boolean matches(@Nullable AEKey what, FluidStack fluid) {
        return what instanceof AEFluidKey fluidKey
                && FluidStack.isSameFluidSameComponents(toFluidStack(fluidKey, 1), fluid);
    }

    @Nullable
    public static AEItemKey of(ItemResource resource) {
        if (resource.isEmpty()) {
            return null;
        }
        return AEItemKey.of(resource.toStack());
    }

    @Nullable
    public static AEFluidKey of(FluidResource resource) {
        if (resource.isEmpty()) {
            return null;
        }
        return of(resource.toStack(1));
    }

    /**
     * Converts a given item resource and amount into a generic stack. If the resource is empty, null is returned.
     */
    @Nullable
    public static GenericStack fromItemResource(ItemResource resource, long amount) {
        var key = of(resource);
        if (key == null) {
            return null;
        }
        return new GenericStack(key, amount);
    }

    /**
     * Converts a given fluid resource and amount into a generic stack. If the resource is empty, null is returned.
     */
    @Nullable
    public static GenericStack fromFluidResource(FluidResource resource, long amount) {
        var key = of(resource);
        if (key == null) {
            return null;
        }
        return new GenericStack(key, amount);
    }

    /**
     * Converts a given fluid stack into a generic stack. If the fluid stack is empty, null is returned. Used to be
     * {@code GenericStack.fromFluidStack}.
     */
    @Nullable
    public static GenericStack fromFluidStack(FluidStack stack) {
        var key = of(stack);
        if (key == null) {
            return null;
        }
        return new GenericStack(key, stack.getAmount());
    }

    /**
     * Exposes an adjacent block's item handler as an {@link ItemTransfer}, if there is one.
     */
    @Nullable
    public static ItemTransfer wrapExternal(Level level, BlockPos pos, Direction side) {
        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
        if (handler != null) {
            return new PlatformInventoryWrapper(handler);
        }
        return null;
    }

    /**
     * Adapts an {@link InternalInventory} to the NeoForge {@link ResourceHandler} interface. Replicates the dispatch
     * that used to be implemented by overriding {@code InternalInventory.toResourceHandler()}. Adapters for
     * {@link BaseInternalInventory} subclasses are cached on the inventory to maintain referential equality over time.
     */
    @SuppressWarnings("unchecked")
    public static ResourceHandler<ItemResource> toResourceHandler(InternalInventory inv) {
        if (inv instanceof SupplierInternalInventory<?> supplier) {
            return toResourceHandler(supplier.getDelegate());
        }
        if (inv instanceof PlatformInventoryWrapper wrapper) {
            return wrapper.getHandler();
        }
        if (inv instanceof PlayerInternalInventory playerInv) {
            return PlayerInventoryWrapper.of(playerInv.getInventory());
        }
        if (inv instanceof CarriedItemInventory carriedInv) {
            return CarriedSlotWrapper.of(carriedInv.getMenu());
        }
        if (inv instanceof ConfigMenuInventory) {
            throw new UnsupportedOperationException();
        }
        if (inv == InternalInventory.empty()) {
            return EmptyResourceHandler.instance();
        }
        if (inv instanceof CombinedInternalInventory combined) {
            return (ResourceHandler<ItemResource>) combined.getOrCreatePlatformAdapter(ignored -> {
                var inventories = combined.getInventories();
                var parts = new ResourceHandler[inventories.length];
                for (int i = 0; i < inventories.length; i++) {
                    parts[i] = toResourceHandler(inventories[i]);
                }
                return new CombinedResourceHandler<>((ResourceHandler<ItemResource>[]) parts);
            });
        }
        if (inv instanceof BaseInternalInventory base) {
            return (ResourceHandler<ItemResource>) base
                    .getOrCreatePlatformAdapter(InternalInventoryResourceHandler::new);
        }
        return new InternalInventoryResourceHandler(inv);
    }
}
