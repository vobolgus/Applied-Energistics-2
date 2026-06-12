package appeng.fabric.client;

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import appeng.api.parts.IPart;
import appeng.api.stacks.AEFluidKey;
import appeng.client.ClientLoaderHooks;
import appeng.client.api.model.parts.PartModel;
import appeng.client.api.model.parts.RegisterPartModelsEvent;
import appeng.client.api.renderer.parts.PartRenderer;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;

/**
 * Fabric implementation of the {@link ClientLoaderHooks} seam (mirrors
 * {@code appeng.neoforge.client.NeoForgeClientLoaderHooks}). The addon-facing part model/renderer registration events
 * are driven through the {@code ae2:client_registration} entrypoint ({@link AE2FabricClientRegistration}) instead of
 * NeoForge's mod event bus.
 */
public class FabricClientLoaderHooks implements ClientLoaderHooks {
    public static final String CLIENT_REGISTRATION_ENTRYPOINT = "ae2:client_registration";

    @Override
    public void postRegisterPartModels(
            ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> modelIdMapper) {
        var event = new RegisterPartModelsEvent(modelIdMapper);
        for (var entrypoint : FabricLoader.getInstance()
                .getEntrypoints(CLIENT_REGISTRATION_ENTRYPOINT, AE2FabricClientRegistration.class)) {
            entrypoint.registerPartModels(event);
        }
    }

    @Override
    public void collectPartRenderers(PartRendererCollector collector) {
        for (var container : FabricLoader.getInstance()
                .getEntrypointContainers(CLIENT_REGISTRATION_ENTRYPOINT, AE2FabricClientRegistration.class)) {
            var modId = container.getProvider().getMetadata().getId();
            var event = new RegisterPartRendererEvent(new RegisterPartRendererEvent.PartRegistrationSink() {
                @Override
                public <T extends IPart> void register(Class<T> partClass, PartRenderer<? super T, ?> renderer) {
                    collector.register(modId, partClass, renderer);
                }
            });
            container.getEntrypoint().registerPartRenderers(event);
        }
    }

    @Override
    public InputConstants.Key getBoundKey(KeyMapping keyMapping) {
        return KeyMappingHelper.getBoundKeyOf(keyMapping);
    }

    @Override
    public FluidRenderInfo getFluidRenderInfo(AEFluidKey fluid) {
        var fluidState = fluid.getFluid().defaultFluidState();
        var fluidModel = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        // The vanilla tint source on the legacy block state (GuideME's Fabric precedent); NeoForge instead
        // patches a FluidStack-aware fluidTintSource() into the fluid model.
        var tintSource = fluidModel.tintSource();
        var color = tintSource != null ? tintSource.color(fluidState.createLegacyBlock()) : -1;
        var variant = FluidVariant.of(fluid.getFluid(), fluid.getComponentsPatch());
        return new FluidRenderInfo(
                fluidModel.stillMaterial().sprite(),
                color,
                FluidVariantAttributes.isLighterThanAir(variant));
    }

    @Override
    public void setTooltipForNextFrame(GuiGraphicsExtractor guiGraphics, Font font, List<Component> lines,
            Optional<TooltipComponent> image, ItemStack stack, int x, int y) {
        // The ItemStack-carrying overload is a NeoForge patch; use the vanilla overload here.
        guiGraphics.setTooltipForNextFrame(font, lines, image, x, y);
    }
}
