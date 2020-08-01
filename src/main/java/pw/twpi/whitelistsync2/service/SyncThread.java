package pw.twpi.whitelistsync2.service;

import pw.twpi.whitelistsync2.WhitelistSync2;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class SyncThread {

    private final JavaPlugin plugin;
    private final BaseService service;

    public SyncThread(JavaPlugin plugin, BaseService service) {
        this.plugin = plugin;
        this.service = service;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (service.getClass().equals(MySqlService.class)) {
                while (plugin.isEnabled()) {
                    service.copyDatabaseWhitelistedPlayersToLocal(plugin.getServer());

                    if (plugin.getConfig().getBoolean("general.sync-ops")) {
                        service.copyDatabaseOppedPlayersToLocal(plugin.getServer());
                    }

                    try {
                        Thread.sleep(plugin.getConfig().getInt("mysql.sync-time") * 1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            } else if (service.getClass().equals(SqLiteService.class)) {

                while (plugin.isEnabled()) {
                    service.copyDatabaseWhitelistedPlayersToLocal(plugin.getServer());

                    if (plugin.getConfig().getBoolean("general.sync-ops")) {
                        service.copyDatabaseOppedPlayersToLocal(plugin.getServer());
                    }

                    try {
                        Thread.sleep(plugin.getConfig().getInt("sqlite.sync-time") * 1000);
                    } catch (InterruptedException e) {
                    }
                }

            } else {
                WhitelistSync2.LOGGER.severe("Error in the Sync Thread! "
                        + "Nothing will be synced! Please report to author!");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                }
            }
        });

    }

}