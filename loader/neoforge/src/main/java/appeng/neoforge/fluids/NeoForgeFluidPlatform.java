package appeng.neoforge.fluids;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import appeng.blockentity.storage.SkyStoneTankBlockEntity;
import appeng.blockentity.storage.SkyStoneTankFluidHandler;
import appeng.util.fluid.FluidPlatform;

/**
 * NeoForge implementation of the {@link FluidPlatform} seam. Display names are resolved through the NeoForge
 * {@code FluidType} (via {@link FluidStack#getHoverName()}), exactly as before the loader decoupling.
 */
public class NeoForgeFluidPlatform implements FluidPlatform {
    @Override
    public Component getFluidDisplayName(Fluid fluid, DataComponentPatch components) {
        return new FluidStack(fluid.builtInRegistryHolder(), 1, components).getHoverName();
    }

    @Override
    public boolean interactWithTank(Player player, InteractionHand hand, SkyStoneTankBlockEntity tank) {
        return FluidUtil.interactWithFluidHandler(player, hand, tank.getBlockPos(),
                SkyStoneTankFluidHandler.get(tank));
    }

    @Override
    @Nullable
    public SoundEvent getBucketFillSound(Fluid fluid) {
        return fluid.getFluidType().getSound(SoundActions.BUCKET_FILL);
    }
}
