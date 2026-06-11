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

package appeng.neoforge.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.LoadingValidationCallback;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import appeng.blockentity.spatial.SpatialAnchorBlockEntity;
import appeng.core.AppEng;
import appeng.server.services.ChunkLoadingService;

/**
 * {@link ChunkLoadingService} backed by NeoForge's {@link TicketController}.
 */
public class NeoForgeChunkLoadingService implements ChunkLoadingService, LoadingValidationCallback {

    // Flag to ignore a server after it is stopping as grid nodes might reevaluate their grids during a shutdown.
    private boolean running = true;

    private final TicketController controller = new TicketController(AppEng.makeId("default"), this);

    public NeoForgeChunkLoadingService(IEventBus modEventBus) {
        modEventBus.addListener(this::register);
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void register(RegisterTicketControllersEvent event) {
        event.register(controller);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent evt) {
        this.running = true;
    }

    private void onServerStopping(ServerStoppingEvent event) {
        this.running = false;
    }

    @Override
    public void validateTickets(ServerLevel level, TicketHelper ticketHelper) {
        // Iterate over all blockpos registered as chunk loader to initialize them
        ticketHelper.getBlockTickets().forEach((blockPos, chunks) -> {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);

            // Add all persisted chunks to the list of handled ones by each anchor.
            // Or remove all in case the anchor no longer exists.
            if (blockEntity instanceof SpatialAnchorBlockEntity anchor) {
                for (Long chunk : chunks.normal()) {
                    anchor.registerChunk(ChunkPos.unpack(chunk));
                }
                for (Long chunk : chunks.naturalSpawning()) {
                    anchor.registerChunk(ChunkPos.unpack(chunk));
                }
            } else {
                ticketHelper.removeAllTickets(blockPos);
            }
        });
    }

    @Override
    public boolean forceChunk(ServerLevel level, BlockPos owner, ChunkPos position) {
        if (running) {
            return controller.forceChunk(level, owner, position.x(), position.z(), true, true);
        }

        return false;
    }

    @Override
    public boolean releaseChunk(ServerLevel level, BlockPos owner, ChunkPos position) {
        if (running) {
            return controller.forceChunk(level, owner, position.x(), position.z(), false, true);
        }

        return false;
    }
}
