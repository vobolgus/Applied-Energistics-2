package appeng.fabric.client.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.minecraft.client.resources.model.geometry.BakedQuad;

/**
 * FRAPI impedance side channel: vanilla 26.1 {@link BakedQuad} carries no per-vertex color (NeoForge patches
 * {@code bakedColors} into it, which AE2's quad pipeline relies on for e.g. cable colors and pre-baked facade tints).
 * The Fabric twins of {@code CubeBuilder}/{@code FacadeBuilder} record the baked color of a quad here (weak identity
 * keys), and the Fabric model twins re-apply it when re-emitting the quad through the Fabric Renderer API (whose mesh
 * format does support per-vertex colors).
 * <p>
 * Quads that reach the vanilla render path directly (without being re-emitted by an AE2 fabric twin) lose the color —
 * see the Phase 3b runtime-risk list in PORTING_NOTES.
 */
public final class QuadColors {
    /**
     * Guava cache with weakKeys uses identity equality, which is what we need (equal-by-content quads may carry
     * different colors).
     */
    private static final Cache<BakedQuad, Integer> BAKED_COLORS = CacheBuilder.newBuilder()
            .weakKeys()
            .build();

    private QuadColors() {
    }

    public static void put(BakedQuad quad, int argb) {
        if (argb != -1) {
            BAKED_COLORS.put(quad, argb);
        }
    }

    /**
     * @return the baked ARGB color for the quad, or -1 (white) if none was recorded.
     */
    public static int get(BakedQuad quad) {
        var color = BAKED_COLORS.getIfPresent(quad);
        return color != null ? color : -1;
    }
}
