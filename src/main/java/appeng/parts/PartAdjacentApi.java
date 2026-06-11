package appeng.parts;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;

import appeng.api.lookup.AEApiCache;
import appeng.api.lookup.AEApiLookup;
import appeng.api.parts.IPartHost;
import appeng.util.Platform;

/**
 * Utility class to cache an API that is adjacent to a part.
 */
public class PartAdjacentApi<T> {
    private final AEBasePart part;
    private final AEApiLookup<T> lookup;
    private final Runnable invalidationListener;
    private AEApiCache<T> cache;

    public PartAdjacentApi(AEBasePart part, AEApiLookup<T> lookup) {
        this(part, lookup, () -> {
        });
    }

    public PartAdjacentApi(AEBasePart part, AEApiLookup<T> lookup, Runnable invalidationListener) {
        this.lookup = lookup;
        this.part = part;
        this.invalidationListener = invalidationListener;
    }

    @Nullable
    public T find() {
        if (!(part.getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }

        var host = part.getHost().getBlockEntity();
        var attachedSide = part.getSide();
        var targetPos = host.getBlockPos().relative(attachedSide);

        if (!Platform.areBlockEntitiesTicking(serverLevel, targetPos)) {
            return null;
        }

        if (cache == null) {
            cache = lookup.createCache(
                    serverLevel,
                    targetPos,
                    attachedSide.getOpposite(),
                    () -> isPartValid(part),
                    invalidationListener);
        }

        return cache.get();
    }

    public static boolean isPartValid(AEBasePart part) {
        var be = part.getBlockEntity();
        return be instanceof IPartHost host && host.getPart(part.getSide()) == part && !be.isRemoved();
    }
}
