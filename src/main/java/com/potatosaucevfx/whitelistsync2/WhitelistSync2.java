package com.potatosaucevfx.whitelistsync2;

import com.potatosaucevfx.whitelistsync2.commands.CommandOp;
import com.potatosaucevfx.whitelistsync2.commands.CommandWhitelist;
import com.potatosaucevfx.whitelistsync2.service.BaseService;
import com.potatosaucevfx.whitelistsync2.service.MySqlService;
import com.potatosaucevfx.whitelistsync2.service.SqLiteService;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class WhitelistSync2 extends JavaPlugin {

    public FileConfiguration config = getConfig();
    public Server server = getServer();

    public static String SERVER_FILEPATH;

    // Database Service
    public static BaseService whitelistService;

    @Override
    public void onEnable() {
        getLogger().info("Setting up Whitelist Sync!");

        SERVER_FILEPATH = getServer().getWorldContainer().getAbsolutePath();

        server.setWhitelist(true);

        // Setup config
        LoadConfiguration();

        // Setup Services
        LoadServices();

        // Commands
        this.getCommand("wl").setExecutor(new CommandWhitelist(this, whitelistService));
        this.getCommand("wlop").setExecutor(new CommandOp(this, whitelistService));

        Utilities.StartSyncThread(this, whitelistService);
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down Whitelist Sync!");
    }


    public void LoadConfiguration() {
        config.options().copyDefaults(true);
        saveConfig();
    }

    public void LoadServices() {
        if(config.getString("general.sync-mode").equalsIgnoreCase("SQLITE")) {
            whitelistService = new SqLiteService(this);
            getLogger().info("Database setup!");
        }
        else if(config.getString("general.sync-mode").equalsIgnoreCase("MYSQL")) {
            whitelistService = new MySqlService(server, config);
            getLogger().info("Database setup!");
        }
        else {
            getLogger().severe("Please check what sync-mode is set in the config and make sure it is set to a supported mode!");
            getLogger().severe("Failed to setup Whitelist Sync Database!");
        }


    }

}
