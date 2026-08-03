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

package appeng.fabric.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

import appeng.blockentity.storage.DriveBlockEntity;
import appeng.fabric.network.CellItemIdSync;

/**
 * Replaces the cell-item raw ids in the drive's update packet with registry identifiers - the Fabric fix for the
 * static-registry raw-id desync (see {@link CellItemIdSync} for the full why and the wire form).
 * <p>
 * Both call sites are unique within their method: {@code writeToStream} writes the packed cell states with
 * {@code writeInt} and only the cell loop uses {@code writeVarInt}; {@code readFromStream} mirrors that with
 * {@code readInt}. {@code super.writeToStream}/{@code super.readFromStream} are separate invocations and are not
 * touched. With {@code defaultRequire: 1} an upstream change to either loop fails the build instead of silently
 * reverting the fix.
 * <p>
 * Redirecting rather than overwriting keeps the shared {@code DriveBlockEntity} the only place that knows what the
 * fields mean - this mixin only changes how one {@code int} travels.
 */
@Mixin(DriveBlockEntity.class)
public abstract class DriveCellItemIdMixin {

    @Redirect(method = "writeToStream", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/RegistryFriendlyByteBuf;writeVarInt(I)Lnet/minecraft/network/FriendlyByteBuf;"))
    private FriendlyByteBuf ae2$writeCellItemId(RegistryFriendlyByteBuf data, int rawItemId) {
        return CellItemIdSync.writeItemByRawId(data, rawItemId);
    }

    @Redirect(method = "readFromStream", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/RegistryFriendlyByteBuf;readVarInt()I"))
    private int ae2$readCellItemId(RegistryFriendlyByteBuf data) {
        return CellItemIdSync.readItemAsRawId(data);
    }
}
