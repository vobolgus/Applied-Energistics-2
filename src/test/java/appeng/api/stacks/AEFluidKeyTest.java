package appeng.api.stacks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;

import appeng.util.BootstrapMinecraft;
import appeng.util.CodecTestUtil;

/**
 * Guards the persistence and network format of {@link AEFluidKey} against regressions from the removal of the NeoForge
 * {@link FluidStack} as its internal representation. The serialized shapes must remain byte-identical to the previous
 * FluidStack-based implementation.
 */
@BootstrapMinecraft
@ExtendWith(EphemeralTestServerProvider.class)
class AEFluidKeyTest {
    private static RegistryAccess registryAccess() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    static DataComponentPatch testPatch() {
        return DataComponentPatch.builder()
                .set(DataComponents.CUSTOM_NAME, Component.literal("Test"))
                .build();
    }

    @Test
    void testJsonRoundtripWithoutComponents(MinecraftServer server) {
        var expected = GsonHelper.parse("{\"id\":\"minecraft:water\"}");
        CodecTestUtil.testRoundtrip(AEFluidKey.CODEC, AEFluidKey.of(Fluids.WATER), JsonOps.INSTANCE, expected);
    }

    @Test
    void testJsonRoundtripWithComponents(MinecraftServer server) {
        var expected = GsonHelper
                .parse("{\"id\":\"minecraft:water\",\"components\":{\"minecraft:custom_name\":\"Test\"}}");
        var key = AEFluidKey.of(Fluids.WATER, testPatch());
        CodecTestUtil.testRoundtrip(AEFluidKey.CODEC, key, JsonOps.INSTANCE, expected);
    }

    /**
     * The packet encoding must be byte-identical to {@code FluidStack.STREAM_CODEC} for an amount-1 stack.
     */
    @Test
    void testPacketEncodingMatchesFluidStackStreamCodec(MinecraftServer server) {
        for (var patch : new DataComponentPatch[] { DataComponentPatch.EMPTY, testPatch() }) {
            var key = AEFluidKey.of(Fluids.LAVA, patch);
            assertNotNull(key);

            var keyBuffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess());
            key.writeToPacket(keyBuffer);
            var keyBytes = new byte[keyBuffer.readableBytes()];
            keyBuffer.getBytes(0, keyBytes);

            var stackBuffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess());
            FluidStack.STREAM_CODEC.encode(stackBuffer, new FluidStack(Fluids.LAVA, 1, patch));
            var stackBytes = new byte[stackBuffer.readableBytes()];
            stackBuffer.getBytes(0, stackBytes);

            assertArrayEquals(stackBytes, keyBytes);

            // And the key must decode from the FluidStack encoding
            assertEquals(key, AEFluidKey.fromPacket(stackBuffer));
            keyBuffer.release();
            stackBuffer.release();
        }
    }

    @Test
    void testFromPacketRejectsEmptyStacks(MinecraftServer server) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess());
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, FluidStack.EMPTY);
        assertThrows(DecoderException.class, () -> AEFluidKey.fromPacket(buffer));
        buffer.release();
    }

    /**
     * Hash codes feed into the iteration order of AE2's key-based hash maps; they must match the previous
     * implementation ({@code FluidStack.hashFluidAndComponents}) exactly.
     */
    @Test
    void testHashCodeMatchesFluidStackHash(MinecraftServer server) {
        for (var patch : new DataComponentPatch[] { DataComponentPatch.EMPTY, testPatch() }) {
            var key = AEFluidKey.of(Fluids.WATER, patch);
            assertNotNull(key);
            assertEquals(FluidStack.hashFluidAndComponents(new FluidStack(Fluids.WATER, 1, patch)), key.hashCode());
        }
    }
}
