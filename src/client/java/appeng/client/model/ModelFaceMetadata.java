package appeng.client.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ModelFaceMetadata {
    private ModelFaceMetadata() {
    }

    public static Map<Identifier, Set<Identifier>> loadEmissiveTextures(ResourceManager resourceManager,
            Predicate<Identifier> modelFilter, Logger logger, String modelDescription) {
        var modelConverter = FileToIdConverter.json("models");
        var models = new HashMap<Identifier, JsonObject>();
        for (var entry : modelConverter.listMatchingResources(resourceManager).entrySet()) {
            var modelId = modelConverter.fileToId(entry.getKey());
            try (var reader = entry.getValue().openAsReader()) {
                models.put(modelId, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException | RuntimeException e) {
                logger.warn("Failed to read {} model {}", modelDescription, modelId, e);
            }
        }

        var result = new HashMap<Identifier, Set<Identifier>>();
        for (var modelId : models.keySet()) {
            if (!modelFilter.test(modelId)) {
                continue;
            }
            var textures = findEmissiveTextures(modelId, models);
            if (!textures.isEmpty()) {
                result.put(modelId, textures);
            }
        }
        return Map.copyOf(result);
    }

    static Set<Identifier> findEmissiveTextures(Identifier modelId, Map<Identifier, JsonObject> models) {
        var chain = new ArrayList<JsonObject>();
        var visited = new HashSet<Identifier>();
        var currentId = modelId;
        JsonObject geometryModel = null;
        while (currentId != null && visited.add(currentId)) {
            var model = models.get(currentId);
            if (model == null) {
                break;
            }
            chain.add(model);
            if (geometryModel == null && model.has("elements") && model.get("elements").isJsonArray()) {
                geometryModel = model;
            }
            currentId = readParent(model);
        }
        if (geometryModel == null) {
            return Set.of();
        }

        var textures = new HashMap<String, String>();
        for (var i = chain.size() - 1; i >= 0; i--) {
            addTextures(chain.get(i), textures);
        }
        return findEmissiveTextures(geometryModel, textures);
    }

    static Set<Identifier> findEmissiveTextures(JsonObject model) {
        var textures = new HashMap<String, String>();
        addTextures(model, textures);
        return findEmissiveTextures(model, textures);
    }

    private static void addTextures(JsonObject model, Map<String, String> textures) {
        if (!model.has("textures") || !model.get("textures").isJsonObject()) {
            return;
        }
        for (var entry : model.getAsJsonObject("textures").entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                textures.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
    }

    private static Set<Identifier> findEmissiveTextures(JsonObject geometryModel, Map<String, String> textures) {
        if (!geometryModel.has("elements") || !geometryModel.get("elements").isJsonArray()) {
            return Set.of();
        }

        var emissive = new HashSet<Identifier>();
        var nonEmissive = new HashSet<Identifier>();
        for (var element : geometryModel.getAsJsonArray("elements")) {
            if (!element.isJsonObject() || !element.getAsJsonObject().has("faces")) {
                continue;
            }
            for (var face : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                if (!face.getValue().isJsonObject()) {
                    continue;
                }
                var faceData = face.getValue().getAsJsonObject();
                if (!faceData.has("texture")) {
                    continue;
                }
                var texture = resolveTexture(textures, faceData.get("texture").getAsString());
                if (texture != null) {
                    (isFullbright(faceData) ? emissive : nonEmissive).add(texture);
                }
            }
        }
        emissive.removeAll(nonEmissive);
        return Set.copyOf(emissive);
    }

    private static boolean isFullbright(JsonObject face) {
        if (!face.has("neoforge_data") || !face.get("neoforge_data").isJsonObject()) {
            return false;
        }
        var data = face.getAsJsonObject("neoforge_data");
        return data.has("block_light") && data.get("block_light").getAsInt() == 15
                && data.has("sky_light") && data.get("sky_light").getAsInt() == 15;
    }

    @Nullable
    private static Identifier readParent(JsonObject model) {
        if (!model.has("parent") || !model.get("parent").isJsonPrimitive()) {
            return null;
        }
        return Identifier.tryParse(model.get("parent").getAsString());
    }

    @Nullable
    private static Identifier resolveTexture(Map<String, String> textures, String texture) {
        var resolved = texture;
        var visited = new HashSet<String>();
        while (resolved.startsWith("#")) {
            var key = resolved.substring(1);
            if (!visited.add(key)) {
                return null;
            }
            resolved = textures.get(key);
            if (resolved == null) {
                return null;
            }
        }
        return Identifier.tryParse(resolved);
    }
}
