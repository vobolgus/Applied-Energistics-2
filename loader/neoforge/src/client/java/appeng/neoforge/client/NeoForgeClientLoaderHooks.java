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

package appeng.neoforge.client;

import com.mojang.serialization.MapCodec;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModWorkManager;

import appeng.api.parts.IPart;
import appeng.client.ClientLoaderHooks;
import appeng.client.api.model.parts.PartModel;
import appeng.client.api.model.parts.RegisterPartModelsEvent;
import appeng.client.api.renderer.parts.PartRenderer;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;

/**
 * NeoForge implementation of the {@link ClientLoaderHooks} seam: posts the addon-facing {@link RegisterPartModelsEvent}
 * and {@link RegisterPartRendererEvent} on the mod event buses, exactly as the shared code did before the loader
 * decoupling.
 */
public class NeoForgeClientLoaderHooks implements ClientLoaderHooks {
    @Override
    public void postRegisterPartModels(
            ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> modelIdMapper) {
        ModLoader.postEvent(new RegisterPartModelsEvent(modelIdMapper));
    }

    @Override
    public void collectPartRenderers(PartRendererCollector collector) {
        ModLoader.dispatchParallelEvent(
                "Collect Part Renderers",
                ModWorkManager.syncExecutor(),
                ModWorkManager.parallelExecutor(),
                () -> {
                },
                (modContainer, deferredWorkQueue) -> new RegisterPartRendererEvent(modContainer, deferredWorkQueue,
                        new RegisterPartRendererEvent.PartRegistrationSink() {
                            @Override
                            public <T extends IPart> void register(Class<T> partClass,
                                    PartRenderer<? super T, ?> renderer) {
                                collector.register(modContainer.getModId(), partClass, renderer);
                            }
                        }));
    }
}
