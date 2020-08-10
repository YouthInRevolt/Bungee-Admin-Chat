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
package com.rogue.adminchat.channel;

import com.rogue.adminchat.AdminChat;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

/**
 * @author 1Rogue
 * @version 1.3.1
 * @since 1.3.0
 */
public class ChannelManager {

    private final Map<String, List<String>> mutes = new HashMap();
    private final Map<String, Channel> channels = new ConcurrentHashMap();
    private final AdminChat plugin;

    public ChannelManager(AdminChat plugin) {
        this.plugin = plugin;
        try {
            setup();
        } catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Error grabbing channels!", ex);
        }
    }

    /**
     * Gets the channel configurations from the channels.yml file, or loads a
     * new one if it does not exist. Also registers appropriate permissions
     */
    private void setup() throws IOException {
        if (this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdir();
        }
        File chan = new File(this.plugin.getDataFolder() + File.separator + "channels.yml");
        if (!chan.exists()) {
            copyDefaultConfigs("channels.yml", chan);
        }

        Configuration yaml;
        try {
            yaml = ConfigurationProvider.getProvider(YamlConfiguration.class).load(chan);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (yaml.getSection("channels") == null) {
            this.plugin.getLogger().severe("No channels found, disabling!");
            return;
        }
        for (String s : yaml.getSection("channels").getKeys()) {
            String format = yaml.getString("channels." + s + ".format");
            String cmd = yaml.getString("channels." + s + ".command");
            if (format != null && cmd != null && !cmd.equalsIgnoreCase("adminchat")) {
                this.plugin.getLogger().log(Level.CONFIG, "Adding command {0}!", cmd);
                this.channels.put(cmd, new Channel(s, cmd, format));
            }
        }
    }

    /**
     * Returns a map of the current channels, with the command as their key
     */
    public Map<String, Channel> getChannels() {
        return this.channels;
    }

    /**
     * Returns a Channel by a requested key. This method is thread-safe.
     */
    public synchronized Channel getChannel(String name) throws ChannelNotFoundException {
        Channel chan = this.channels.get(name);
        if (chan != null) {
            return chan;
        } else {
            throw new ChannelNotFoundException("Unknown Channel: &c" + name);
        }
    }

    /**
     * Checks if there is a channel by the command name
     *
     * @param name The command used to call the channel
     * @return True if exists, false otherwise
     * @version 1.3.2
     * @since 1.3.2
     */
    public synchronized boolean isChannel(String name) {
        return this.channels.containsKey(name);
    }

    /**
     * Parses the format string and sends it to players
     *
     * @param channel The channel to send to, based on command
     * @param name    The user sending the message
     * @param message The message to send to others in the channel
     * @version 1.3.1
     * @since 1.2.0
     */
    public void sendMessage(String channel, String name, String message) {
        if (this.isMuted(name, channel)) {
            this.plugin.communicate(name, "You are muted this channel!");
        } else {
            try {
                Channel chan = this.getChannel(channel);
                String send = chan.getFormat();
                send = send.replace("{NAME}", name);
                send = send.replace("{MESSAGE}", message);

                ProxiedPlayer target = AdminChat.getInstance().getProxy().getPlayer(name);

                for (ProxiedPlayer player : AdminChat.getInstance().getProxy().getPlayers()) {
                    if (player.hasPermission("adminchat.channel." + chan.getName())) {
                        player.sendMessage(send.replace("&", "§").replace("{SERVER}", target.getServer().getInfo().getName()));
                    }
                }
            } catch (ChannelNotFoundException ex) {
                ex.printStackTrace();
                this.plugin.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                this.plugin.communicate(name, ex.getMessage());
            }
        }
    }

    /**
     * Adds passed player names to a mute list. Does not verify the names are
     * players.
     *
     * @param channel Channel to mute in
     * @param names   Names to mute
     * @return True if all names were successfully added.
     * @throws ChannelNotFoundException If no channel is found by the provided name
     * @version 1.3.2
     * @since 1.3.2
     */
    public void mute(String channel, String... names) throws ChannelNotFoundException {
        if (channel != null) {
            if (this.channels.get(channel) == null) {
                throw new ChannelNotFoundException("Unknown channel: " + channel);
            } else {
                for (String name : names) {
                    List<String> muted = new ArrayList<>();
                    if (this.mutes.containsKey(name))
                        muted.addAll(this.mutes.get(name));
                    muted.add(channel);
                    if (muted != null) {
                        this.mutes.put(name, muted);
                    }
                }
            }
        } else {
            for (String name : names) {
                synchronized (this.mutes) {
                    this.mutes.put(name, null);
                }
            }
        }
    }

    /**
     * Unmutes a player within a channel, or globally
     *
     * @param channel The channel to mute in, null if global
     * @param names   Players to mute by name
     * @throws ChannelNotFoundException If no channel is found by the provided name
     * @version 1.3.2
     * @since 1.3.2
     */
    public void unmute(String channel, String... names) throws ChannelNotFoundException {
        if (channel != null) {
            if (this.channels.get(channel) == null) {
                throw new ChannelNotFoundException("Unknown Channel: " + channel);
            } else {
                for (String name : names) {
                    List<String> muted = new ArrayList<>();
                    if (this.mutes.containsKey(name)) {
                        muted.addAll(this.mutes.get(name));
                        if (muted != null) {
                            muted.remove(channel);
                            if (muted.isEmpty())
                                this.mutes.remove(name);
                            else
                                this.mutes.put(name, muted);
                        }
                    }
                }
            }
        } else {
            for (String name : names) {
                synchronized (this.mutes) {
                    this.mutes.put(name, null);
                }
            }
        }
    }

    /**
     * Returns whether or not a player is muted in a channel
     *
     * @param name    The name to check
     * @param channel The channel to check against, null for a global check
     * @return True if muted in the channel, false otherwise
     */
    public synchronized boolean isMuted(String name, String channel) {
        if (this.mutes.containsKey(name)) {
            List<String> chans = this.mutes.get(name);
            return chans == null || chans.contains(channel);
        } else {
            return false;
        }
    }

    /**
     * Copy a file from the ressource (In Jar file) to the actual server)
     *
     * @param ressourceFile Name of the file (ex: config.yml)
     * @param targetFile    File to transfer the data
     */
    private void copyDefaultConfigs(String ressourceFile, File targetFile) {
        try {
            InputStream ressource = AdminChat.getInstance().getResourceAsStream(ressourceFile);
            OutputStream out = new FileOutputStream(targetFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = ressource.read(buf)) > 0) // Copying the data to the new file
            {
                out.write(buf, 0, len);
            }
            out.close();
            ressource.close();
        } catch (Exception ex) {
        }
    }
}
