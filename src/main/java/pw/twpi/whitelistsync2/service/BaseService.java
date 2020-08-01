package pw.twpi.whitelistsync2.service;

import pw.twpi.whitelistsync2.models.OppedPlayer;
import pw.twpi.whitelistsync2.models.WhitelistedPlayer;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.util.ArrayList;

public interface BaseService {

    public boolean initializeDatabase();


    // Getter functions
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase();
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase();

    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromLocal();
    public ArrayList<OppedPlayer> getOppedPlayersFromLocal();


    // Syncing functions
    public boolean copyLocalWhitelistedPlayersToDatabase();
    public boolean copyLocalOppedPlayersToDatabase();

    public boolean copyDatabaseWhitelistedPlayersToLocal(Server server);
    public boolean copyDatabaseOppedPlayersToLocal(Server server);


    // Addition functions
    public boolean addWhitelistPlayer(OfflinePlayer player);
    public boolean addOppedPlayer(OfflinePlayer player);


    // Removal functions
    public boolean removeWhitelistPlayer(OfflinePlayer player);
    public boolean removeOppedPlayer(OfflinePlayer player);
}
