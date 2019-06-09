package com.potatosaucevfx.whitelistsync2.service;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.*;
import java.io.IOException;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class SyncThread {

    private final JavaPlugin plugin;
    private final BaseService service;

    // Watch Listener
    private FileSystem fileSystem;
    private WatchService watcher;

    public SyncThread(JavaPlugin plugin, BaseService service) {
        this.plugin = plugin;
        this.service = service;

        final JavaPlugin _plugin = plugin;
        final BaseService _service = service;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            public void run() {
                if (_service.getClass().equals(MySqlService.class)) {
                    while (_plugin.isEnabled()) {
                        _service.updateLocalWhitelistFromDatabase(_plugin.getServer());

                        if (_plugin.getConfig().getBoolean("general.sync-ops")) {
                            _service.updateLocalOpListFromDatabase(_plugin.getServer());
                        }

                        try {
                            Thread.sleep(_plugin.getConfig().getInt("mysql.sync-time") * 1000);
                        } catch (InterruptedException e) {
                        }
                    }
                } else if (_service.getClass().equals(SqLiteService.class)) {

                    if (_plugin.getConfig().getString("sqlite.mode").equalsIgnoreCase("INTERVAL")) {
                        while (_plugin.isEnabled()) {
                            _service.updateLocalWhitelistFromDatabase(_plugin.getServer());

                            if (_plugin.getConfig().getBoolean("general.sync-ops")) {
                                _service.updateLocalOpListFromDatabase(_plugin.getServer());
                            }

                            try {
                                Thread.sleep(_plugin.getConfig().getInt("sqlite.sync-time") * 1000);
                            } catch (InterruptedException e) {
                            }
                        }
                    } else if (_plugin.getConfig().getString("sqlite.mode").equalsIgnoreCase("LISTENER")) {
                        checkSQliteDB(_plugin);
                    }

                } else {
                    _plugin.getLogger().severe("Error in the Sync Thread! "
                            + "Nothing will be synced! Please report to author!");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });

    }

    private void checkSQliteDB(JavaPlugin plugin) {
        try {
            this.fileSystem = FileSystems.getDefault();
            this.watcher = fileSystem.newWatchService();

            Path dataBasePath = fileSystem.getPath(plugin.getConfig().getString("sqlite.database-path").replace("whitelist.db", ""));
            dataBasePath.register(watcher, ENTRY_MODIFY);

        } catch (IOException e) {
            plugin.getLogger().severe("Error finding whitelist database file. "
                    + "This should not happen, please report.\n" + e.getMessage());
        }

        while (plugin.isEnabled()) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // Test if whitelist is changed
                if (event.context().toString().equalsIgnoreCase("whitelist.db")) {
                    service.updateLocalWhitelistFromDatabase(plugin.getServer());

                    if (plugin.getConfig().getBoolean("general.sync-ops")) {
                        service.updateLocalOpListFromDatabase(plugin.getServer());
                    }
                }
            }

            // Reset the key -- this step is critical if you want to
            // receive further watch events.  If the key is no longer valid,
            // the directory is inaccessible so exit the loop.
            boolean valid = key.reset();
            if (!valid) {
                break;
            }

            try {
                Thread.sleep(plugin.getConfig().getInt("sqlite.listener-time") * 1000);
            } catch (InterruptedException e) {
            }

        }
    }

}