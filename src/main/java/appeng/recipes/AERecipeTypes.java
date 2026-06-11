package appeng.recipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import appeng.core.AppEng;
import appeng.core.registration.AERegistries;
import appeng.recipes.entropy.EntropyRecipe;
import appeng.recipes.game.CraftingUnitTransformRecipe;
import appeng.recipes.game.StorageCellDisassemblyRecipe;
import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.recipes.mattercannon.MatterCannonAmmo;
import appeng.recipes.quartzcutting.QuartzCuttingRecipe;
import appeng.recipes.transform.TransformRecipe;

public final class AERecipeTypes {
    private AERecipeTypes() {
    }

    public static final RecipeType<TransformRecipe> TRANSFORM = register("transform");
    public static final RecipeType<EntropyRecipe> ENTROPY = register("entropy");
    public static final RecipeType<InscriberRecipe> INSCRIBER = register("inscriber");
    public static final RecipeType<ChargerRecipe> CHARGER = register("charger");
    public static final RecipeType<MatterCannonAmmo> MATTER_CANNON_AMMO = register("matter_cannon");
    public static final RecipeType<QuartzCuttingRecipe> QUARTZ_CUTTING = register("quartz_cutting");
    public static final RecipeType<CraftingUnitTransformRecipe> CRAFTING_UNIT_TRANSFORM = register(
            "crafting_unit_transform");
    public static final RecipeType<StorageCellDisassemblyRecipe> CELL_DISASSEMBLY = register(
            "storage_cell_disassembly");

    /**
     * Forces the class to be loaded, ensuring all registration entries above were collected into {@link AERegistries}.
     */
    public static void init() {
    }

    private static <T extends Recipe<?>> RecipeType<T> register(String id) {
        // was Neo's RecipeType.simple(...) (a Neo patch); this is its exact body. The vanilla alternative,
        // RecipeType.register(String), eagerly registers into BuiltInRegistries and would bypass AERegistries.
        var name = AppEng.makeId(id).toString();
        RecipeType<T> type = new RecipeType<>() {
            @Override
            public String toString() {
                return name;
            }
        };
        // was DeferredRegister: DR.register(id, () -> type)
        AERegistries.register(Registries.RECIPE_TYPE, AppEng.makeId(id), () -> type);
        return type;
    }
}
