package appeng.api.parts;

import java.util.Set;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class RegisterPartCapabilitiesEventInternal {
    private RegisterPartCapabilitiesEventInternal() {
    }

    public static void register(RegisterPartCapabilitiesEvent partEvent, RegisterCapabilitiesEvent event) {

        for (var registration : partEvent.registry.getRegistrations()) {
            register(event, registration, partEvent.registry.getHostTypes());
        }

    }

    @SuppressWarnings("unchecked")
    private static <T, C> void register(RegisterCapabilitiesEvent event,
            PartApiRegistry.Registration<T, C> registration,
            Set<BlockEntityType<? extends IPartHost>> hostTypes) {
        var capability = (BlockCapability<T, C>) registration.capability();
        ICapabilityProvider<IPartHost, C, T> provider = registration::find;
        for (var hostType : hostTypes) {
            event.registerBlockEntity(capability, hostType, provider);
        }
    }
}
