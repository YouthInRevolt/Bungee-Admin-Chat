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

import com.rogue.adminchat.AdminChat;
import com.rogue.adminchat.channel.Channel;
import com.rogue.adminchat.channel.ChannelManager;
import com.rogue.adminchat.channel.ChannelNotFoundException;
import com.rogue.adminchat.runnable.UnmuteRunnable;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author 1Rogue
 * @version 1.3.1
 * @since 1.3.0
 */
public class Command extends net.md_5.bungee.api.plugin.Command {

    private final AdminChat plugin;
    public static Map<String, String> toggled = new ConcurrentHashMap();
    private String commandLabel;

    public Command(String label, AdminChat plugin) {
        super(label);
        this.commandLabel = label;
        this.plugin = plugin;
    }

    public void execute(final CommandSender sender, String[] args) {
        if (commandLabel.equalsIgnoreCase("adminchat")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("adminchat.*")) {
                if (sender instanceof ProxiedPlayer) {
                    this.plugin.reload(sender.getName());
                } else {
                    this.plugin.reload();
                }
                return;
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("muteall") && sender.hasPermission("adminchat.*")) {
                long time = Long.parseLong(args[1]);
                if (args.length < 3) {
                    this.plugin.setGlobalMute(true);
                    this.plugin.getExecutiveManager().runAsyncTask(new Runnable() {
                        public void run() {
                            plugin.setGlobalMute(false);
                        }
                    }, time);
                } else {
                    StringBuilder badtargets = new StringBuilder();
                    int badtar = 0;
                    List<String> targets = new ArrayList();
                    for (int i = 2; i < args.length; i++) {
                        ProxiedPlayer target = this.plugin.getProxy().getPlayer(args[i]);
                        if (target == null) {
                            badtargets.append("&c").append(args[2]).append("&a, ");
                            badtar++;
                        } else {
                            targets.add(target.getName());
                        }
                        try {
                            this.plugin.getChannelManager().mute(null, targets.toArray(new String[targets.size()]));
                            this.plugin.getExecutiveManager().runAsyncTask(new UnmuteRunnable(
                                    this.plugin,
                                    null,
                                    targets.toArray(new String[targets.size()])), time);
                        } catch (ChannelNotFoundException ex) {
                            this.plugin.communicate(sender.getName(), ex.getMessage());
                        }
                    }
                    if (badtargets.length() != 0) {
                        this.plugin.communicate(sender.getName(), "Player" + ((badtar == 1) ? "" : "s") + " " + badtargets.substring(0, badtargets.length() - 2) + " not found!");
                        return;
                    }
                }
            }
        }
        final ChannelManager manager = this.plugin.getChannelManager();
        final Map<String, Channel> channels;
        synchronized (channels = manager.getChannels()) {
            ACCommand command = this.getCommand(commandLabel);
            if (command == null) {
                this.plugin.communicate(sender.getName(), "Unknown command: &c" + commandLabel);
                return;
            }
            String chanName;
            try {
                chanName = manager.getChannel(command.getCommand()).getName();
            } catch (ChannelNotFoundException ex) {
                this.plugin.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                this.plugin.communicate(sender.getName(), ex.getMessage());
                return;
            }
            ProxiedPlayer target;
            final String channel = command.getCommand();
            switch (command.getType()) {
                case NORMAL:
                    if(!sender.hasPermission("adminchat.channel." + channel) && !sender.hasPermission("adminchat.*")) {
                        sender.sendMessage("§cYou are not allowed to use this command!");
                        return;
                    }
                    StringBuilder msg = new StringBuilder();
                    if (args.length > 0) {
                        for (String s : args) {
                            msg.append(s).append(" ");
                        }
                        String name;
                        if (sender instanceof ProxiedPlayer) {
                            name = sender.getName();
                        } else {
                            name = "CONSOLE";
                        }

                        manager.sendMessage(channel, name, msg.toString().trim());
                    }
                    break;
                case TOGGLE:
                    if(!sender.hasPermission("adminchat.channel." + channel) && !sender.hasPermission("adminchat.*")) {
                        sender.sendMessage("§cYou are not allowed to use this command!");
                        return;
                    }
                    if (sender instanceof ProxiedPlayer) {
                        synchronized (this.toggled) {
                            String chan = this.toggled.get(sender.getName());
                            if (chan != null) {
                                this.toggled.remove(sender.getName());
                                this.plugin.communicate((ProxiedPlayer) sender, "Automatic chat disabled!");
                            } else {
                                this.toggled.put(sender.getName(), channel);
                                this.plugin.communicate((ProxiedPlayer) sender, "Now chatting in channel: '" + channel + "'!");
                            }
                        }
                    }
                    break;
                case MUTE:
                    if(!sender.hasPermission("adminchat.channel." + channel) && !sender.hasPermission("adminchat.*")) {
                        sender.sendMessage("§cYou are not allowed to use this command!");
                        return;
                    }
                    if (args.length < 2) {
                        this.plugin.communicate(sender.getName(), "Invalid arguments.");
                        this.plugin.communicate(sender.getName(), "Usage: &c/<chan>mute <Player> <time>");
                        return;
                    }
                    target = this.plugin.getProxy().getPlayer(args[0]);
                    if (target == null) {
                        this.plugin.communicate(sender.getName(), "Unknown player: &c" + args[0]);
                        return;
                    }
                    try {
                        long time = Long.parseLong(args[1]);
                        final String name = target.getName();
                        try {
                            manager.mute(channel, target.getName());
                            AdminChat.getInstance().communicate(sender.getName(), "You have muted " + args[0] + " from the channel " + channel + " for " + time + " seconds!");
                            this.plugin.getExecutiveManager().runAsyncTask(new Runnable() {
                                public void run() {
                                    try {
                                        manager.unmute(channel, name);
                                    } catch (ChannelNotFoundException ex) {
                                        plugin.communicate(sender.getName(), ex.getMessage());
                                    }
                                }
                            }, time);
                        } catch (ChannelNotFoundException ex) {
                            this.plugin.communicate(sender.getName(), ex.getMessage());
                        }
                    } catch (NumberFormatException exception) {
                        this.plugin.communicate(sender.getName(), "Invalid arguments.");
                        this.plugin.communicate(sender.getName(), "Usage: &c/<chan>mute <Player> <time>");
                    }
                    break;
                case UNMUTE:
                    if(!sender.hasPermission("adminchat.channel." + channel) && !sender.hasPermission("adminchat.*")) {
                        sender.sendMessage("§cYou are not allowed to use this command!");
                        return;
                    }
                    if (args.length == 1) {
                        target = AdminChat.getInstance().getProxy().getPlayer(args[0]);
                        if (target == null) {
                            this.plugin.communicate(sender.getName(), "Unknown Player: &c" + args[0]);
                            return;
                        }
                        try {
                            manager.unmute(channel, target.getName());
                            this.plugin.communicate(sender.getName(), "You have unmuted " + args[0] + " from the channel " + channel);
                        } catch (ChannelNotFoundException ex) {
                            Logger.getLogger(CommandHandler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        this.plugin.communicate(sender.getName(), "Invalid arguments.");
                        this.plugin.communicate(sender.getName(), "Usage: &c/<chan>unmute <Player>");
                    }
                    break;
                default:
                    this.plugin.communicate(sender.getName(), "Unknown Command!");
                    break;
            }
        }
        return;
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