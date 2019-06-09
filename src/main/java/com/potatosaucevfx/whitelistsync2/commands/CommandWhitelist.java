package com.potatosaucevfx.whitelistsync2.commands;

import com.potatosaucevfx.whitelistsync2.Utilities;
import com.potatosaucevfx.whitelistsync2.service.BaseService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandWhitelist implements CommandExecutor {

    private JavaPlugin plugin;
    private BaseService service;
    private Server server;

    private final String USAGE_STRING = "/wl <list|add|remove|sync|copyServerToDatabase>";

    public CommandWhitelist(JavaPlugin plugin, BaseService service) {
        this.plugin = plugin;
        this.service = service;
        this.server = plugin.getServer();
    }

    public boolean onCommand(CommandSender sender, Command command, String primaryCommand, String[] args) {
        if (args.length > 0) {
            if (args.length > 0) {
                //Action for showing list
                if (args[0].equalsIgnoreCase("list")) {

                    sender.sendMessage(Utilities.FormatWhitelistUsersOutput(service.pullWhitelistedNamesFromDatabase(server)));

                } // Actions for adding a player to whitelist
                else if (args[0].equalsIgnoreCase("add")) {

                    if (args.length > 1) {

                        OfflinePlayer user = Bukkit.getOfflinePlayer(args[1]);

                        if (user != null) {

                            if (service.addPlayerToDatabaseWhitelist(user)) {
                                user.setWhitelisted(true);
                                sender.sendMessage(user.getName() + " added to the whitelist.");
                            } else {
                                sender.sendMessage("Error adding " + user.getName() + " from whitelist!");
                                return false;
                            }

                        } else {
                            sender.sendMessage("User " + args[1] + " not found!");
                            return false;
                        }

                    } else {
                        sender.sendMessage("You must specify a name to add to the whitelist!");
                        return false;
                    }
                } // Actions for removing player from whitelist
                else if (args[0].equalsIgnoreCase("remove")) {

                    if (args.length > 1) {

                        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                        if (player != null) {

                            if (service.removePlayerFromDatabaseWhitelist(player)) {
                                player.setWhitelisted(false);
                                sender.sendMessage(player.getName() + " removed from the whitelist.");
                            } else {
                                sender.sendMessage("Error removing " + player.getName() + " from whitelist!");
                                return false;
                            }

                        } else {
                            sender.sendMessage("You must specify a valid name to remove from the whitelist!");
                            return false;
                        }

                    }

                } // Sync Database to server
                else if (args[0].equalsIgnoreCase("sync")) {

                    if (service.updateLocalWhitelistFromDatabase(server)) {
                        sender.sendMessage("Local up to date with database!");
                    } else {
                        sender.sendMessage("Error syncing local to database!");
                        return false;
                    }

                } // Sync server to database
                else if (args[0].equalsIgnoreCase("copyservertodatabase")) {

                    if (service.pushLocalWhitelistToDatabase(server)) {
                        sender.sendMessage("Pushed local to database!");
                    } else {
                        sender.sendMessage("Error pushing local to database!");
                        return false;
                    }

                } else {
                    sender.sendMessage(USAGE_STRING);
                    return false;
                }
            } else {
                sender.sendMessage(USAGE_STRING);
                return false;
            }
        } else {
            sender.sendMessage(USAGE_STRING);
            return false;
        }

        return true;
    }
}
