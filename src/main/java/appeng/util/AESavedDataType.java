/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2025, TeamAppliedEnergistics, All rights reserved.
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

package appeng.util;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Loader-neutral description of a {@link SavedData} type whose constructor and codec need access to the owning
 * {@link ServerLevel}.
 * <p>
 * NeoForge patches the vanilla {@code SavedDataType} record to make level-sensitive factories/codecs possible (and its
 * {@code dataFixType} nullable); vanilla (Fabric) has neither. This record captures AE2's requirements; the
 * loader-specific translation to an actual {@code SavedDataType} happens in
 * {@link LoaderPlatform#computeSavedDataIfAbsent}.
 *
 * @param id           id of the saved data (determines the file name in the level's {@code data/} folder)
 * @param factory      creates a fresh instance for the given level
 * @param codecFactory creates the codec used to load/save instances for the given level
 */
public record AESavedDataType<T extends SavedData>(
        Identifier id,
        Function<ServerLevel, T> factory,
        Function<ServerLevel, Codec<T>> codecFactory) {
}
