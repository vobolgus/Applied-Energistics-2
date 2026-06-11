package appeng.blockentity.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

import appeng.api.stacks.AEFluidKey;
import appeng.util.BootstrapMinecraft;

/**
 * The NBT shape written by the sky stone tank must remain identical to the previous
 * {@code FluidStacksResourceHandler}-based serialization: a {@code stacks} list of {@code FluidStack.OPTIONAL_CODEC}
 * entries (empty slots as {@code {}}).
 */
@BootstrapMinecraft
@ExtendWith(EphemeralTestServerProvider.class)
class SkyStoneTankSerializationTest {
    private static DataComponentPatch testPatch() {
        return DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal("Test"))
                .build();
    }

    @Test
    void testEmptyTankShape(MinecraftServer server) {
        var expected = encodeReference(FluidStack.EMPTY);
        assertEquals(expected, encodeTank(null, 0));
    }

    @Test
    void testFilledTankShape(MinecraftServer server) {
        var expected = encodeReference(new FluidStack(Fluids.WATER, 5000));
        assertEquals(expected, encodeTank(AEFluidKey.of(Fluids.WATER), 5000));
    }

    @Test
    void testFilledTankWithComponentsShape(MinecraftServer server) {
        var expected = encodeReference(new FluidStack(Fluids.LAVA.builtInRegistryHolder(), 123, testPatch()));
        assertEquals(expected, encodeTank(AEFluidKey.of(Fluids.LAVA, testPatch()), 123));
    }

    /**
     * Encodes a single-slot stack list the way the previous NeoForge {@code StacksResourceHandler} serialization did.
     */
    private static Tag encodeReference(FluidStack stack) {
        return FluidStack.OPTIONAL_CODEC.listOf().encodeStart(NbtOps.INSTANCE, List.of(stack)).getOrThrow();
    }

    private static Tag encodeTank(@Nullable AEFluidKey fluid, int amount) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        SkyStoneTankBlockEntity.writeTank(output, fluid, amount);
        return output.buildResult().get("stacks");
    }
}
