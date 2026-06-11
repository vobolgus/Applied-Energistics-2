package appeng.util.render;

/**
 * A typed, identity-based key for values stored in {@link AERenderData}.
 * <p>
 * Loader-neutral replacement for NeoForge's {@code ModelProperty}: block entities and cable bus parts build their
 * render data using these keys in shared code, while the loader-specific layer transports the resulting
 * {@link AERenderData} to the client-side models (on NeoForge as the single {@code ModelData} property
 * {@code appeng.neoforge.render.NeoForgeRenderData#AE_RENDER_DATA}).
 *
 * @param <T> type of the associated value
 */
public final class AERenderProperty<T> {
    private final String debugName;

    public AERenderProperty(String debugName) {
        this.debugName = debugName;
    }

    @Override
    public String toString() {
        return "AERenderProperty[" + debugName + "]";
    }
}
