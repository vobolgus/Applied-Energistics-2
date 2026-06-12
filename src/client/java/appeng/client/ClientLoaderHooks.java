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

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import appeng.api.parts.IPart;
import appeng.api.stacks.AEFluidKey;
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

    /**
     * The currently bound key of the given key mapping (NeoForge: the {@code KeyMapping#getKey()} patch; Fabric:
     * {@code KeyMappingHelper.getBoundKeyOf}).
     */
    InputConstants.Key getBoundKey(KeyMapping keyMapping);

    /**
     * Resolves the still sprite, tint color and gravity behavior used to render the given fluid (NeoForge: the
     * {@code FluidModel#fluidTintSource()} patch + {@code FluidType}; Fabric: the vanilla {@code FluidModel} tint
     * source + {@code FluidVariantAttributes}).
     */
    FluidRenderInfo getFluidRenderInfo(AEFluidKey fluid);

    /**
     * Render information for a fluid: the still sprite, the ARGB tint color (-1 if untinted) and whether the fluid is
     * lighter than air (gases render top-down).
     */
    record FluidRenderInfo(TextureAtlasSprite sprite, int color, boolean lighterThanAir) {
    }

    /**
     * Schedules an item tooltip for the next frame. On NeoForge this uses the patched overload that carries the
     * {@link ItemStack} so tooltip-event listeners of other mods receive the stack context; the vanilla (Fabric)
     * overload drops the stack parameter.
     */
    void setTooltipForNextFrame(GuiGraphicsExtractor guiGraphics, Font font, List<Component> lines,
            Optional<TooltipComponent> image, ItemStack stack, int x, int y);

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
