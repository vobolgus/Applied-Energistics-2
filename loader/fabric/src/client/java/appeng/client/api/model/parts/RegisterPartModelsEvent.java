package appeng.client.api.model.parts;

import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

/**
 * Fabric twin of the NeoForge event with the same FQN. On Fabric this is a plain callback object passed to the
 * {@code ae2:client_registration} entrypoint ({@code appeng.fabric.client.AE2FabricClientRegistration}) instead of
 * being posted on a mod event bus.
 */
public class RegisterPartModelsEvent {
    private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> modelIdMapper;

    @ApiStatus.Internal
    public RegisterPartModelsEvent(
            ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> modelIdMapper) {
        this.modelIdMapper = modelIdMapper;
    }

    public void registerModelType(Identifier type, MapCodec<? extends PartModel.Unbaked> codec) {
        this.modelIdMapper.put(type, codec);
    }
}
