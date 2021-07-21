package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Logr;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.h2.tools.Server;

import java.sql.*;

public class DatabaseManager {
    private final static int CURRENT_DB_VERSION = 3;

    private static DatabaseManager singletonInstance = null;
    private Connection con = null;
    private boolean useMySql;
    private String connectString;
    private Server h2Server = null;
    private String user;
    private String pass;

    private DatabaseManager(BGHR plugin) {
        FileConfiguration config = plugin.getConfig();

        useMySql = config.getBoolean("use mysql instead of h2", false);

        if (useMySql) {
            String hostname = config.getString("mysql.hostname", "localhost");
            String port = config.getString("mysql.port", "3306");
            String database = config.getString("mysql.database", "BGHR");
            this.user = config.getString("mysql.user", "minecraft");
            this.pass = config.getString("mysql.pass", "hunter2");
            String arguments = config.getString("mysql.arguments", " ");

            connectString = String.format("jdbc:mysql://%s:%s/%s%s", hostname, port, database, arguments.length() > 1 ? "?" + arguments : "");
            try {
                getConnection();
            } catch (SQLException e) {
                Logr.warn("Could not connect to MySQL: " + e.getMessage());
                Logr.warn("Using H2 database instead...");
                useMySql = false;
            }
        }
        if (!useMySql) {
            try {
                h2Server = Server.createTcpServer("-ifNotExists").start();
            } catch (SQLException e) {
                Bukkit.getLogger().warning("could not create h2 server. is it open elsewhere?");
                e.printStackTrace();
            }
            if (h2Server != null) {
                connectString = String.format("jdbc:h2:tcp://localhost/%s/plugin_data;mode=MySQL;DATABASE_TO_LOWER=TRUE",
                                        plugin.getDataFolder().getAbsolutePath());
                this.user = "bghr";
                this.pass = "bghr";
            }
        }
    }

    public static void initialize(BGHR plugin) {
            singletonInstance = new DatabaseManager(plugin);
        try {
            singletonInstance.setupTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DatabaseManager get() {
        if (singletonInstance == null)
            throw new InstantiationError("must be initialized first using .get(plugin)");
        return singletonInstance;
    }

    public Connection getConnection() throws SQLException {
        if (con != null && !con.isClosed())
            return con;
        con = DriverManager.getConnection(connectString, user, pass);
        return con;
    }

    public boolean isUsingMySql() {
        return useMySql;
    }

    private void setupTables() throws SQLException {
        getConnection();

        try (PreparedStatement createStats = con.prepareStatement("CREATE TABLE IF NOT EXISTS player_stats (" +
                "uuid CHAR(36) PRIMARY KEY, " +
                "played INT," +
                "kills INT," +
                "killstreak INT," +
                "deaths INT," +
                "wins INT," +
                "secondplaces INT," +
                "totaltime BIGINT," +
                "quits INT," +
                "damagedealt BIGINT," +
                "damagetaken BIGINT," +
                "activeabilities INT," +
                "chests INT," +
                "itemslooted INT," +
                "arrowsshot INT," +
                "foodeaten INT)");
             PreparedStatement createSettings = con.prepareStatement("CREATE TABLE IF NOT EXISTS player_settings (" +
                 "uuid CHAR(36) PRIMARY KEY," +
                 "showmain BOOLEAN," +
                 "showhelp BOOLEAN," +
                 "defaultkit VARCHAR(24)" +
                 ")");
             PreparedStatement createVersionTable = con.prepareStatement("CREATE TABLE IF NOT EXISTS db_version (" +
                 "lockcol CHAR(1) PRIMARY KEY," +
                 "version TINYINT)");
             PreparedStatement createDataTable = con.prepareStatement("CREATE TABLE IF NOT EXISTS player_data (" +
                 "uuid CHAR(36) PRIMARY KEY," +
                 "lastworld CHAR(36)," +
                 "lastx INT," +
                 "lasty INT," +
                 "lastz INT," +
                 "inventory TEXT," +
                 "armor TEXT," +
                 "enderchest TEXT)")) {

            createStats.execute();
            createSettings.execute();
            createVersionTable.execute();
            createDataTable.execute();

            if (!databaseIsCurrent())
                updateV3();

            Logr.info("Connected to " + (useMySql ? "MySQL" : "H2" + " database."));
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean databaseIsCurrent() throws SQLException {
        int foundVersion = 0;

        getConnection();
        try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM db_version WHERE lockcol='v'")) {
            ResultSet result = stmt.executeQuery();
            if (result.next()) {
                foundVersion = result.getInt("version");
            }
            else {
                PreparedStatement setVer = con.prepareStatement("INSERT INTO db_version (lockcol, version) VALUES (?, ?)");
                setVer.setString(1, "v");
                setVer.setInt(2, CURRENT_DB_VERSION);
                setVer.execute();
                setVer.close();
                con.close();
                result.close();
                return true;
            }
            con.close();
            result.close();

            if (foundVersion < CURRENT_DB_VERSION) {
                Logr.info("Updating current database to new format...");
                return false;
            } else {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    private void updateV3() {
        try {
            Logr.info("Updating database to v3...");
            getConnection();
            PreparedStatement s = con.prepareStatement("ALTER TABLE player_stats ADD chests INT");
            s.execute();
            s.close();

            s = con.prepareStatement("DELETE FROM db_version");
            s.execute();
            s.close();;

            s = con.prepareStatement("ALTER TABLE db_version ADD lock CHAR(1) NOT NULL");
            s.execute();
            s.close();;

            s = con.prepareStatement("ALTER TABLE db_version MODIFY version TINYINT NOT NULL");
            s.execute();
            s.close();;

            s = con.prepareStatement("INSERT INTO db_version (version, lock) VALUES (?,?)");
            s.setInt(1, CURRENT_DB_VERSION);
            s.setString(2, "v");
            s.execute();
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
