package pw.twpi.whitelistsync2.service;

import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.OppedPlayersFileUtilities;
import pw.twpi.whitelistsync2.json.WhitelistedPlayersFileUtilities;
import pw.twpi.whitelistsync2.models.OppedPlayer;
import pw.twpi.whitelistsync2.models.WhitelistedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class SqLiteService implements BaseService {

    // Function used to initialize the database file
    @Override
    public boolean initializeDatabase() {
        WhitelistSync2.LOGGER.info("Setting up the SQLite service...");
        File databaseFile = new File(WhitelistSync2.CONFIG.getString("sqlite.database-path"));
        boolean isSuccess = true;

        try {
            Class.forName("org.sqlite.JDBC").newInstance();
        } catch (Exception e) {
            WhitelistSync2.LOGGER.severe("Failed to init sqlite connector. Is the library missing?");
            e.printStackTrace();
            isSuccess = false;
        }

        // If database does not exist, create a new one
        if (!databaseFile.exists() && isSuccess) {
            String url = "jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path");
            try {
                Connection conn = DriverManager.getConnection(url);

                WhitelistSync2.LOGGER.info("A new database \"" + WhitelistSync2.CONFIG.getString("sqlite.database-path") + "\" has been created.");
                conn.close();
            } catch (SQLException e) {
                // Something is wrong...
                WhitelistSync2.LOGGER.severe("Failed to create new SQLite database file!");
                e.printStackTrace();
                isSuccess = false;
            }
        }

        // Create whitelist table if it doesn't exist.
        if (isSuccess) {
            try {
                Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));

                // If the conn is valid, everything below this will run
                WhitelistSync2.LOGGER.info("Connected to SQLite database successfully!");

                // SQL statement for creating a new table
                String sql = "CREATE TABLE IF NOT EXISTS whitelist (\n"
                        + "	uuid text NOT NULL PRIMARY KEY,\n"
                        + "	name text,\n"
                        + " whitelisted integer NOT NULL);";
                Statement stmt = conn.createStatement();
                stmt.execute(sql);
                stmt.close();

                if (WhitelistSync2.CONFIG.getBoolean("general.sync-ops")) {
                    // SQL statement for creating a new table
                    sql = "CREATE TABLE IF NOT EXISTS op (\n"
                            + "	uuid text NOT NULL PRIMARY KEY,\n"
                            + "	name text,\n"
                            + " isOp integer NOT NULL);";
                    Statement stmt2 = conn.createStatement();
                    stmt2.execute(sql);
                    stmt2.close();
                }

                conn.close();
            } catch (SQLException e) {
                // Something is wrong...
                WhitelistSync2.LOGGER.severe("Error creating op or whitelist table!\n" + e.getMessage());
                e.printStackTrace();
                isSuccess = false;
            }
        }

        return isSuccess;
    }

    @Override
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
        // ArrayList for whitelisted players.
        ArrayList<WhitelistedPlayer> whitelistedPlayers = new ArrayList<>();

        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, name, whitelisted FROM whitelist WHERE whitelisted = 1;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Save queried return to names list.
            while (rs.next()) {
                whitelistedPlayers.add(new WhitelistedPlayer(rs.getString("uuid"), rs.getString("name"), true));
                records++;
            }

            // Total time taken.
            long timeTaken = System.currentTimeMillis() - startTime;

            // WhitelistSync2.LOGGER.debug("Database pulled whitelisted players | Took " + timeTaken + "ms | Read " + records + " records.");

            stmt.close();
            conn.close();
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.severe("Error querying whitelisted players from database!");
            e.printStackTrace();
        }

        return whitelistedPlayers;
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
        // ArrayList for opped players.
        ArrayList<OppedPlayer> oppedPlayers = new ArrayList<>();

        if (WhitelistSync2.CONFIG.getBoolean("general.sync-ops")) {
            try {
                // Keep track of records.
                int records = 0;

                // Connect to database.
                Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));
                long startTime = System.currentTimeMillis();

                String sql = "SELECT uuid, name FROM op WHERE isOp = 1;";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                // Save queried return to names list.
                while (rs.next()) {
                    oppedPlayers.add(new OppedPlayer(rs.getString("uuid"), rs.getString("name"), true));
                    records++;
                }

                // Total time taken.
                long timeTaken = System.currentTimeMillis() - startTime;

                // WhitelistSync2.LOGGER.debug("Database pulled opped players | Took " + timeTaken + "ms | Read " + records + " records.");

                stmt.close();
                conn.close();
            } catch (SQLException e) {
                WhitelistSync2.LOGGER.severe("Error querying opped players from database!");
                e.printStackTrace();
            }

        } else {
            WhitelistSync2.LOGGER.severe("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return oppedPlayers;
    }

    @Override
    public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromLocal() {
        return WhitelistedPlayersFileUtilities.getWhitelistedPlayers();
    }

    @Override
    public ArrayList<OppedPlayer> getOppedPlayersFromLocal() {
        return OppedPlayersFileUtilities.getOppedPlayers();
    }

    @Override
    public boolean copyLocalWhitelistedPlayersToDatabase() {
        // Load local whitelist to memory.
        ArrayList<WhitelistedPlayer> whitelistedPlayers = WhitelistedPlayersFileUtilities.getWhitelistedPlayers();

        // TODO: Start job on thread to avoid lag?
        // Keep track of records.
        int records = 0;
        try {
            // Connect to database.
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));
            long startTime = System.currentTimeMillis();
            // Loop through local whitelist and insert into database.
            for (WhitelistedPlayer player : whitelistedPlayers) {

                if (player.getUuid() != null && player.getName() != null) {
                    PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)");
                    stmt.setString(1, player.getUuid());
                    stmt.setString(2, player.getName());
                    stmt.executeUpdate();
                    stmt.close();

                    records++;
                }
            }
            // Record time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            // WhitelistSync2.LOGGER.debug("Whitelist table updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
            conn.close();

            return true;
        } catch (SQLException e) {
            WhitelistSync2.LOGGER.severe("Failed to update database with local records.");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean copyLocalOppedPlayersToDatabase() {
        // Load local opped players to memory.
        ArrayList<OppedPlayer> oppedPlayers = OppedPlayersFileUtilities.getOppedPlayers();

        if (WhitelistSync2.CONFIG.getBoolean("general.sync-ops")) {
            // TODO: Start job on thread to avoid lag?
            // Keep track of records.
            int records = 0;
            try {
                // Connect to database.
                Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));
                long startTime = System.currentTimeMillis();
                // Loop through local opped players and insert into database.
                for (OppedPlayer player : oppedPlayers) {

                    if (player.getUuid() != null && player.getName() != null) {
                        PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO op(uuid, name, isOp) VALUES (?, ?, 1)");
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getName());
                        stmt.executeUpdate();
                        stmt.close();

                        records++;
                    }
                }
                // Record time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                // WhitelistSync2.LOGGER.debug("Op table updated | Took " + timeTaken + "ms | Wrote " + records + " records.");
                conn.close();

                return true;
            } catch (SQLException e) {
                WhitelistSync2.LOGGER.severe("Failed to update database with local records.");
                e.printStackTrace();
            }
        } else {
            WhitelistSync2.LOGGER.severe("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }

    @Override
    public boolean copyDatabaseWhitelistedPlayersToLocal(Server server) {
        try {
            int records = 0;

            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));
            long startTime = System.currentTimeMillis();

            String sql = "SELECT name, uuid, whitelisted FROM whitelist;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            ArrayList<WhitelistedPlayer> localWhitelistedPlayers = WhitelistedPlayersFileUtilities.getWhitelistedPlayers();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String name = rs.getString("name");
                int whitelisted = rs.getInt("whitelisted");

                if (whitelisted == 1) {
                    if (localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid))) {
                        try {
                            Bukkit.getOfflinePlayer(UUID.fromString(uuid)).setWhitelisted(true);
                            // WhitelistSync2.LOGGER.debug("Added " + name + " to whitelist.");
                            records++;
                        } catch (NullPointerException e) {
                            WhitelistSync2.LOGGER.severe("Player is null?");
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid))) {
                        Bukkit.getOfflinePlayer(UUID.fromString(uuid)).setWhitelisted(false);
                        // WhitelistSync2.LOGGER.debug("Removed " + name + " from whitelist.");
                        records++;
                    }
                }

            }
            long timeTaken = System.currentTimeMillis() - startTime;
            // WhitelistSync2.LOGGER.debug("Copied whitelist database to local | Took " + timeTaken + "ms | Wrote " + records + " records.");

            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            WhitelistSync2.LOGGER.severe("Error querying whitelisted players from database!");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean copyDatabaseOppedPlayersToLocal(Server server) {

        if (WhitelistSync2.CONFIG.getBoolean("general.sync-ops")) {

            try {
                int records = 0;

                Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));
                long startTime = System.currentTimeMillis();

                String sql = "SELECT name, uuid, isOp FROM op;";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                ArrayList<OppedPlayer> localOppedPlayers = OppedPlayersFileUtilities.getOppedPlayers();

                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    String name = rs.getString("name");
                    int opped = rs.getInt("isOp");

                    if (opped == 1) {
                        if (localOppedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid))) {
                            try {
                                Bukkit.getOfflinePlayer(UUID.fromString(uuid)).setOp(true);
                                // WhitelistSync2.LOGGER.debug("Opped " + name + ".");
                                records++;
                            } catch (NullPointerException e) {
                                WhitelistSync2.LOGGER.severe("Player is null?");
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (localOppedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid))) {
                            Bukkit.getOfflinePlayer(UUID.fromString(uuid)).setOp(false);
                            // WhitelistSync2.LOGGER.debug("Deopped " + name + ".");
                            records++;
                        }
                    }
                }
                long timeTaken = System.currentTimeMillis() - startTime;
                // WhitelistSync2.LOGGER.debug("Copied op database to local | Took " + timeTaken + "ms | Wrote " + records + " records.");

                stmt.close();
                conn.close();
                return true;
            } catch (SQLException e) {
                WhitelistSync2.LOGGER.severe("Error querying opped players from database!");
                e.printStackTrace();
            }
        } else {
            WhitelistSync2.LOGGER.severe("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }

    @Override
    public boolean addWhitelistPlayer(OfflinePlayer player) {
        try {
            // Open connection
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));

            // Start time.
            long startTime = System.currentTimeMillis();

            String sql = "INSERT OR REPLACE INTO whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            // WhitelistSync2.LOGGER.debug("Added " + player.getName() + " to whitelist | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            WhitelistSync2.LOGGER.severe("Error adding " + player.getName() + " to whitelist database!");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean addOppedPlayer(OfflinePlayer player) {
        if (WhitelistSync2.CONFIG.getBoolean("general.sync-ops")) {
            try {
                // Open connection
                Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));

                // Start time.
                long startTime = System.currentTimeMillis();

                PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO op(uuid, name, isOp) VALUES (?, ?, 1)");
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.executeUpdate();

                // Time taken.
                long timeTaken = System.currentTimeMillis() - startTime;
                // WhitelistSync2.LOGGER.debug("Database opped " + player.getName() + " | Took " + timeTaken + "ms");
                stmt.close();
                conn.close();
                return true;

            } catch (SQLException e) {
                WhitelistSync2.LOGGER.severe("Error opping " + player.getName() + " !");
                e.printStackTrace();
            }
        } else {
            WhitelistSync2.LOGGER.severe("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }

    @Override
    public boolean removeWhitelistPlayer(OfflinePlayer player) {
        try {
            // Open connection
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));

            // Start time.
            long startTime = System.currentTimeMillis();

            PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO whitelist(uuid, name, whitelisted) VALUES (?, ?, 0)");
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, player.getName());
            stmt.executeUpdate();

            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            // WhitelistSync2.LOGGER.debug("Removed " + player.getName() + " from whitelist | Took " + timeTaken + "ms");
            stmt.close();
            conn.close();
            return true;

        } catch (SQLException e) {
            WhitelistSync2.LOGGER.severe("Error removing " + player.getName() + " to whitelist database!");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean removeOppedPlayer(OfflinePlayer player) {
        if (WhitelistSync2.CONFIG.getBoolean("general.sync-ops")) {
            try {
                // Open connection
                Connection conn = DriverManager.getConnection("jdbc:sqlite:" + WhitelistSync2.CONFIG.getString("sqlite.database-path"));

                // Start time.
                long startTime = System.currentTimeMillis();

                PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO op(uuid, name, isOp) VALUES (?, ?, 0)");
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                stmt.executeUpdate();

                // Time taken
                long timeTaken = System.currentTimeMillis() - startTime;
                // WhitelistSync2.LOGGER.debug("Deopped " + player.getName() + " | Took " + timeTaken + "ms");
                stmt.close();
                conn.close();
                return true;

            } catch (SQLException e) {
                WhitelistSync2.LOGGER.severe("Error deopping " + player.getName() + ".");
                e.printStackTrace();
            }
        } else {
            WhitelistSync2.LOGGER.severe("Op list syncing is currently disabled in your config. "
                    + "Please enable it and restart the server to use this feature.");
        }

        return false;
    }

}
