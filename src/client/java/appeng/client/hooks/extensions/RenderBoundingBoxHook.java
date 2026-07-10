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

package appeng.client.hooks.extensions;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Client-side twin of the {@code appeng.hooks.extensions} shims (see its {@code package-info}): declares NeoForge's
 * injected {@code IBlockEntityRendererExtension#getRenderBoundingBox} with NeoForge's exact default body. On NeoForge,
 * an implementor's override simultaneously overrides the injected extension method, keeping behavior identical.
 * <p>
 * Fabric does not dispatch this hook. Minecraft 26.1 extracts block entities from already-visible render sections and
 * performs no per-block-entity bounding-box frustum test, so the larger SkyStoneChest box is not needed there.
 */
public interface RenderBoundingBoxHook<T extends BlockEntity> {
    default AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }
}
