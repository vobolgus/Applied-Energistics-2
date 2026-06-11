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

package appeng.client;

import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import appeng.api.parts.IPart;
import appeng.client.api.model.parts.PartModel;
import appeng.client.api.renderer.parts.PartRenderer;

/**
 * Loader-neutral seam for the client-side registration events that AE2 itself <em>posts</em> so that addons can
 * register part models and part renderers. Each loader forwards these to its own addon-facing event/entrypoint
 * mechanism (NeoForge: {@code RegisterPartModelsEvent}/{@code RegisterPartRendererEvent} posted via {@code ModLoader};
 * Fabric: a custom entrypoint). The loader-specific implementation (e.g.
 * {@code appeng.neoforge.client.NeoForgeClientLoaderHooks}) is injected once during mod construction via {@link #init}.
 */
public interface ClientLoaderHooks {
    /**
     * Gives all mods the opportunity to register their part model types into the given id mapper (NeoForge:
     * {@code RegisterPartModelsEvent}).
     */
    void postRegisterPartModels(
            ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> modelIdMapper);

    /**
     * Gives all mods the opportunity to register their part renderers with the given collector (NeoForge:
     * {@code RegisterPartRendererEvent}, dispatched in parallel per mod).
     */
    void collectPartRenderers(PartRendererCollector collector);

    /**
     * Receives part renderer registrations, attributed to the mod that registered them.
     */
    interface PartRendererCollector {
        <T extends IPart> void register(String modId, Class<T> partClass, PartRenderer<? super T, ?> renderer);
    }

    static ClientLoaderHooks get() {
        var instance = Holder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("The loader-specific ClientLoaderHooks have not been initialized yet");
        }
        return instance;
    }

    /**
     * Injects the loader-specific implementation. Must be called exactly once during mod construction.
     */
    static void init(ClientLoaderHooks hooks) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("The ClientLoaderHooks have already been initialized");
        }
        Holder.INSTANCE = hooks;
    }

    /**
     * Internal holder for the injected implementation.
     */
    final class Holder {
        @Nullable
        private static volatile ClientLoaderHooks INSTANCE;

        private Holder() {
        }
    }
}
