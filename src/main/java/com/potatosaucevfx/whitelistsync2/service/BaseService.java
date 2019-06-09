package com.potatosaucevfx.whitelistsync2.service;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public interface BaseService {

    // Pushed local whitelist to database
    public boolean pushLocalWhitelistToDatabase(Server server);

    // Pushed local op list to database
    public boolean pushLocalOpListToDatabase(Server server);

    // Gets ArrayList of uuids whitelisted in database.
    public ArrayList<String> pullWhitelistedUuidsFromDatabase(Server server);

    // Gets ArrayList of uuids ops in database.
    public ArrayList<String> pullOpUuidsFromDatabase(Server server);

    // Gets ArrayList of names whitelisted in database.
    public ArrayList<String> pullWhitelistedNamesFromDatabase(Server server);

    // Gets ArrayList of names ops in database.
    public ArrayList<String> pullOppedNamesFromDatabase(Server server);

    // Adds player to database whitelist.
    public boolean addPlayerToDatabaseWhitelist(OfflinePlayer player);

    // Adds op player to database.
    public boolean addPlayerToDatabaseOp(OfflinePlayer player);

    // Removes player from database.
    public boolean removePlayerFromDatabaseWhitelist(OfflinePlayer player);

    // Removes op player from database.
    public boolean removePlayerFromDatabaseOp(OfflinePlayer player);

    // Copies whitelist from database to server.
    public boolean updateLocalWhitelistFromDatabase(Server server);

    // Copies op list from database to server.
    public boolean updateLocalOpListFromDatabase(Server server);
}
