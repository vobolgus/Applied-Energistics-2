package appeng.util.render;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * An immutable, loader-neutral set of {@link AERenderProperty} values, mirroring the shape of NeoForge's
 * {@code ModelData}. Used for render data that is contributed incrementally by several parties (cable bus parts via
 * {@link appeng.api.parts.IPart#collectRenderData}); block entities with a fixed set of render data return their own
 * immutable state object from {@code AEBaseBlockEntity#getRenderData()} instead.
 * <p>
 * Unlike {@code ModelData}, equality is content-based, since render data participates in the equality of
 * {@code appeng.block.networking.CableBusRenderState}, which is used as a model cache key.
 */
public final class AERenderData {
    public static final AERenderData EMPTY = new AERenderData(Map.of());

    private final Map<AERenderProperty<?>, Object> properties;

    private AERenderData(Map<AERenderProperty<?>, Object> properties) {
        this.properties = properties;
    }

    public boolean has(AERenderProperty<?> property) {
        return properties.containsKey(property);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(AERenderProperty<T> property) {
        return (T) properties.get(property);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static <T> AERenderData of(AERenderProperty<T> property, T value) {
        return builder().with(property, value).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof AERenderData that && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    public static final class Builder {
        // AERenderProperty uses identity-based hashCode/equals, values use content equality
        private final Map<AERenderProperty<?>, Object> properties = new HashMap<>();

        private Builder() {
        }

        public <T> Builder with(AERenderProperty<T> property, T value) {
            if (value == null) {
                throw new IllegalArgumentException("Render data value for " + property + " must not be null");
            }
            properties.put(property, value);
            return this;
        }

        public AERenderData build() {
            if (properties.isEmpty()) {
                return EMPTY;
            }
            return new AERenderData(Map.copyOf(properties));
        }
    }
}
