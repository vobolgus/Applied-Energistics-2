package appeng.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class PartModelsTest {
    @Test
    void findsFullbrightTextureReferences() {
        var model = JsonParser.parseString("""
                {
                  "textures": {
                    "bright": "ae2:part/terminal_bright",
                    "normal": "ae2:part/terminal_dark",
                    "ambiguous": "ae2:part/shared"
                  },
                  "elements": [{
                    "faces": {
                      "north": {
                        "texture": "#bright",
                        "neoforge_data": { "block_light": 15, "sky_light": 15 }
                      },
                      "south": { "texture": "#normal" },
                      "east": {
                        "texture": "#ambiguous",
                        "neoforge_data": { "block_light": 15, "sky_light": 15 }
                      },
                      "west": { "texture": "#ambiguous" }
                    }
                  }]
                }
                """).getAsJsonObject();

        assertThat(ModelFaceMetadata.findEmissiveTextures(model))
                .containsExactly(Identifier.parse("ae2:part/terminal_bright"));
    }

    @Test
    void ignoresIncompleteAndCyclicTextureMetadata() {
        var model = JsonParser.parseString("""
                {
                  "textures": {
                    "cycle_a": "#cycle_b",
                    "cycle_b": "#cycle_a"
                  },
                  "elements": [{
                    "faces": {
                      "north": {
                        "texture": "#cycle_a",
                        "neoforge_data": { "block_light": 15, "sky_light": 15 }
                      },
                      "south": {
                        "texture": "ae2:part/not_fullbright",
                        "neoforge_data": { "block_light": 15, "sky_light": 14 }
                      }
                    }
                  }]
                }
                """).getAsJsonObject();

        assertThat(ModelFaceMetadata.findEmissiveTextures(model)).isEmpty();
    }

    @Test
    void resolvesInheritedGeometryWithChildTextures() {
        var parentId = Identifier.parse("ae2:block/controller_block_lights");
        var childId = Identifier.parse("ae2:block/controller_block_online");
        var parent = JsonParser.parseString("""
                {
                  "textures": { "particle": "ae2:block/controller" },
                  "elements": [{
                    "faces": {
                      "north": {
                        "texture": "#lights",
                        "neoforge_data": { "block_light": 15, "sky_light": 15 }
                      },
                      "south": { "texture": "#block" }
                    }
                  }]
                }
                """).getAsJsonObject();
        var child = JsonParser.parseString("""
                {
                  "parent": "ae2:block/controller_block_lights",
                  "textures": {
                    "lights": "ae2:block/controller_lights",
                    "block": "ae2:block/controller_powered"
                  }
                }
                """).getAsJsonObject();

        assertThat(ModelFaceMetadata.findEmissiveTextures(childId, Map.of(parentId, parent, childId, child)))
                .containsExactly(Identifier.parse("ae2:block/controller_lights"));
    }

    @Test
    void keepsNormallyLitTexturesOutOfMixedModels() {
        var model = JsonParser.parseString("""
                {
                  "textures": {
                    "core": "ae2:block/mysterious_cube_core",
                    "shell": "ae2:block/mysterious_cube_side"
                  },
                  "elements": [
                    { "faces": { "north": { "texture": "#core" } } },
                    { "faces": { "north": {
                      "texture": "#shell",
                      "neoforge_data": { "block_light": 15, "sky_light": 15 }
                    } } }
                  ]
                }
                """).getAsJsonObject();

        assertThat(ModelFaceMetadata.findEmissiveTextures(model))
                .containsExactly(Identifier.parse("ae2:block/mysterious_cube_side"));
    }
}
