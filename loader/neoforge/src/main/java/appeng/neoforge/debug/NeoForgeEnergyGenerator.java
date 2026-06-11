package appeng.neoforge.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import appeng.debug.EnergyGeneratorPlatform;

/**
 * NeoForge implementation of the {@link EnergyGeneratorPlatform} seam: pushes Forge Energy into adjacent energy
 * capabilities. This used to be inlined in {@code EnergyGeneratorBlockEntity.serverTick}.
 */
public class NeoForgeEnergyGenerator implements EnergyGeneratorPlatform {
    @Override
    public void pushEnergy(Level level, BlockPos targetPos, Direction targetSide, int amount) {
        var consumer = level.getCapability(Capabilities.Energy.BLOCK, targetPos, targetSide);
        if (consumer != null) {
            try (var tx = Transaction.open(null)) {
                consumer.insert(amount, tx);
                tx.commit();
            }
        }
    }
}
