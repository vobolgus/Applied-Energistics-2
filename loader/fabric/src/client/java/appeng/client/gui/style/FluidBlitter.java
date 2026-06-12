/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.client.gui.style;

import appeng.api.stacks.AEFluidKey;
import appeng.client.ClientLoaderHooks;

/**
 * Creates a {@link Blitter} to draw fluids into the user interface.
 * <p>
 * Fabric twin of the NeoForge class with the same FQN; the {@code FluidStack}-based overload does not exist here, and
 * sprite/tint resolution goes through the {@link ClientLoaderHooks} fluid-render seam.
 */
public final class FluidBlitter {

    private FluidBlitter() {
    }

    public static Blitter create(AEFluidKey fluidKey) {
        var renderInfo = ClientLoaderHooks.get().getFluidRenderInfo(fluidKey);
        return Blitter.sprite(renderInfo.sprite())
                .colorRgb(renderInfo.color())
                // Most fluid texture have transparency, but we want an opaque slot
                .blending(false);
    }

}
