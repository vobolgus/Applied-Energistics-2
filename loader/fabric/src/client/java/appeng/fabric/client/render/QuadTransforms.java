package appeng.fabric.client.render;

import com.mojang.math.Transformation;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import net.minecraft.client.resources.model.geometry.BakedQuad;

/**
 * Fabric mirror of NeoForge's {@code QuadTransforms} helper, reduced to vanilla quads: vanilla 26.1 {@link BakedQuad}s
 * carry no baked normals or colors (NeoForge patches), so only the positions are transformed. Like NeoForge, the quad
 * direction is left untransformed (upstream TODO).
 */
public final class QuadTransforms {
    private QuadTransforms() {
    }

    /**
     * Returns a baked quad with the passed transformation applied.
     */
    public static BakedQuad applyTransformation(BakedQuad quad, Transformation transformation) {
        if (transformation.equals(Transformation.IDENTITY)) {
            return quad;
        }
        var matrix = transformation.getMatrix();
        var posTemp = new Vector4f();
        var result = new BakedQuad(
                transformPosition(posTemp, quad.position0(), matrix),
                transformPosition(posTemp, quad.position1(), matrix),
                transformPosition(posTemp, quad.position2(), matrix),
                transformPosition(posTemp, quad.position3(), matrix),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.direction(),
                quad.materialInfo());
        // Carry any side-channel baked color over to the transformed quad
        QuadColors.put(result, QuadColors.get(quad));
        return result;
    }

    private static Vector3fc transformPosition(Vector4f temp, Vector3fc pos, org.joml.Matrix4fc matrix) {
        temp.set(pos.x(), pos.y(), pos.z(), 1);
        matrix.transform(temp);
        temp.div(temp.w);
        return new Vector3f(temp.x(), temp.y(), temp.z());
    }
}
