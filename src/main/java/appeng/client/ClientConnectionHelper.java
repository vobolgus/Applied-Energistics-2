/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2026, TeamAppliedEnergistics, All rights reserved.
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

package appeng.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

/**
 * Client-only access to the current connection for code paths that live in common classes.
 * <p>
 * The callers (menus, {@code MenuTypeBuilder}) are classloaded on the dedicated server too, so they must not reference
 * {@code net.minecraft.client} types themselves. Routing the calls through this class keeps the client types out of
 * their constant pools; this class is only ever classloaded from client-side code paths.
 */
public final class ClientConnectionHelper {
    private ClientConnectionHelper() {
    }

    /**
     * Notifies the server that the given container was closed on the client.
     */
    public static void sendContainerClose(int containerId) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundContainerClosePacket(containerId));
        }
    }

    /**
     * Checks whether the given slash-command fully parses against the command tree the server sent to this client, i.e.
     * whether the player would be allowed to run it (permission mods strip commands the player may not use from the
     * synced tree).
     */
    public static boolean canRunCommand(String command) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        var commands = connection.getCommands();
        var parseResult = commands.parse(command.substring(1), connection.getSuggestionsProvider());
        // A remaining-input parse result means the command did not fully parse against the synced tree
        return !parseResult.getReader().canRead();
    }
}
