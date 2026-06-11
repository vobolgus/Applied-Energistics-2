package appeng.blockentity.storage;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import appeng.api.stacks.AEFluidKey;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.util.fluid.FluidPlatform;

public class SkyStoneTankBlockEntity extends AEBaseBlockEntity {

    public static final int BUCKET_CAPACITY = 16;

    /**
     * Serialization of a non-empty tank slot. Replicates the NBT shape of the NeoForge
     * {@code FluidStack.OPTIONAL_CODEC} ({@code id}, {@code amount}, optional {@code components}) that was used while
     * the tank was a {@code FluidStacksResourceHandler}, to keep saved data byte-identical.
     */
    private record TankContents(AEFluidKey fluid, int amount) {
        static final Codec<TankContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AEFluidKey.MAP_CODEC.forGetter(TankContents::fluid),
                ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(TankContents::amount))
                .apply(instance, TankContents::new));

        // An empty slot is serialized as an empty map, like FluidStack.OPTIONAL_CODEC did
        static final Codec<List<Optional<TankContents>>> LIST_CODEC = ExtraCodecs.optionalEmptyMap(CODEC).listOf();
    }

    /**
     * Key of the slot list within the "tank" child, kept from {@code StacksResourceHandler.VALUE_IO_KEY}.
     */
    private static final String TANK_VALUE_IO_KEY = "stacks";

    @Nullable
    private AEFluidKey storedFluid;
    private int storedAmount;

    // Cached loader-specific fluid handler adapter (e.g. a NeoForge ResourceHandler), see getOrCreateFluidHandler
    @Nullable
    private Object platformFluidHandler;

    public SkyStoneTankBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
    }

    @Override
    public void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        serializeTank(data.child("tank"));
    }

    @Override
    public void loadTag(ValueInput data) {
        super.loadTag(data);
        deserializeTank(data.childOrEmpty("tank"));
    }

    private void serializeTank(ValueOutput output) {
        writeTank(output, storedFluid, storedAmount);
    }

    @VisibleForTesting
    static void writeTank(ValueOutput output, @Nullable AEFluidKey fluid, int amount) {
        var contents = fluid == null ? Optional.<TankContents>empty()
                : Optional.of(new TankContents(fluid, amount));
        output.store(TANK_VALUE_IO_KEY, TankContents.LIST_CODEC, List.of(contents));
    }

    private void deserializeTank(ValueInput input) {
        input.read(TANK_VALUE_IO_KEY, TankContents.LIST_CODEC).ifPresent(slots -> {
            var contents = slots.isEmpty() ? Optional.<TankContents>empty() : slots.getFirst();
            if (contents.isPresent()) {
                storedFluid = contents.get().fluid();
                storedAmount = contents.get().amount();
            } else {
                storedFluid = null;
                storedAmount = 0;
            }
        });
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        return FluidPlatform.get().interactWithTank(player, hand, this);
    }

    /**
     * @return The fluid stored in the tank, or null if it is empty.
     */
    @Nullable
    public AEFluidKey getStoredFluid() {
        return storedFluid;
    }

    /**
     * @return The amount of stored fluid, or 0 if the tank is empty.
     */
    public int getStoredAmount() {
        return storedAmount;
    }

    /**
     * @return The tank capacity for any fluid.
     */
    public int getCapacity() {
        return AEFluidKey.AMOUNT_BUCKET * BUCKET_CAPACITY;
    }

    /**
     * Directly overwrites the tank contents without notifying anyone. Used by the loader-specific fluid handler, which
     * is responsible for calling {@link #onTankContentsChanged()} when a transaction is committed.
     */
    @ApiStatus.Internal
    public void setContents(@Nullable AEFluidKey fluid, int amount) {
        if (fluid == null || amount <= 0) {
            this.storedFluid = null;
            this.storedAmount = 0;
        } else {
            this.storedFluid = fluid;
            this.storedAmount = amount;
        }
    }

    /**
     * Called by the loader-specific fluid handler after the tank contents changed.
     */
    @ApiStatus.Internal
    public void onTankContentsChanged() {
        markForUpdate();
        setChanged();
    }

    /**
     * Returns the cached loader-specific fluid handler adapter for this tank, creating it with the given factory on
     * first use.
     */
    @ApiStatus.Internal
    public Object getOrCreateFluidHandler(Function<? super SkyStoneTankBlockEntity, ?> factory) {
        if (platformFluidHandler == null) {
            platformFluidHandler = factory.apply(this);
        }
        return platformFluidHandler;
    }

    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean ret = super.readFromStream(data);
        var input = TagValueInput.create(ProblemReporter.DISCARDING, data.registryAccess(), data.readNbt());
        deserializeTank(input);
        return ret;
    }

    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, data.registryAccess());
        serializeTank(output);
        data.writeNbt(output.buildResult());
    }
}
