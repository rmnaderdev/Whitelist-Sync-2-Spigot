package com.potatosaucevfx.whitelistsync2;

import com.potatosaucevfx.whitelistsync2.service.BaseService;
import com.potatosaucevfx.whitelistsync2.service.SyncThread;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

/**
 * Utility class to help keep main class clean
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class Utilities {

    public static void StartSyncThread(JavaPlugin plugin, BaseService service) {
        new SyncThread(plugin, service);
        plugin.getLogger().info("Sync Thread Started!");
    }


    public static String FormatOpUsersOutput(ArrayList<String> names) {
        String outstr = "";

        if(names.isEmpty()) {
            outstr = "Op list is empty";
        } else {
            for(int i = 0; i < names.size(); i++) {

                if(i % 5 == 0 && i != 0) {
                    outstr += "\n";
                }

                if(i == names.size() - 1) {
                    outstr += names.get(i);
                } else {
                    outstr += names.get(i) + ", ";
                }

            }
        }

        return outstr;
    }

    public static String FormatWhitelistUsersOutput(ArrayList<String> names) {
        String outstr = "";

        if(names.isEmpty()) {
            outstr = "Whitelist is empty";
        } else {
            for(int i = 0; i < names.size(); i++) {

                if(i % 5 == 0 && i != 0) {
                    outstr += "\n";
                }

                if(i == names.size() - 1) {
                    outstr += names.get(i);
                } else {
                    outstr += names.get(i) + ", ";
                }

            }
        }

        return outstr;
    }

}