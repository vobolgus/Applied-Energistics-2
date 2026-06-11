package appeng.debug;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Loader-neutral seam for pushing external energy (e.g. Forge Energy / Team Reborn Energy) into adjacent blocks. Only
 * used by the {@link EnergyGeneratorBlockEntity debug energy generator}. The loader-specific implementation (e.g.
 * {@code appeng.neoforge.debug.NeoForgeEnergyGenerator}) is injected once during mod construction via {@link #init}.
 */
public interface EnergyGeneratorPlatform {
    /**
     * Pushes up to the given amount of energy into the energy handler of the block at the given position, if there is
     * one.
     *
     * @param targetSide The side of the target block to push into.
     */
    void pushEnergy(Level level, BlockPos targetPos, Direction targetSide, int amount);

    static EnergyGeneratorPlatform get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific EnergyGeneratorPlatform has not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(EnergyGeneratorPlatform platform) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The EnergyGeneratorPlatform has already been initialized");
        }
        Holder.INSTANCE = platform;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile EnergyGeneratorPlatform INSTANCE;

        private Holder() {
        }
    }
}
