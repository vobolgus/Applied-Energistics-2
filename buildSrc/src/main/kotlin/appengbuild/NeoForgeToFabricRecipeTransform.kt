package appengbuild

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

/**
 * Datagen-parity transform for the generated recipe JSONs (see PORTING_NOTES.md, "Phase 4 datagen
 * parity"). The generated tree (`src/generated/resources`) is produced by the NeoForge datagen and is
 * kept byte-identical to upstream; NeoForge-specific JSON constructs are mapped to their Fabric API
 * equivalents on copy (wired into `processResources` in `loader/fabric/build.gradle.kts`):
 *
 *  - top-level `neoforge:conditions` -> `fabric:load_conditions` (fabric-resource-conditions-api-v1).
 *    AE2's datagen only ever emits `not(tag_empty(<item tag>))`, which maps to the positive
 *    `fabric:tags_populated` condition; its `registry` field is omitted because it defaults to
 *    `minecraft:item` - exactly the registry `neoforge:tag_empty` checks. Any other condition shape
 *    fails the build so upstream drift is caught here instead of at runtime.
 *
 *  - `"neoforge:ingredient_type": "neoforge:difference"` -> `"fabric:type": "fabric:difference"`
 *    (fabric-recipe-api-v1 custom ingredients). The payload fields (`base`/`subtracted`, both
 *    vanilla `Ingredient.CODEC`) are identical on the two loaders, so only the dispatch key/type
 *    id change. Any other custom ingredient type fails the build.
 *
 *  - guard: any remaining `neoforge:`-namespaced map key fails the build.
 *
 * History: this class started as a Groovy `FilterReader` defined inline in
 * `loader/fabric/build.gradle`; it moved here (first as Java, then Kotlin) when the build scripts
 * were converted to the Kotlin DSL, which cannot define Groovy classes. JSON handling is via Gson
 * instead of Groovy's JsonSlurper/JsonOutput. Behavior is identical except for cosmetic
 * pretty-printing (Gson indents with 2 spaces, JsonOutput used 4).
 */
class NeoForgeToFabricRecipeTransform(input: Reader) : FilterReader(StringReader(transform(input.readText()))) {

    companion object {
        private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

        internal fun transform(json: String): String {
            var root: JsonElement = rewriteIngredients(JsonParser.parseString(json))
            if (root is JsonObject && root.has("neoforge:conditions")) {
                val remapped = JsonObject()
                val conditions = JsonArray()
                for (condition in root.getAsJsonArray("neoforge:conditions")) {
                    conditions.add(mapCondition(condition))
                }
                remapped.add("fabric:load_conditions", conditions)
                for ((key, value) in root.entrySet()) {
                    if (key != "neoforge:conditions") {
                        remapped.add(key, value)
                    }
                }
                root = remapped
            }
            assertNoNeoForgeKeys(root)
            return GSON.toJson(root)
        }

        private fun mapCondition(condition: JsonElement): JsonObject {
            if (condition is JsonObject
                && isString(condition.get("type"), "neoforge:not")
            ) {
                val value = condition.get("value")
                if (value is JsonObject && isString(value.get("type"), "neoforge:tag_empty")) {
                    val mapped = JsonObject()
                    mapped.addProperty("condition", "fabric:tags_populated")
                    val values = JsonArray()
                    values.add(value.get("tag"))
                    mapped.add("values", values)
                    return mapped
                }
            }
            throw IllegalStateException(
                "Unmapped neoforge:conditions entry (extend the datagen-parity transform in " +
                        "buildSrc/src/main/kotlin/appengbuild/NeoForgeToFabricRecipeTransform.kt): $condition"
            )
        }

        private fun rewriteIngredients(node: JsonElement): JsonElement {
            if (node is JsonObject) {
                val out = JsonObject()
                for ((key, value) in node.entrySet()) {
                    if (key == "neoforge:ingredient_type") {
                        if (!isString(value, "neoforge:difference")) {
                            throw IllegalStateException(
                                "Unmapped custom ingredient type (extend the datagen-parity transform in " +
                                        "buildSrc/src/main/kotlin/appengbuild/NeoForgeToFabricRecipeTransform.kt): $value"
                            )
                        }
                        out.addProperty("fabric:type", "fabric:difference")
                    } else {
                        out.add(key, rewriteIngredients(value))
                    }
                }
                return out
            }
            if (node is JsonArray) {
                val out = JsonArray()
                for (element in node) {
                    out.add(rewriteIngredients(element))
                }
                return out
            }
            return node
        }

        private fun assertNoNeoForgeKeys(node: JsonElement) {
            if (node is JsonObject) {
                for ((key, value) in node.entrySet()) {
                    if (key.startsWith("neoforge:")) {
                        throw IllegalStateException(
                            "Unhandled neoforge-namespaced key '$key' in a generated recipe JSON " +
                                    "(extend the datagen-parity transform in " +
                                    "buildSrc/src/main/kotlin/appengbuild/NeoForgeToFabricRecipeTransform.kt)"
                        )
                    }
                    assertNoNeoForgeKeys(value)
                }
            } else if (node is JsonArray) {
                for (element in node) {
                    assertNoNeoForgeKeys(element)
                }
            }
        }

        private fun isString(element: JsonElement?, value: String): Boolean =
            element is JsonPrimitive && element.isString && element.asString == value
    }
}
