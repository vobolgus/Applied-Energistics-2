package appeng.fabric.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fabric mirror of NeoForge's {@code DynamicBlockStateModel} convenience interface: block state models implement the
 * world-aware {@link #collectParts(BlockAndTintGetter, BlockPos, BlockState, RandomSource, List)} overload; the
 * context-free vanilla overload delegates with empty context (exactly like NeoForge's default), and the FRAPI-injected
 * {@code FabricBlockStateModel#emitQuads} drives the world-aware overload and emits the collected parts.
 */
public interface FabricDynamicBlockStateModel extends BlockStateModel {
    @Override
    default void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        collectParts(BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), random, parts);
    }

    void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
            List<BlockStateModelPart> parts);

    @Override
    default void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state,
            RandomSource random, Predicate<net.minecraft.core.Direction> cullTest) {
        var parts = new ArrayList<BlockStateModelPart>();
        collectParts(blockView, pos, state, random, parts);
        for (var part : parts) {
            part.emitQuads(emitter, cullTest);
        }
    }
}
