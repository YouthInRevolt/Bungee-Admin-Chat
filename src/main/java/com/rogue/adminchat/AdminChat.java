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

import com.rogue.adminchat.channel.Channel;
import com.rogue.adminchat.channel.ChannelManager;
import com.rogue.adminchat.command.Command;
import com.rogue.adminchat.command.CommandHandler;
import com.rogue.adminchat.executables.ExecutiveManager;
import com.rogue.adminchat.runnable.UpdateRunnable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * AdminChat's main class
 *
 * @author 1Rogue
 * @version 1.3.1
 * @since 1.0
 */
public final class AdminChat extends Plugin {

    private AdminListener listener;
    private ChannelManager cmanager;
    private CommandHandler chandle;
    private ExecutiveManager emanager;
    private boolean isUpdate = false;
    private boolean globalMute = false;
    private static AdminChat instance;

    @Override
    public void onLoad() {
        File file = new File(this.getDataFolder(), "config.yml");
        if (this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        if (!file.exists()) {
            copyDefaultConfigs("config.yml", file);
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        this.getLogger().info("Enabling executive manager...");
        this.emanager = new ExecutiveManager(this);

        this.getLogger().info("Enabling Channel Manager...");
        this.cmanager = new ChannelManager(this);

        this.getLogger().info("Enabling Listener...");
        this.listener = new AdminListener(this);

        this.getLogger().info("Enabling Command Handler...");
        this.chandle = new CommandHandler(this);
        setExecs();

        ProxyServer.getInstance().getPluginManager().registerListener(this, new AdminListener(this));
    }


    /**
     * Sets the command handler as the executor for channel commands
     *
     * @version 1.3.0
     * @since 1.3.0
     */
    public void setExecs() {
        final Map<String, Channel> channels;
        synchronized (channels = this.getChannelManager().getChannels()) {
            for (String cmd : channels.keySet()) {
                ProxyServer.getInstance().getPluginManager().registerCommand(this, new Command(cmd, this));
                ProxyServer.getInstance().getPluginManager().registerCommand(this, new Command(cmd + "toggle", this));
                ProxyServer.getInstance().getPluginManager().registerCommand(this, new Command(cmd + "mute", this));
                ProxyServer.getInstance().getPluginManager().registerCommand(this, new Command(cmd + "unmute", this));
            }
            ProxyServer.getInstance().getPluginManager().registerCommand(this, new Command("adminchat", this));
        }
    }

    @Override
    public void onDisable() {

    }

    /**
     * Reloads AdminChat
     *
     * @version 1.3.0
     * @since 1.3.0
     */
    public void reload(String... names) {
        this.onDisable();
        try {
            Thread.sleep(250L);
        } catch (InterruptedException ex) {
            this.getLogger().log(Level.SEVERE, null, ex);
        }
        listener = null;
        chandle = null;
        cmanager = null;
        isUpdate = false;
        this.onLoad();
        this.onEnable();
        for (String s : names) {
            this.communicate(s, "Reloaded!");
        }
    }

    /**
     * Sends a message to a player through AdminChat
     *
     * @param player  The player to send to
     * @param message The message to send
     * @version 1.3.0
     * @since 1.2.0
     */
    public void communicate(ProxiedPlayer player, String message) {
        if (player != null) {
            player.sendMessage(("&a[&cAdminChat&a] " + message).replace("&", "§"));
        }
    }

    /**
     * Sends a message to a player through AdminChat
     *
     * @param player  The player to send to
     * @param message The message to send
     * @version 1.3.0
     * @since 1.2.0
     */
    public void communicate(String player, String message) {
        this.communicate(getProxy().getPlayer(player), message);
    }

    /**
     * Sets the update status for the plugin
     *
     * @param status The status to set
     * @return The newly set status
     * @version 1.2.0
     * @since 1.2.0
     */
    public boolean setUpdateStatus(boolean status) {
        this.isUpdate = status;
        return this.isUpdate;
    }

    /**
     * Whether or not the plugin is outdated
     *
     * @return True if outdated, false otherwise
     * @version 1.2.0
     * @since 1.2.0
     */
    public boolean isOutOfDate() {
        return this.isUpdate;
    }

    /**
     * Returns AdminChat's channel manager
     *
     * @return Channel Manager class for AdminChat
     * @version 1.3.0
     * @since 1.3.0
     */
    public ChannelManager getChannelManager() {
        return this.cmanager;
    }

    /**
     * Returns AdminChat's Command Handler
     *
     * @return Command Handler class for AdminChat
     * @version 1.3.0
     * @since 1.3.0
     */
    public CommandHandler getCommandHandler() {
        return this.chandle;
    }

    /**
     * Gets the scheduler for plugin tasks
     *
     * @return The scheduler for AdminChat
     * @version 1.3.2
     * @since 1.3.2
     */
    public ExecutiveManager getExecutiveManager() {
        return this.emanager;
    }

    /**
     * Sets whether or not to globally mute everyone without an override perm.
     *
     * @param mute TRue to mute all, false otherwise
     * @version 1.3.2
     * @since 1.3.2
     */
    public void setGlobalMute(boolean mute) {
        this.globalMute = mute;
    }

    public static AdminChat getInstance() {
        return instance;
    }

    /**
     * Copy a file from the ressource (In Jar file) to the actual server)
     *
     * @param ressourceFile Name of the file (ex: config.yml)
     * @param targetFile    File to transfer the data
     */
    private void copyDefaultConfigs(String ressourceFile, File targetFile) {
        try {
            InputStream ressource = this.getResourceAsStream(ressourceFile);
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
