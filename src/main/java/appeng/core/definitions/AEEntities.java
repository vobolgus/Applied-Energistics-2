/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.core.definitions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;

import appeng.core.AppEng;
import appeng.core.registration.AERegistries;
import appeng.core.registration.AERegistryEntry;
import appeng.entity.TinyTNTPrimedEntity;

public final class AEEntities {

    public static final Map<String, String> ENTITY_ENGLISH_NAMES = new HashMap<>();

    public static final AERegistryEntry<EntityType<?>, EntityType<TinyTNTPrimedEntity>> TINY_TNT_PRIMED = create(
            "tiny_tnt_primed",
            "Tiny TNT Primed",
            TinyTNTPrimedEntity::new,
            MobCategory.MISC,
            // was Neo's setTrackingRange(16).setUpdateInterval(4).setShouldReceiveVelocityUpdates(true);
            // clientTrackingRange/updateInterval are the identical vanilla knobs, and vanilla sends velocity
            // updates for all entity types except a fixed exclusion list (trackDeltas), so the third call is moot.
            builder -> builder.clientTrackingRange(16).updateInterval(4));

    /**
     * Forces the class to be loaded, ensuring all registration entries above were collected into {@link AERegistries}.
     */
    public static void init() {
    }

    private static <T extends Entity> AERegistryEntry<EntityType<?>, EntityType<T>> create(String id,
            String englishName,
            EntityFactory<T> entityFactory,
            MobCategory classification,
            Consumer<Builder<T>> customizer) {
        ENTITY_ENGLISH_NAMES.put(id, englishName);
        // was DeferredRegister: DR.register(id, () -> {...})
        return AERegistries.register(Registries.ENTITY_TYPE, AppEng.makeId(id), () -> {
            Builder<T> builder = Builder.of(entityFactory, classification);
            customizer.accept(builder);
            // Temporarily disable the data fixer check to avoid the annoying "no data fixer registered for ae2:xxx".
            boolean prev = SharedConstants.CHECK_DATA_FIXER_SCHEMA;
            SharedConstants.CHECK_DATA_FIXER_SCHEMA = false;
            EntityType<T> result = builder.build(ResourceKey.create(Registries.ENTITY_TYPE, AppEng.makeId(id)));
            SharedConstants.CHECK_DATA_FIXER_SCHEMA = prev;
            return result;
        });
    }

}
