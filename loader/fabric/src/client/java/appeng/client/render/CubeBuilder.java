/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.client.render;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;

import appeng.fabric.client.render.QuadColors;

/**
 * Builds the quads for a cube.
 * <p>
 * Fabric twin of the NeoForge class with the same FQN. NeoForge's version builds the quads through the NeoForge
 * {@code MutableQuad}; this version constructs the vanilla {@link BakedQuad}s directly, replicating the same vertex
 * positions ({@code MutableQuad#setCubeFaceFromSpriteCoords}), UV assignment and material resolution
 * ({@code MutableQuad#setSprite}). FRAPI impedance: the vanilla quad format has no per-vertex colors and no baked
 * normals (both are NeoForge patches), so the color is recorded in the {@link QuadColors} side channel (re-applied by
 * the fabric model twins when re-emitting through FRAPI meshes) and the normals — which NeoForge bakes to the face
 * normal anyway — are left to the render pipeline's face-normal default.
 */
public class CubeBuilder {

    private final Consumer<BakedQuad> output;

    private final EnumMap<Direction, TextureAtlasSprite> textures = new EnumMap<>(Direction.class);

    private EnumSet<Direction> drawFaces = EnumSet.allOf(Direction.class);

    private final EnumMap<Direction, Vector4f> customUv = new EnumMap<>(Direction.class);

    private final byte[] uvRotations = new byte[Direction.values().length];

    private final boolean[] flipU = new boolean[Direction.values().length];

    private final boolean[] flipV = new boolean[Direction.values().length];

    private int color = 0xFFFFFFFF;

    private boolean emissiveMaterial;

    public CubeBuilder(Consumer<BakedQuad> output) {
        this.output = output;
    }

    public void addCube(float x1, float y1, float z1, float x2, float y2, float z2) {
        x1 /= 16.0f;
        y1 /= 16.0f;
        z1 /= 16.0f;
        x2 /= 16.0f;
        y2 /= 16.0f;
        z2 /= 16.0f;

        for (var face : this.drawFaces) {
            this.putFace(face, x1, y1, z1, x2, y2, z2);
        }
    }

    public void addQuad(Direction face, float x1, float y1, float z1, float x2, float y2, float z2) {
        this.putFace(face, x1, y1, z1, x2, y2, z2);
    }

    public void setFlipU(Direction side, boolean enable) {
        flipU[side.ordinal()] = enable;
    }

    public void setFlipV(Direction side, boolean enable) {
        flipV[side.ordinal()] = enable;
    }

    private static final class UvVector {
        float u1;
        float u2;
        float v1;
        float v2;
    }

    private void putFace(Direction face, float x1, float y1, float z1, float x2, float y2, float z2) {

        var texture = this.textures.get(face);

        var uv = new UvVector();

        // The user might have set specific UV coordinates for this face
        var customUv = this.customUv.get(face);
        if (customUv != null) {
            uv.u1 = texture.getU(customUv.x());
            uv.v1 = texture.getV(customUv.y());
            uv.u2 = texture.getU(customUv.z());
            uv.v2 = texture.getV(customUv.w());
        } else {
            uv = this.getStandardUv(face, texture, x1, y1, z1, x2, y2, z2);
        }

        // Mirrors MutableQuad#setCubeFaceFromSpriteCoords for the same
        // (left, bottom, right, top, depth) arguments the NeoForge twin passes per face.
        var positions = new Vector3f[] { new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f() };
        switch (face) {
            case DOWN -> setCubeFacePositions(positions, face, x1, z1, x2, z2, y1);
            case UP -> setCubeFacePositions(positions, face, x1, 1 - z2, x2, 1 - z1, 1 - y2);
            case NORTH -> setCubeFacePositions(positions, face, 1 - x2, y1, 1 - x1, y2, z1);
            case SOUTH -> setCubeFacePositions(positions, face, x1, y1, x2, y2, 1 - z2);
            case WEST -> setCubeFacePositions(positions, face, z1, y1, z2, y2, x1);
            case EAST -> setCubeFacePositions(positions, face, 1 - z2, y1, 1 - z1, y2, 1 - x2);
        }

        // Mirrors MutableQuad#setUv assignment in the NeoForge twin's setFaceUV
        var packedUv = new long[4];
        setFaceUV(face, packedUv, uv);

        // Mirrors MutableQuad#setSprite(Material.Baked) with forceTranslucent = false
        var transparency = texture.contents().transparency();
        var itemRenderType = texture.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)
                ? (transparency.hasTranslucent() ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet())
                : (transparency.hasTranslucent() ? Sheets.translucentItemSheet() : Sheets.cutoutItemSheet());
        var materialInfo = new BakedQuad.MaterialInfo(
                texture,
                ChunkSectionLayer.byTransparency(transparency),
                itemRenderType,
                -1,
                true,
                emissiveMaterial ? 15 : 0);

