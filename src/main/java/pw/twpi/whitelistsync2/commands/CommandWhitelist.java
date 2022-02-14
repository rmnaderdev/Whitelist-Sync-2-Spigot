package pw.twpi.whitelistsync2.commands;

import net.rmnad.minecraft.forge.whitelistsynclib.services.BaseService;
import pw.twpi.whitelistsync2.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import pw.twpi.whitelistsync2.json.WhitelistedPlayersFileUtilities;

public class CommandWhitelist implements CommandExecutor {

    private JavaPlugin plugin;
    private BaseService service;
    private Server server;

    private final String WL_MANAGE = "whitelistsync2.wl.manage";
    private final String WL_VIEW = "whitelistsync2.wl.view";

    public CommandWhitelist(JavaPlugin plugin, BaseService service) {
        this.plugin = plugin;
        this.service = service;
        this.server = plugin.getServer();
    }

    public boolean onCommand(CommandSender sender, Command command, String primaryCommand, String[] args) {

        if (args.length > 0) {
            //Action for showing list
            if (args[0].equalsIgnoreCase("list")) {
                if(!(sender.hasPermission(WL_VIEW) || sender.hasPermission(WL_MANAGE))) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }

                sender.sendMessage(Utilities.FormatWhitelistedPlayersOutput(service.getWhitelistedPlayersFromDatabase()));

                return true;
            } else if (args[0].equalsIgnoreCase("add")) {
                // Actions for adding a player to whitelist
                if(!sender.hasPermission(WL_MANAGE)) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }

                if (args.length > 1) {

                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);

                    if (player != null) {

                        if (service.addWhitelistPlayer(player.getUniqueId(), player.getName())) {
                            player.setWhitelisted(true);
                            sender.sendMessage(player.getName() + " added to the whitelist.");
                        } else {
                            sender.sendMessage("Error adding " + player.getName() + " from whitelist!");
                        }

                    } else {
                        sender.sendMessage("User " + args[1] + " not found!");
                    }

                } else {
                    sender.sendMessage("You must specify a name to add to the whitelist!");
                }

                return true;
            } else if (args[0].equalsIgnoreCase("remove")) {
                // Actions for removing player from whitelist
                if(!sender.hasPermission(WL_MANAGE)) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }

                if (args.length > 1) {

                    OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                    if (player != null) {

                        if (service.removeWhitelistPlayer(player.getUniqueId(), player.getName())) {
                            player.setWhitelisted(false);
                            sender.sendMessage(player.getName() + " removed from the whitelist.");
                        } else {
                            sender.sendMessage("Error removing " + player.getName() + " from whitelist!");
                        }

                    } else {
                        sender.sendMessage("You must specify a valid name to remove from the whitelist!");
                    }

                }

                return true;
            } else if (args[0].equalsIgnoreCase("sync")) {
                // Sync Database to server
                if(!sender.hasPermission(WL_MANAGE)) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }

                boolean status = service.copyDatabaseWhitelistedPlayersToLocal(
                        WhitelistedPlayersFileUtilities.getWhitelistedPlayers(),
                        (uuid, name) -> {
                            Bukkit.getOfflinePlayer(uuid).setWhitelisted(true);
                        },
                        (uuid, name) -> {
                            Bukkit.getOfflinePlayer(uuid).setWhitelisted(false);
                        }
                );

                if (status) {
                    sender.sendMessage("Local up to date with database!");
                } else {
                    sender.sendMessage("Error syncing local to database!");
                }

                return true;
            } else if (args[0].equalsIgnoreCase("copyservertodatabase")) {
                // Sync server to database
                if(!sender.hasPermission(WL_MANAGE)) {
                    sender.sendMessage("You do not have permission to use this command.");
                    return true;
                }

                boolean status = service.copyLocalWhitelistedPlayersToDatabase(WhitelistedPlayersFileUtilities.getWhitelistedPlayers());

                if (status) {
                    sender.sendMessage("Pushed local to database!");
                } else {
                    sender.sendMessage("Error pushing local to database!");
                }

                return true;
            }
        }

        return false;
    }
}
