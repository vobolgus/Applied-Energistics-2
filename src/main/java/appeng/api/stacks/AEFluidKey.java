package appeng.api.stacks;

import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import io.netty.handler.codec.DecoderException;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import appeng.api.storage.AEKeyFilter;
import appeng.core.AELog;
import appeng.util.fluid.FluidPlatform;

public final class AEFluidKey extends AEKey {
    public static final MapCodec<AEFluidKey> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BuiltInRegistries.FLUID.holderByNameCodec().validate(
                            holder -> holder.is(Fluids.EMPTY.builtInRegistryHolder())
                                    ? DataResult.error(() -> "Fluid must not be minecraft:empty")
                                    : DataResult.success(holder))
                            .fieldOf("id").forGetter(key -> key.fluid),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(key -> key.components.asPatch()))
                    .apply(instance, AEFluidKey::new));
    public static final Codec<AEFluidKey> CODEC = MAP_CODEC.codec();

    /**
     * Network encoding of the fluid holder. Same codec that the NeoForge {@code FluidStack.STREAM_CODEC} used before
     * the loader decoupling; kept for a byte-identical wire format.
     */
    private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Fluid>> FLUID_HOLDER_STREAM_CODEC = ByteBufCodecs
            .holderRegistry(Registries.FLUID);

    public static final int AMOUNT_BUCKET = 1000;
    public static final int AMOUNT_BLOCK = 1000;

    private final Holder<Fluid> fluid;
    /**
     * Built exactly like the component map of a NeoForge {@code FluidStack} (patch applied over the fluid's component
     * prototype), so that hashing and equality remain identical to the previous FluidStack-based implementation.
     */
    private final PatchedDataComponentMap components;
    private final int hashCode;

    private AEFluidKey(Holder<Fluid> fluid, DataComponentPatch componentsPatch) {
        Preconditions.checkArgument(!fluid.is(Fluids.EMPTY.builtInRegistryHolder()), "fluid was empty");
        this.fluid = fluid;
        this.components = PatchedDataComponentMap.fromPatch(fluid.components(), componentsPatch);
        // Same formula as FluidStack.hashFluidAndComponents
        this.hashCode = 31 * (31 + getFluid().hashCode()) + components.hashCode();
    }

    public static AEFluidKey of(Fluid fluid) {
        return of(fluid, DataComponentPatch.EMPTY);
    }

    @Nullable
    public static AEFluidKey of(Fluid fluid, DataComponentPatch components) {
        if (fluid == Fluids.EMPTY) {
            return null;
        }
        return new AEFluidKey(fluid.builtInRegistryHolder(), components);
    }

    public static boolean is(AEKey what) {
        return what instanceof AEFluidKey;
    }

    public static AEKeyFilter filter() {
        return AEFluidKey::is;
    }

    @Override
    public AEKeyType getType() {
        return AEKeyType.fluids();
    }

    @Override
    public AEFluidKey dropSecondary() {
        return of(getFluid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AEFluidKey aeFluidKey = (AEFluidKey) o;
        // The hash code comparison is a fast-fail cheap check
        // Fluid identity + component map equality replicates FluidStack.isSameFluidSameComponents
        return hashCode == aeFluidKey.hashCode && getFluid() == aeFluidKey.getFluid()
                && Objects.equals(components, aeFluidKey.components);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public static AEFluidKey fromTag(ValueInput input) {
        try {
            return input.read(MAP_CODEC).orElseThrow();
        } catch (Exception e) {
            AELog.debug("Tried to load an invalid fluid key from NBT: %s", input, e);
            return null;
        }
    }

    @Override
    public void toTag(ValueOutput output) {
        output.store(MAP_CODEC, this);
    }

    @Override
    public Object getPrimaryKey() {
        return getFluid();
    }

    @Override
    public Identifier getId() {
        return BuiltInRegistries.FLUID.getKey(getFluid());
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, Level level, BlockPos pos) {
        // Fluids are voided
    }

    @Override
    protected Component computeDisplayName() {
        return FluidPlatform.get().getFluidDisplayName(getFluid(), getComponentsPatch());
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isTagged(TagKey<?> tag) {
        // This will just return false for incorrectly cast tags
        return fluid.is((TagKey<Fluid>) tag);
    }

    @Override
    public <T> @Nullable T get(DataComponentType<T> type) {
        return components.get(type);
    }

    @Override
    public boolean hasComponents() {
        return components.isEmpty();
    }

    /**
     * @return The changes to the fluid's default data components represented by this key.
     */
    public DataComponentPatch getComponentsPatch() {
        return components.asPatch();
    }

    public Fluid getFluid() {
        return fluid.value();
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf data) {
        // Byte-identical replication of FluidStack.STREAM_CODEC for an amount-1 stack:
        // varint amount, fluid registry id, component patch
        data.writeVarInt(1);
        FLUID_HOLDER_STREAM_CODEC.encode(data, fluid);
        DataComponentPatch.STREAM_CODEC.encode(data, components.asPatch());
    }

    public static AEFluidKey fromPacket(RegistryFriendlyByteBuf data) {
        // Byte-identical replication of FluidStack.STREAM_CODEC, including its rejection of empty stacks
        int amount = data.readVarInt();
        if (amount <= 0) {
            throw new DecoderException("Empty FluidStack not allowed");
        }
        var holder = FLUID_HOLDER_STREAM_CODEC.decode(data);
        var patch = DataComponentPatch.STREAM_CODEC.decode(data);
        if (holder.is(Fluids.EMPTY.builtInRegistryHolder())) {
            throw new DecoderException("Empty FluidStack not allowed");
        }
        return new AEFluidKey(holder, patch);
    }

    public static boolean is(@Nullable GenericStack stack) {
        return stack != null && stack.what() instanceof AEFluidKey;
    }

    @Override
    public String toString() {
        var id = BuiltInRegistries.FLUID.getKey(getFluid());
        String idString = id != BuiltInRegistries.FLUID.getDefaultKey() ? id.toString()
                : getFluid().getClass().getName() + "(unregistered)";
        return components.isEmpty() ? idString : idString + " (+components)";
    }
}
