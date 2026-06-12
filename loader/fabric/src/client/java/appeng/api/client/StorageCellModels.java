/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 TeamAppliedEnergistics
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.client;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import appeng.core.AppEng;

/**
 * A registry for 3D models used to render storage cells in the world, when they are inserted into a drive or similar
 * machines.
 * <p>
 * This is the <strong>Fabric twin</strong> of the NeoForge class with the same FQN. The only difference is the
 * standalone-model key type: NeoForge's {@code StandaloneModelKey<BlockStateModel>} is replaced by Fabric's
 * {@link ExtraModelKey}&lt;BlockStateModel&gt; (registered by the AE2 Fabric client entrypoint through a
 * {@code ModelLoadingPlugin} and resolved via {@code FabricModelManager#getModel}).
 */
public final class StorageCellModels {

    private static final Identifier MODEL_CELL_DEFAULT = AppEng.makeId("block/drive_cell");
    private static final ExtraModelKey<BlockStateModel> MODEL_CELL_DEFAULT_STANDALONE = ExtraModelKey
            .create(MODEL_CELL_DEFAULT::toString);

    private static final Map<Item, Identifier> registry = new IdentityHashMap<>();
    private static final Map<Item, ExtraModelKey<BlockStateModel>> standaloneRegistry = new IdentityHashMap<>();

    private StorageCellModels() {
    }

    /**
     * Register a new model for a storage cell item.
     * <p>
     * This method only maps an {@link Item} to a {@link Identifier} which can be looked up from the
     * {@link ModelBakery}. No validation about missing models will be done.
     * <p>
     * Will throw an exception in case a model is already registered for an item.
     * <p>
     * For examples look at our cell part models within the drive model directory.
     *
     * @param itemLike The cell item
     * @param model    The {@link Identifier} representing the model.
     */
    public synchronized static void registerModel(ItemLike itemLike, Identifier model) {
        Objects.requireNonNull(itemLike, "itemLike");
        var item = Objects.requireNonNull(itemLike.asItem(), "item.asItem()");
        Objects.requireNonNull(model, "model");
        Preconditions.checkArgument(!registry.containsKey(item), "Cannot register an item twice.");

        registry.put(item, model);
        standaloneRegistry.put(item, ExtraModelKey.create(model::toString));
    }

    /**
     * The {@link Identifier} of the model used to render the given storage cell {@link Item} when inserted into a drive
     * or similar.
     *
     * @return null, if no model is registered.
     */
    @Nullable
    public synchronized static Identifier model(ItemLike itemLike) {
        Objects.requireNonNull(itemLike, "itemLike");
        var item = Objects.requireNonNull(itemLike.asItem(), "itemLike.asItem()");

        return registry.get(item);
    }

    /**
     * The extra-model key for a given cell item, for use with Fabric's {@code FabricModelManager#getModel}. Since the
     * keys are never allowed to change, the registry pre-creates them for your registered models.
     *
     * @return null, if no model is registered.
     */
    @Nullable
    public synchronized static ExtraModelKey<BlockStateModel> standaloneModel(ItemLike itemLike) {
        Objects.requireNonNull(itemLike, "itemLike");
        var item = Objects.requireNonNull(itemLike.asItem(), "itemLike.asItem()");

        return standaloneRegistry.get(item);
    }

    /**
     * A copy of all registered mappings.
     */
    public synchronized static Map<Item, Identifier> models() {
        return new IdentityHashMap<>(registry);
    }

    /**
     * A copy of all registered standalone model keys, keyed by the model id they resolve.
     */
    public synchronized static Map<Item, ExtraModelKey<BlockStateModel>> standaloneModels() {
        return new IdentityHashMap<>(standaloneRegistry);
    }

    /**
     * Returns the default model, which can be used when no explicit model is registered.
     */
    public static Identifier getDefaultModel() {
        return MODEL_CELL_DEFAULT;
    }

    /**
     * Returns the default model, which can be used when no explicit model is registered.
     */
    public static ExtraModelKey<BlockStateModel> getDefaultStandaloneModel() {
        return MODEL_CELL_DEFAULT_STANDALONE;
    }

}
