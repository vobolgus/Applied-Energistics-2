package appeng.api.parts;

import java.util.Objects;
import java.util.function.Function;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;

/**
 * NeoForge mod-bus event for registering capabilities provided by parts. Delegates into the loader-neutral
 * {@link PartApiRegistry}.
 */
public class RegisterPartCapabilitiesEvent extends Event implements IModBusEvent {

    final PartApiRegistry registry = new PartApiRegistry();

    /**
     * When using capabilities with a context other than {@link Direction}, you need to register a mapping function for
     * AE2 to get the side from the context. It cannot determine which part on a part host should handle the capability
     * otherwise.
     */
    public <T, C> void registerContext(BlockCapability<T, C> capability, Function<C, Direction> directionGetter) {
        registry.registerContext(capability, directionGetter);
    }

    /**
     * Expose a capability for a part class.
     * <p>
     * When looking for an API instance, providers are queried starting from the class of the part, and then moving up
     * to its superclass, and so on, until a provider returning a nonnull API is found.
     * <p>
     * If the context of the lookup is not {@link Direction}, you need to register a mapping function for your custom
     * context! That must be done before this function is called. Currently, the query will fail silently, but IT WILL
     * throw an exception in the future!
     */
    public <T, C, P extends IPart> void register(BlockCapability<T, C> capability,
            ICapabilityProvider<P, C, T> provider,
            Class<P> partClass) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(partClass, "partClass");
        Objects.requireNonNull(provider, "provider");

        registry.register(capability, provider::getCapability, partClass);
    }

    /**
     * Adds a new type of block entity that will participate in forwarding API lookups to its attached parts.
     */
    public <T extends BlockEntity & IPartHost> void addHostType(BlockEntityType<T> hostType) {
        registry.addHostType(hostType);
    }
}
