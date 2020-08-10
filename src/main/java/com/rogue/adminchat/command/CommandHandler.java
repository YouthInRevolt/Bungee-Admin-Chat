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
package com.rogue.adminchat.command;

import static com.rogue.adminchat.command.CommandType.*;

import com.rogue.adminchat.AdminChat;
import com.rogue.adminchat.AdminListener;
import com.rogue.adminchat.channel.Channel;
import com.rogue.adminchat.channel.ChannelManager;
import com.rogue.adminchat.channel.ChannelNotFoundException;
import com.rogue.adminchat.runnable.UnmuteRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

/**
 * @author 1Rogue
 * @version 1.3.1
 * @since 1.3.0
 */
public class CommandHandler {

    private final AdminChat plugin;
    private final Map<String, String> toggled = new ConcurrentHashMap();

    public CommandHandler(AdminChat plugin) {
        this.plugin = plugin;
    }


    /**
     * Returns a list of players that are toggled for admin chat
     *
     * @return List of toggled players
     * @version 1.3.0
     * @since 1.2.0
     */
    public Map<String, String> getToggled() {
        return this.toggled;
    }

    /**
     * Returns AdminChat's custom command class for a provided command name
     *
     * @param cmd The command to try against
     * @return The {@link ACCommand} version of the command
     */
    private ACCommand getCommand(String cmd) {
        if (cmd.endsWith("toggle")) {
            String command = cmd.substring(0, cmd.length() - 6);
            if (this.plugin.getChannelManager().isChannel(command)) {
                return new ACCommand(command, CommandType.TOGGLE);
            }
        } else if (cmd.endsWith("unmute")) {
            String command = cmd.substring(0, cmd.length() - 6);
            if (this.plugin.getChannelManager().isChannel(command)) {
                return new ACCommand(command, CommandType.UNMUTE);
            }
        }
        if (cmd.endsWith("mute")) {
            String command = cmd.substring(0, cmd.length() - 4);
            if (this.plugin.getChannelManager().isChannel(command)) {
                return new ACCommand(command, CommandType.MUTE);
            }
        }
        if (this.plugin.getChannelManager().isChannel(cmd)) {
            return new ACCommand(cmd, CommandType.NORMAL);
        }
        return null;
    }
}