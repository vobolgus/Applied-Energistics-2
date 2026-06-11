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

package appeng.parts.automation;

import appeng.api.stacks.AEKeyType;

/**
 * Registers the default item/fluid import/export/external-storage strategies, which adapt the loader's transfer API.
 * This is a <strong>loader-duplicated</strong> class: it is invoked from the static initializer of the shared
 * {@link StackWorldBehaviors}, and each loader registers its own transfer-API adapters (NeoForge:
 * {@code ResourceHandler}-based, Fabric: {@code Storage}-based).
 */
final class DefaultStorageStrategies {
    private DefaultStorageStrategies() {
    }

    static void register() {
        StackWorldBehaviors.registerImportStrategy(AEKeyType.items(), StorageImportStrategy::createItem);
        StackWorldBehaviors.registerImportStrategy(AEKeyType.fluids(), StorageImportStrategy::createFluid);
        StackWorldBehaviors.registerExportStrategy(AEKeyType.items(), StorageExportStrategy::createItem);
        StackWorldBehaviors.registerExportStrategy(AEKeyType.fluids(), StorageExportStrategy::createFluid);
        StackWorldBehaviors.registerExternalStorageStrategy(AEKeyType.items(),
                ForgeExternalStorageStrategy::createItem);
        StackWorldBehaviors.registerExternalStorageStrategy(AEKeyType.fluids(),
                ForgeExternalStorageStrategy::createFluid);
    }
}
