package appeng.fabric.client.render;

import com.mojang.math.Transformation;

import org.joml.Matrix4fc;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.core.Direction;

/**
 * Fabric mirror of NeoForge's {@code ComposedModelState}: an implementation of {@link ModelState} which prepends an
 * additional transform onto the incoming {@link ModelState}.
 */
public final class ComposedModelState implements ModelState {
    private final ModelState parent;
    private final Transformation transformation;

    public ComposedModelState(ModelState parent, Transformation transformation) {
        this.parent = parent;
        this.transformation = parent.transformation().compose(transformation);
    }

    @Override
    public Transformation transformation() {
        return transformation;
    }

    @Override
    public Matrix4fc faceTransformation(Direction side) {
        return parent.faceTransformation(side);
    }

    @Override
    public Matrix4fc inverseFaceTransformation(Direction side) {
        return parent.inverseFaceTransformation(side);
    }
}
