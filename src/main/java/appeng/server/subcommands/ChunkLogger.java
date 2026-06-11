/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.server.subcommands;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.server.ISubCommand;

/**
 * The loader entrypoint must wire {@link #chunkLoaded} and {@link #chunkUnloaded} to its chunk load/unload events; they
 * are no-ops unless logging has been enabled via the command.
 */
public class ChunkLogger implements ISubCommand {

    /**
     * The loggers that are currently enabled (previously: loggers registered to the NeoForge game bus).
     */
    private static final List<ChunkLogger> ACTIVE_LOGGERS = new CopyOnWriteArrayList<>();

    private boolean enabled = false;

    public static void chunkLoaded(LevelAccessor level, ChunkAccess chunk) {
        for (var logger : ACTIVE_LOGGERS) {
            logger.onChunkLoadEvent(level, chunk);
        }
    }

    public static void chunkUnloaded(LevelAccessor level, ChunkAccess chunk) {
        for (var logger : ACTIVE_LOGGERS) {
            logger.onChunkUnloadEvent(level, chunk);
        }
    }

    private void displayStack() {
        if (AEConfig.instance().isChunkLoggerTraceEnabled()) {
            boolean output = false;
            for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
                if (output) {
                    AELog.info(
                            "		" + e.getClassName() + '.' + e.getMethodName() + " (" + e.getLineNumber() + ')');
                } else {
                    output = e.getClassName().contains("EventBus") && e.getMethodName().contains("post");
                }
            }
        }
    }

    private void onChunkLoadEvent(LevelAccessor eventLevel, ChunkAccess chunk) {
        if (eventLevel instanceof ServerLevel level) {
            var chunkPos = chunk.getPos();
            var center = getCenter(chunk);
            AELog.info("Loaded chunk " + chunkPos.x() + "," + chunkPos.z() + " [center: " + center + "] in "
                    + level.dimension().identifier());
            this.displayStack();
        }
    }

    private void onChunkUnloadEvent(LevelAccessor eventLevel, ChunkAccess chunk) {
        if (eventLevel instanceof ServerLevel level) {
            var chunkPos = chunk.getPos();
            var center = getCenter(chunk);
            AELog.info("Unloaded chunk " + chunkPos.x() + "," + chunkPos.z() + " [center: " + center + "] in "
                    + level.dimension().identifier());
            this.displayStack();
        }
    }

    private static String getCenter(ChunkAccess chunk) {
        var chunkPos = chunk.getPos();
        var x = chunkPos.getMiddleBlockX();
        var z = chunkPos.getMiddleBlockZ();
        var y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
        return x + " " + y + " " + z;
    }

    @Override
    public void call(MinecraftServer srv, CommandContext<CommandSourceStack> data,
            CommandSourceStack sender) {
        this.enabled = !this.enabled;

        if (this.enabled) {
            ACTIVE_LOGGERS.add(this);
            sender.sendSuccess(() -> Component.translatable("commands.ae2.ChunkLoggerOn"), true);
        } else {
            ACTIVE_LOGGERS.remove(this);
            sender.sendSuccess(() -> Component.translatable("commands.ae2.ChunkLoggerOff"), true);
        }
    }
}
