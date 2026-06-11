package appeng.neoforge.render;

import org.jetbrains.annotations.Nullable;

import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

/**
 * NeoForge bridge for AE2's loader-neutral render-data contract: the single, untyped render-data object produced by
 * {@code AEBaseBlockEntity#getRenderData()} (e.g. {@code appeng.block.networking.CableBusRenderState},
 * {@code appeng.blockentity.crafting.CraftingCubeModelData} or {@link appeng.util.render.AERenderData}) is transported
 * to client-side models as a NeoForge {@link ModelData} containing only the {@link #AE_RENDER_DATA} property.
 */
public final class NeoForgeRenderData {
    /**
     * The single model property under which all AE2 render data is published to NeoForge's model data system.
     */
    public static final ModelProperty<Object> AE_RENDER_DATA = new ModelProperty<>();

    private NeoForgeRenderData() {
    }

    public static ModelData wrap(@Nullable Object renderData) {
        return renderData != null ? ModelData.of(AE_RENDER_DATA, renderData) : ModelData.EMPTY;
    }

    @Nullable
    public static Object unwrap(ModelData modelData) {
        return modelData.get(AE_RENDER_DATA);
    }
}