        var quad = new BakedQuad(
                positions[0], positions[1], positions[2], positions[3],
                packedUv[0], packedUv[1], packedUv[2], packedUv[3],
                face,
                materialInfo);

        // Vanilla BakedQuads cannot carry the per-vertex color; record it in the side channel
        if (color != 0xFFFFFFFF) {
            QuadColors.put(quad, color);
        }

        output.accept(quad);
    }

    private static void setCubeFacePositions(Vector3f[] positions, Direction side,
            float left, float bottom, float right, float top, float depth) {
        switch (side) {
            case NORTH -> {
                positions[0].set(1 - left, top, depth);
                positions[1].set(1 - left, bottom, depth);
                positions[2].set(1 - right, bottom, depth);
                positions[3].set(1 - right, top, depth);
            }
            case SOUTH -> {
                positions[0].set(left, top, 1 - depth);
                positions[1].set(left, bottom, 1 - depth);
                positions[2].set(right, bottom, 1 - depth);
                positions[3].set(right, top, 1 - depth);
            }
            case EAST -> {
                positions[0].set(1 - depth, top, 1 - left);
                positions[1].set(1 - depth, bottom, 1 - left);
                positions[2].set(1 - depth, bottom, 1 - right);
                positions[3].set(1 - depth, top, 1 - right);
            }
            case WEST -> {
                positions[0].set(depth, top, left);
                positions[1].set(depth, bottom, left);
                positions[2].set(depth, bottom, right);
                positions[3].set(depth, top, right);
            }
            case UP -> {
                positions[0].set(left, 1 - depth, 1 - top);
                positions[1].set(left, 1 - depth, 1 - bottom);
                positions[2].set(right, 1 - depth, 1 - bottom);
                positions[3].set(right, 1 - depth, 1 - top);
            }
            case DOWN -> {
                positions[0].set(left, depth, top);
                positions[1].set(left, depth, bottom);
                positions[2].set(right, depth, bottom);
                positions[3].set(right, depth, top);
            }
        }
    }

    private void setFaceUV(Direction face, long[] packedUv, UvVector uv) {
        var rotation = uvRotations[face.ordinal()];

        if (flipU[face.ordinal()]) {
            var tmp = uv.u1;
            uv.u1 = uv.u2;
            uv.u2 = tmp;
        }
        if (flipV[face.ordinal()]) {
            var tmp = uv.v1;
            uv.v1 = uv.v2;
            uv.v2 = tmp;
        }

        switch (face) {
            case DOWN, UP -> {
                packedUv[(0 + 4 - rotation) % 4] = UVPair.pack(uv.u1, uv.v1);
                packedUv[(1 + 4 - rotation) % 4] = UVPair.pack(uv.u1, uv.v2);
                packedUv[(2 + 4 - rotation) % 4] = UVPair.pack(uv.u2, uv.v2);
                packedUv[(3 + 4 - rotation) % 4] = UVPair.pack(uv.u2, uv.v1);
            }
            case NORTH, SOUTH, WEST, EAST -> {
                packedUv[(0 + 4 - rotation) % 4] = UVPair.pack(uv.u1, uv.v2);
                packedUv[(1 + 4 - rotation) % 4] = UVPair.pack(uv.u1, uv.v1);
                packedUv[(2 + 4 - rotation) % 4] = UVPair.pack(uv.u2, uv.v1);
                packedUv[(3 + 4 - rotation) % 4] = UVPair.pack(uv.u2, uv.v2);
            }
        }
    }

    private UvVector getStandardUv(Direction face, TextureAtlasSprite texture, float x1, float y1, float z1, float x2,
            float y2, float z2) {
        UvVector uv = new UvVector();

        if (face.getAxis() != Direction.Axis.Y) {
            uv.v1 = texture.getV(1 - y1);
            uv.v2 = texture.getV(1 - y2);
        } else {
            uv.v1 = texture.getV(z1);
            uv.v2 = texture.getV(z2);
        }

        switch (face) {
            case DOWN, UP, SOUTH -> {
                uv.u1 = texture.getU(x1);
                uv.u2 = texture.getU(x2);
            }
            case NORTH -> {
                uv.u1 = texture.getU(1 - x2);
                uv.u2 = texture.getU(1 - x1);
            }
            case WEST -> {
                uv.u1 = texture.getU(z1);
                uv.u2 = texture.getU(z2);
            }
            case EAST -> {
                uv.u1 = texture.getU(1 - z2);
                uv.u2 = texture.getU(1 - z1);
            }
        }

        return uv;
    }

    public void setTexture(TextureAtlasSprite texture) {
        for (Direction face : Direction.values()) {
            this.textures.put(face, texture);
        }
    }

    public void setTexture(Material.Baked texture) {
        for (Direction face : Direction.values()) {
            this.textures.put(face, texture.sprite());
        }
    }

    public void setTextures(Material.Baked up, Material.Baked down, Material.Baked north,
            Material.Baked south, Material.Baked east, Material.Baked west) {
        this.textures.put(Direction.UP, up.sprite());
        this.textures.put(Direction.DOWN, down.sprite());
        this.textures.put(Direction.NORTH, north.sprite());
        this.textures.put(Direction.SOUTH, south.sprite());
        this.textures.put(Direction.EAST, east.sprite());
        this.textures.put(Direction.WEST, west.sprite());
    }

    public void setTextures(TextureAtlasSprite up, TextureAtlasSprite down, TextureAtlasSprite north,
            TextureAtlasSprite south, TextureAtlasSprite east, TextureAtlasSprite west) {
        this.textures.put(Direction.UP, up);
        this.textures.put(Direction.DOWN, down);
        this.textures.put(Direction.NORTH, north);
        this.textures.put(Direction.SOUTH, south);
        this.textures.put(Direction.EAST, east);
        this.textures.put(Direction.WEST, west);
    }

    public void setTexture(Direction facing, TextureAtlasSprite sprite) {
        this.textures.put(facing, sprite);
    }

    public void setDrawFaces(EnumSet<Direction> drawFaces) {
        this.drawFaces = drawFaces;
    }

    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Sets the vertex color for future vertices to the given RGB value, and forces the alpha component to 255.
     */
    public void setColorRGB(int color) {
        this.setColor(color | 0xFF000000);
    }

    public void setColorRGB(float r, float g, float b) {
        this.setColorRGB((int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255));
    }

    public void setEmissiveMaterial(boolean renderFullBright) {
        this.emissiveMaterial = renderFullBright;
    }

    public void setCustomUv(Direction facing, float u1, float v1, float u2, float v2) {
        this.customUv.put(facing, new Vector4f(u1, v1, u2, v2));
    }

    public void setUvRotation(Direction facing, int rotation) {
        Preconditions.checkArgument(rotation >= 0 && rotation <= 3, "rotation");
        this.uvRotations[facing.ordinal()] = (byte) rotation;
    }
}
