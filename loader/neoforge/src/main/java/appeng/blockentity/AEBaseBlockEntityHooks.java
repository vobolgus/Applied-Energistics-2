package appeng.blockentity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

import appeng.neoforge.render.NeoForgeRenderData;

/**
 * NeoForge-specific base class of {@link AEBaseBlockEntity}. It bridges AE2's loader-neutral render-data contract
 * ({@link #getRenderData()} / {@link #requestRenderUpdate()}) to NeoForge's model-data system
 * ({@code IBlockEntityExtension#getModelData()} / {@code IBlockEntityExtension#requestModelDataUpdate()}, which are
 * interface-injected into {@link BlockEntity} and therefore can only be overridden from a class on the block entity's
 * class hierarchy).
 * <p>
 * The Fabric loader module provides a class of the same fully qualified name that instead implements Fabric's
 * {@code RenderDataBlockEntity} by directly returning {@link #getRenderData()}.
 */
public abstract class AEBaseBlockEntityHooks extends BlockEntity {
    public AEBaseBlockEntityHooks(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
    }

    /**
     * Loader-neutral render data for this block entity, passed to the client-side models. Subclasses override this to
     * return an immutable snapshot of their visual state (or null if they have none).
     */
    @Nullable
    public Object getRenderData() {
        return null;
    }

    /**
     * Requests the loader's cached render data for this block entity to be refreshed (was
     * {@code requestModelDataUpdate()}).
     */
    public final void requestRenderUpdate() {
        requestModelDataUpdate();
    }

    @Override
    public final ModelData getModelData() {
        return NeoForgeRenderData.wrap(getRenderData());
    }
}
