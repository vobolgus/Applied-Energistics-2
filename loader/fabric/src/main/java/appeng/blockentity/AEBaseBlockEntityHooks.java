/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2025, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.blockentity;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bridges AE2's loader-neutral render-data contract onto the loader's model-data dispatch. This is a
 * <strong>loader-duplicated</strong> class: this Fabric version implements Fabric's {@link RenderDataBlockEntity}
 * (which happens to use the same {@code getRenderData()} method name) and makes {@link #requestRenderUpdate()} a no-op
 * since Fabric has no client-side model-data cache to refresh.
 */
public abstract class AEBaseBlockEntityHooks extends BlockEntity implements RenderDataBlockEntity {
    public AEBaseBlockEntityHooks(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
    }

    /**
     * Loader-neutral render data for this block entity, passed to the client-side models. Subclasses override this to
     * return an immutable snapshot of their visual state (or null if they have none).
     */
    @Override
    @Nullable
    public Object getRenderData() {
        return null;
    }

    /**
     * Requests the loader's cached render data for this block entity to be refreshed (was
     * {@code requestModelDataUpdate()} on NeoForge). No-op on Fabric, which queries the block entity lazily.
     */
    public final void requestRenderUpdate() {
    }

    /**
     * Called when the chunk containing this block entity is unloaded. On NeoForge this declaration does not exist: the
     * subclass overrides target NeoForge's injected {@code IBlockEntityExtension#onChunkUnloaded} hook instead, whose
     * default is an equivalent no-op.
     * <p>
     * TODO (fabric, Phase 2b): must be invoked from a {@code ServerChunkEvents.CHUNK_UNLOAD} (and client equivalent)
     * handler for all AE2 block entities of the unloading chunk.
     */
    public void onChunkUnloaded() {
    }
}
