package appeng.client.model;

import java.util.List;
import java.util.Objects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

import appeng.client.api.model.parts.PartModel;
import appeng.core.AppEng;
import appeng.parts.automation.PartModelData;
import appeng.util.render.AERenderData;

public class CableAnchorPartModel implements PartModel {
    private final BlockStateModelPart model;
    private final BlockStateModelPart shortModel;

    public CableAnchorPartModel(BlockStateModelPart model, BlockStateModelPart shortModel) {
        this.model = model;
        this.shortModel = shortModel;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, AERenderData partModelData, RandomSource random,
            List<BlockStateModelPart> parts) {
        var shortVersion = Objects.requireNonNullElse(partModelData.get(PartModelData.CABLE_ANCHOR_SHORT), false);

        if (shortVersion) {
            parts.add(shortModel);
        } else {
            parts.add(model);
        }
    }

    @Override
    public Material.Baked particleMaterial() {
        return model.particleMaterial();
    }

    @Override
    public boolean canAttachToStraightCable() {
        return true;
    }

    public record Unbaked(Identifier model, Identifier shortModel) implements PartModel.Unbaked {
        public static final Identifier ID = AppEng.makeId("cable_anchor");
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model),
                Identifier.CODEC.fieldOf("short_model").forGetter(Unbaked::shortModel))
                .apply(builder, Unbaked::new));

        @Override
        public MapCodec<? extends PartModel.Unbaked> codec() {
            return MAP_CODEC;
        }

        @Override
        public PartModel bake(ModelBaker baker, ModelState modelState) {
            var bakedModel = PartModels.bake(baker, model, modelState);
            var shortBakedModel = PartModels.bake(baker, shortModel, modelState);

            return new CableAnchorPartModel(bakedModel, shortBakedModel);
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(model);
            resolver.markDependency(shortModel);
        }
    }
}
