/*
 * Copyright (C) 2013 Spencer Alderman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.rogue.adminchat;

import java.util.Map;

import com.rogue.adminchat.command.Command;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * @author 1Rogue
 * @version 1.3.0
 * @since 1.2.0
 */
public class AdminListener implements Listener {

    private final AdminChat plugin;

    public AdminListener(AdminChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Makes players who have toggled adminchat send chat to the appropriate
     * channels
     *
     * @param event AsyncPlayerChatEvent instance
     * @version 1.3.0
     * @since 1.2.0
     */
    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.getMessage().startsWith("/")) {
            return;
        }

        final String name = ((ProxiedPlayer) event.getSender()).getName();
        String chan = Command.toggled.get(name);
        if (chan != null) {
            event.setCancelled(true);
            plugin.getChannelManager().sendMessage(chan, name, event.getMessage());
        }
    }
}