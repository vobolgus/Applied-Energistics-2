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

import net.minecraft.server.MinecraftServer;

import appeng.fabric.FabricLoaderPlatform;

/**
 * Starts the server main thread inside AE2's server thread group, mirroring NeoForge's patch of
 * {@code MinecraftServer#spin} (which passes {@code SidedThreadGroups.SERVER} to the {@code Thread} constructor).
 * {@code Platform#assertServerThread}/{@code isServer}/{@code isClient} are thread-group based; without this, the
 * integrated server thread on the Fabric client is indistinguishable from a client thread and the first
 * {@code assertServerThread} (e.g. {@code TickHandler#shutdown} on world unload) throws (Phase 3b boot incident #5).
 * Applies to all server flavors spun through this method: integrated, dedicated and the gametest server.
 */
@Mixin(MinecraftServer.class)
public abstract class ServerThreadGroupMixin {
    @Redirect(method = "spin", at = @At(value = "NEW", target = "(Ljava/lang/Runnable;Ljava/lang/String;)Ljava/lang/Thread;"))
    private static Thread ae2$spinInServerThreadGroup(Runnable runnable, String name) {
        return new Thread(FabricLoaderPlatform.SERVER_THREAD_GROUP, runnable, name);
    }
}
