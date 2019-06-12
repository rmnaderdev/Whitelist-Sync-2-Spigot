package com.potatosaucevfx.whitelistsync2.service;

import com.potatosaucevfx.whitelistsync2.json.OPlistRead;
import com.potatosaucevfx.whitelistsync2.json.WhitelistRead;
import com.potatosaucevfx.whitelistsync2.models.OpUser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

public class MySqlService implements BaseService {

    private Connection conn = null;
    private String S_SQL = "";

    private String url;
    private String database;
    private String username;
    private String password;
    private boolean syncOps;
    private boolean logRemoteChanges;
    private int syncTime;

    private Server server;
    private Logger logger;

    public MySqlService(Server server, FileConfiguration config) {

        this.server = server;
        this.logger = server.getLogger();
        this.database = config.getString("mysql.database-name");
        this.syncOps = config.getBoolean("general.sync-ops");
        this.logRemoteChanges = config.getBoolean("general.log-remote-changes");
        this.url = "jdbc:mysql://" + config.getString("mysql.ip") + ":" + config.getInt("mysql.port") + "/?serverTimezone=UTC&autoReconnect=true&useSSL=false";
        this.username = config.getString("mysql.username");
        this.password = config.getString("mysql.password");
        this.syncTime = config.getInt("mysql.sync-time");

        // Check for lib
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (ClassNotFoundException ex) {
            logger.severe("Failed to connect to the mySQL database! mysql-connector library missing!\n" + ex.getMessage());
        } catch (InstantiationException ex) {
            logger.severe("Failed to connect to the mySQL database! Failed to instantiate library!\n" + ex.getMessage());
        } catch (IllegalAccessException ex) {
            logger.severe("Failed to connect to the mySQL database! mysql-connector library missing!\n" + ex.getMessage());
        }

        // Connect
        try {
            openConnection();
            Statement statement = conn.createStatement();
            logger.info("Connected to MYSQL Database!");
            loadDatabase();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private Connection getConnection() {
        return conn;
    }

    public void openConnection() throws SQLException, ClassNotFoundException {
        if (conn != null && !conn.isClosed()) {
            return;
        }

        synchronized (this) {
            if (conn != null && !conn.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, username, password);
        }
    }

    private boolean loadDatabase() {
        // Create database
        try {

            // Create database
            S_SQL = "CREATE DATABASE IF NOT EXISTS " + database + ";";

            // Create statement
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(S_SQL);
            stmt.execute();

            // Create table
            S_SQL = "CREATE TABLE IF NOT EXISTS " + database + ".whitelist ("
                    + "`uuid` VARCHAR(60) NOT NULL,"
                    + "`name` VARCHAR(20) NOT NULL,"
                    + "`whitelisted` TINYINT NOT NULL DEFAULT 1,"
                    + "PRIMARY KEY (`uuid`)"
                    + ")";
            PreparedStatement stmt2 = conn.prepareStatement(S_SQL);
            stmt2.execute();

            // Create table for op list
            if (syncOps) {
                S_SQL = "CREATE TABLE IF NOT EXISTS " + database + ".op ("
                        + "`uuid` VARCHAR(60) NOT NULL,"
                        + "`name` VARCHAR(20) NOT NULL,"
                        + "`level` INT NOT NULL,"
                        + "`bypassesPlayerLimit` TINYINT NOT NULL DEFAULT 0,"
                        + "`isOp` TINYINT NOT NULL DEFAULT 1,"
                        + "PRIMARY KEY (`uuid`)"
                        + ")";
                PreparedStatement stmt3 = conn.prepareStatement(S_SQL);
                stmt3.execute();

                logger.info("OP Sync is ENABLED!");
            } else {
                logger.info("OP Sync is DISABLED!");
            }

            logger.info("Loaded mySQL database!");

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception ee) {
            ee.printStackTrace();
            return false;
        }
    }

    /**
     * Pushes local json whitelist to the database
     *
     * @param server
     * @return success
     */
    @Override
    public boolean pushLocalWhitelistToDatabase(Server server) {
        // Load local whitelist to memory.
        ArrayList<String> uuids = WhitelistRead.getWhitelistUUIDs();
        ArrayList<String> names = WhitelistRead.getWhitelistNames();

        // Start job on thread to avoid lag.
        // Keep track of records.
        int records = 0;
        try {
            // Connect to database.
            Connection conn1 = getConnection();
            long startTime = System.currentTimeMillis();
            // Loop through local whitelist and insert into database.
            for (int i = 0; i < uuids.size() || i < names.size(); i++) {
                if ((uuids.get(i) != null) && (names.get(i) != null)) {
                    try {
                        PreparedStatement sql = conn1.prepareStatement("INSERT IGNORE INTO " + database + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)");
                        sql.setString(1, uuids.get(i));
                        sql.setString(2, names.get(i));
                        sql.executeUpdate();
                        records++;
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                    }
                }
            }
            // Record time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            logger.info("Wrote " + records + " to whitelist table in " + timeTaken + "ms.");
            return true;
        } catch (SQLException e) {
            logger.severe("Failed to update database with local records.\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Pushes local json op list to the database
     *
     * @param server
     * @return success
     */
    @Override
    public boolean pushLocalOpListToDatabase(Server server) {

        ArrayList<OpUser> opUsers = OPlistRead.getOppedUsers();

        // Start job on thread to avoid lag.
        // Keep track of records.
        int records = 0;
        try {
            // Connect to database.
            Connection conn1 = getConnection();
            // If syncing op list
            if (syncOps) {
                records = 0;
                long opStartTime = System.currentTimeMillis();
                // Loop through ops list and add to DB
                for (OpUser opUser : opUsers) {
                    try {
                        PreparedStatement sql = conn1.prepareStatement("INSERT IGNORE INTO " + database + ".op(uuid, name, level, bypassesPlayerLimit, isOp) VALUES (?, ?, ?, ?, true)");
                        sql.setString(1, opUser.getUuid());
                        sql.setString(2, opUser.getName());
                        sql.setInt(3, opUser.getLevel());
                        sql.setInt(4, opUser.isBypassesPlayerLimit() ? 1 : 0);
                        sql.executeUpdate();
                        records++;
                    } catch (ClassCastException e) {
                        e.printStackTrace();
                    }
                    records++;
                }
                // Record time taken.
                long opTimeTaken = System.currentTimeMillis() - opStartTime;
                logger.info("Wrote " + records + " to op table in " + opTimeTaken + "ms.");
            }

            return true;
        } catch (SQLException e) {
            logger.severe("Failed to update database with local records.\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Pull uuids of whitelisted players from database
     *
     * @param server
     * @return List of whitelisted player uuids
     */
    @Override
    public ArrayList<String> pullWhitelistedUuidsFromDatabase(Server server) {
        // ArrayList for uuids.
        ArrayList<String> uuids = new ArrayList();

        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, whitelisted FROM " + database + ".whitelist";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Add querried results to arraylist.
            while (rs.next()) {
                if (rs.getInt("whitelisted") == 1) {
                    uuids.add(rs.getString("uuid"));
                }
                records++;
            }

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;

        } catch (SQLException e) {
            logger.severe("Error querrying uuids from whitelist database!\n" + e.getMessage());
        }
        return uuids;
    }

    /**
     * Pull uuids of opped players from database
     *
     * @param server
     * @return List of opped player uuids
     */
    @Override
    public ArrayList<String> pullOpUuidsFromDatabase(Server server) {
        // ArrayList for uuids.
        ArrayList<String> uuids = new ArrayList();

        try {
            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT uuid, isOp FROM " + database + ".op";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Add querried results to arraylist.
            while (rs.next()) {
                if (rs.getInt("isOp") == 1) {
                    uuids.add(rs.getString("uuid"));
                }
                records++;
            }

            // Time taken
            long timeTaken = System.currentTimeMillis() - startTime;

        } catch (SQLException e) {
            logger.severe("Error querrying uuids from op database!\n" + e.getMessage());
        }
        return uuids;
    }

    /**
     * Pull names of whitelisted players from database
     *
     * @param server
     * @return List of whitelisted players names
     */
    @Override
    public ArrayList<String> pullWhitelistedNamesFromDatabase(Server server) {
        // ArrayList for names.
        ArrayList<String> names = new ArrayList<String>();

        try {

            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT name, whitelisted FROM " + database + ".whitelist";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Save querried return to names list.
            while (rs.next()) {
                if (rs.getInt("whitelisted") == 1) {
                    names.add(rs.getString("name"));
                }
                records++;
            }

            // Total time taken.
            long timeTaken = System.currentTimeMillis() - startTime;

        } catch (SQLException e) {
            logger.severe("Error querrying names from whitelist database!\n" + e.getMessage());
        }
        return names;
    }

    /**
     * Pull names of opped players from database
     *
     * @param server
     * @return List of opped players names
     */
    @Override
    public ArrayList<String> pullOppedNamesFromDatabase(Server server) {
        // ArrayList for names.
        ArrayList<String> names = new ArrayList();

        try {

            // Keep track of records.
            int records = 0;

            // Connect to database.
            Connection conn = getConnection();
            long startTime = System.currentTimeMillis();

            String sql = "SELECT name, isOp FROM " + database + ".op";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Save querried return to names list.
            while (rs.next()) {
                if (rs.getInt("isOp") == 1) {
                    names.add(rs.getString("name"));
                }
                records++;
            }

            // Total time taken.
            long timeTaken = System.currentTimeMillis() - startTime;

        } catch (SQLException e) {
            logger.severe("Error querrying names from op database!\n" + e.getMessage());
        }
        return names;
    }

    /**
     * Method adds player to whitelist in database
     *
     * @param player
     * @return success
     */
    @Override
    public boolean addPlayerToDatabaseWhitelist(OfflinePlayer player) {
        try {
            // Start time.
            long startTime = System.currentTimeMillis();
            // Open connection
            Connection conn1 = getConnection();
            String sql = "REPLACE INTO " + database + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, 1)";
            PreparedStatement stmt = conn1.prepareStatement(sql);
            stmt.setString(1, String.valueOf(player.getUniqueId()));
            stmt.setString(2, player.getName());
            // Execute statement.
            stmt.execute();
            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            return true;

        } catch (SQLException e) {
            logger.severe("Error adding " + player.getName() + " to database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method removes player from whitelist in database
     *
     * @param player
     * @return success
     */
    @Override
    public boolean removePlayerFromDatabaseWhitelist(OfflinePlayer player) {
        try {
            // Start time.
            long startTime = System.currentTimeMillis();
            // Open connection
            Connection conn1 = getConnection();
            String sql = "REPLACE INTO " + database + ".whitelist(uuid, name, whitelisted) VALUES (?, ?, 0)";
            PreparedStatement stmt = conn1.prepareStatement(sql);
            stmt.setString(1, String.valueOf(player.getUniqueId()));
            stmt.setString(2, player.getName());
            // Execute statement.
            stmt.execute();
            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;
            return true;

        } catch (SQLException e) {
            logger.severe("Error removing " + player.getName() + " to database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method adds player to op list in database
     *
     * @param player
     * @return success
     */
    @Override
    public boolean addPlayerToDatabaseOp(OfflinePlayer player) {
        try {
            // Start time.
            long startTime = System.currentTimeMillis();
            // Open connection
            Connection conn1 = getConnection();
            String sql = "REPLACE INTO " + database + ".op(uuid, name, level, isOp, bypassesPlayerLimit) VALUES (?, ?, ?, true, ?)";
            PreparedStatement stmt = conn1.prepareStatement(sql);
            stmt.setString(1, String.valueOf(player.getUniqueId()));
            stmt.setString(2, player.getName());
            stmt.setInt(3, 4);
            stmt.setInt(4, 1);

            // Execute statement.
            stmt.execute();
            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;

            addPlayerToDatabaseWhitelist(player);   // Whitelist player too

            return true;
        } catch (SQLException e) {
            logger.severe("Error adding " + player.getName() + " to op database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method removes player from op list in database
     *
     * @param player
     * @return success
     */
    @Override
    public boolean removePlayerFromDatabaseOp(OfflinePlayer player) {
        try {
            ArrayList<OpUser> oppedUsers = OPlistRead.getOppedUsers();
            // Start time.
            long startTime = System.currentTimeMillis();
            // Open connection
            Connection conn1 = getConnection();
            String sql = "REPLACE INTO " + database + ".op(uuid, name, level, isOp, bypassesPlayerLimit) VALUES (?, ?, ?, false, ?)";
            PreparedStatement stmt = conn1.prepareStatement(sql);
            stmt.setString(1, String.valueOf(player.getUniqueId()));
            stmt.setString(2, player.getName());

            for (OpUser opUser : oppedUsers) {
                if (opUser.getUuid().equalsIgnoreCase(player.getUniqueId().toString())) {
                    stmt.setInt(3, opUser.getLevel());
                    stmt.setInt(4, opUser.isBypassesPlayerLimit() ? 1 : 0);
                }
            }

            // Execute statement.
            stmt.execute();
            // Time taken.
            long timeTaken = System.currentTimeMillis() - startTime;

            return true;
        } catch (SQLException e) {
            logger.severe("Error removing " + player.getName() + " from op database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method pulls whitelist from database and merges it into the local
     * whitelist
     *
     * @param server
     * @return success
     */
    @Override
    public boolean updateLocalWhitelistFromDatabase(Server server) {
        try {
            int records = 0;

            // Start time
            long startTime = System.currentTimeMillis();

            // Open connection
            Connection conn = getConnection();
            String sql = "SELECT name, uuid, whitelisted FROM " + database + ".whitelist";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            ArrayList<String> localUuids = WhitelistRead.getWhitelistUUIDs();
            while (rs.next()) {
                int whitelisted = rs.getInt("whitelisted");
                String uuid = rs.getString("uuid");
                String name = rs.getString("name");

                if (whitelisted == 1) {
                    if (!localUuids.contains(uuid)) {
                        try {
                            Bukkit.getOfflinePlayer(UUID.fromString(uuid)).setWhitelisted(true);

                            if(logRemoteChanges) {
                                logger.info("Added " + name + " to whitelist.");
                            }

                        } catch (NullPointerException e) {
                            logger.severe("Player is null?\n" + e.getMessage());
                        }
                    }
                } else {
                    if (localUuids.contains(uuid)) {
                        Bukkit.getOfflinePlayer(UUID.fromString(uuid)).setWhitelisted(false);

                        if(logRemoteChanges) {
                            logger.info("Removed " + name + " from whitelist.");
                        }
                    }
                }
                records++;
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            //logger.info("Local whitelist.json up to date!");

            return true;
        } catch (SQLException e) {
            logger.severe("Error querying whitelisted players from database!\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Method pulls op list from database and merges it into the local op list
     *
     * @param server
     * @return success
     */
    @Override
    public boolean updateLocalOpListFromDatabase(Server server) {
        try {
            int records = 0;

            // Start time
            long startTime = System.currentTimeMillis();

            // Open connection
            Connection conn = getConnection();
            String sql = "SELECT name, uuid, isOp FROM " + database + ".op";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            ArrayList<String> localUuids = OPlistRead.getOpsUUIDs();

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String name = rs.getString("name");
                int isOp = rs.getInt("isOp");

                if (isOp == 1) {
                    if (!localUuids.contains(uuid)) {
                        try {
                            Bukkit.getOfflinePlayer(UUID.fromString(uuid)).setOp(true);

                            if(logRemoteChanges) {
                                logger.info("Opped " + name + ".");
                            }

                        } catch (NullPointerException e) {
                            logger.severe("Player is null?\n" + e.getMessage());
                        }
                    }
                } else {
                    if (localUuids.contains(uuid)) {
                        Bukkit.getOfflinePlayer(UUID.fromString(uuid)).setOp(false);

                        if(logRemoteChanges) {
                            logger.info("Deopped " + name + ".");
                        }
                    }
                }
                records++;
            }
            long timeTaken = System.currentTimeMillis() - startTime;
            //logger.debug("Local ops.json up to date!");

            return true;
        } catch (SQLException e) {
            logger.severe("Error querying opped players from database!\n" + e.getMessage());
            return false;
        }
    }
}
