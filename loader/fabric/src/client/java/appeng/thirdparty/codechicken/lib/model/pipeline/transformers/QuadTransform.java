// Fabric twin: ported from the NeoForge MutableQuad version onto FRAPI's MutableQuadView
// (math is verbatim; only the quad accessor API differs).
package appeng.thirdparty.codechicken.lib.model.pipeline.transformers;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;

public interface QuadTransform {
    boolean transform(MutableQuadView quad);
}
