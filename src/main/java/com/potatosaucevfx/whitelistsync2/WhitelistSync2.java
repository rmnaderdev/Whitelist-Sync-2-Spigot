package com.potatosaucevfx.whitelistsync2;

import org.bukkit.plugin.java.JavaPlugin;

public class WhitelistSync2 extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("onEnable is called.");
    }

    @Override
    public void onDisable() {
        getLogger().info("onDisable is called.");
    }

}
