package appeng.client.model;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;

import appeng.api.implementations.parts.ICablePart;
import appeng.api.parts.IPartItem;
import appeng.client.ClientLoaderHooks;
import appeng.client.api.model.parts.ClientPart;
import appeng.client.api.model.parts.PartModel;

public final class PartModels {
    private static final Logger LOG = LoggerFactory.getLogger(PartModels.class);

    private static final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends PartModel.Unbaked>> PART_MODEL_IDS = new ExtraCodecs.LateBoundIdMapper<>();
    private static final MapCodec<PartModel.Unbaked> MAP_CODEC = PART_MODEL_IDS.codec(Identifier.CODEC)
            .dispatchMap(PartModel.Unbaked::codec, c -> c);
    public static final Codec<PartModel.Unbaked> CODEC = MAP_CODEC.codec();

    private Map<Identifier, ClientPart> clientParts = null;
    private Map<Identifier, Set<Identifier>> emissiveTextures = Map.of();

    public PartModels() {
        ClientLoaderHooks.get().postRegisterPartModels(PART_MODEL_IDS);
    }

    public CompletableFuture<Void> reload(ResourceManager resourceManager, Executor executor) {
        var fileToIdConverter = FileToIdConverter.json("ae2/parts");
        return CompletableFuture.supplyAsync(() -> {
            var clientParts = new HashMap<Identifier, ClientPart>();
            SimpleJsonResourceReloadListener.scanDirectory(
                    resourceManager,
                    fileToIdConverter,
                    JsonOps.INSTANCE,
                    ClientPart.CODEC,
                    clientParts);
            return new ReloadResult(clientParts, loadEmissiveTextures(resourceManager));
        }, executor)
                .thenAccept(result -> {
                    this.clientParts = result.clientParts;
                    this.emissiveTextures = result.emissiveTextures;
                    LOG.info("Loaded emissive face metadata for {} part models", emissiveTextures.size());

                    for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                        var item = entry.getValue();
                        if (item instanceof IPartItem<?> partItem) {
                            // Skip cables, those are special
                            if (ICablePart.class.isAssignableFrom(partItem.getPartClass())) {
                                continue;
                            }

                            var itemId = entry.getKey().identifier();
                            var modelId = fileToIdConverter.idToFile(itemId);
                            if (!this.clientParts.containsKey(itemId)) {
                                LOG.warn("No part model loaded for part item ID {}. Expected at {}", itemId, modelId);
                            }
                        }
                    }
                });
    }

    public static BlockStateModelPart bake(ModelBaker baker, Identifier model, ModelState modelState) {
        var bakedModel = SimpleModelWrapper.bake(baker, model, modelState);
        var emissiveTextures = appeng.client.AppEngClient.instance().getPartModels().emissiveTextures
                .getOrDefault(model, Set.of());
        return ClientLoaderHooks.get().applyPartModelFaceMetadata(bakedModel, emissiveTextures);
    }

    private static Map<Identifier, Set<Identifier>> loadEmissiveTextures(ResourceManager resourceManager) {
        return ModelFaceMetadata.loadEmissiveTextures(resourceManager,
                modelId -> modelId.getPath().startsWith("part/"), LOG, "part");
    }

    private record ReloadResult(Map<Identifier, ClientPart> clientParts,
            Map<Identifier, Set<Identifier>> emissiveTextures) {
    }

    @Nullable
    public PartModel.Unbaked getPartModel(Identifier id) {
        var clientPart = clientParts.get(id);
        return clientPart != null ? clientPart.model() : null;
    }

    public Map<IPartItem<?>, PartModel.Unbaked> getUnbaked() {
        var result = new IdentityHashMap<IPartItem<?>, PartModel.Unbaked>(this.clientParts.size());

        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (entry.getValue() instanceof IPartItem<?> partItem) {
                var model = getPartModel(entry.getKey().identifier());
                if (model != null) {
                    result.put(partItem, model);
                }
            }
        }

        return result;
    }

    public void resolveDependencies(ResolvableModel.Resolver resolver) {
        if (clientParts == null) {
            throw new IllegalStateException("Part models have not been initialized yet.");
        }
        for (var clientPart : clientParts.values()) {
            clientPart.model().resolveDependencies(resolver);
        }
    }
}
