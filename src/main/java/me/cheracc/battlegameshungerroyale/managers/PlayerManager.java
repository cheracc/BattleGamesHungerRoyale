package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerManager {
    private final static int CURRENT_DB_VERSION = 2;

    private static PlayerManager singletonInstance = null;
    private BGHR plugin;
    private final List<PlayerData> loadedPlayers = new CopyOnWriteArrayList<>();
    private BukkitTask databaseUpdater;
    private BukkitTask playerDataUpdater;
    private Connection databaseConnection;

    private PlayerManager() {
    }

    public boolean isPlayerDataLoaded(Player player) {
        for (PlayerData data : loadedPlayers) {
            if (data.getUuid().equals(player.getUniqueId()) && data.isLoaded())
                return true;
        }
        return false;
    }

    public @NotNull PlayerData getPlayerData(UUID uuid) {
        for (PlayerData d : loadedPlayers) {
            if (d.getUuid().equals(uuid))
                return d;
        }
        return new PlayerData(uuid);
    }

    public @NotNull PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public void thisPlayerIsLoaded(PlayerData data) {
        if (!isPlayerDataLoaded(data.getPlayer()))
            loadedPlayers.add(data);
        else
            Bukkit.getLogger().warning("data for " + data.getPlayer().getName() + " is already loaded");
    }

    public static PlayerManager getInstance() {
        if (singletonInstance == null)
            singletonInstance = new PlayerManager();
        return singletonInstance;
    }

    public void initialize(BGHR plugin) {
        this.plugin = plugin;
        if (!databaseIsCurrent())
            setupTables();
        databaseUpdater = databaseUpdater();
        playerDataUpdater = playerDataUpdater();
    }

    public void disable() {
        databaseUpdater.cancel();
        playerDataUpdater.cancel();
        try {
            if (databaseConnection != null && !databaseConnection.isClosed())
                databaseConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TODO move all db stuff into PlayerData
    private void getConnection() {
        try {
            if (databaseConnection != null && databaseConnection.isClosed())
                databaseConnection = null;
            if (databaseConnection == null) {
                Class.forName("org.h2.Driver");
                databaseConnection = DriverManager.getConnection("jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/plugin_data;mode=MySQL;DATABASE_TO_LOWER=TRUE" , "bghr", "bghr");
            }
        } catch (SQLException|ClassNotFoundException e) {
                Bukkit.getLogger().warning("cannot access or create database file in plugins folder");
                e.printStackTrace();
            }
    }

    private boolean databaseIsCurrent() {
        getConnection();
        try (PreparedStatement stmt = databaseConnection.prepareStatement("SELECT * FROM db_version")) {
            ResultSet result = stmt.executeQuery();
            if (result.next())
                return result.getInt("version") == CURRENT_DB_VERSION;
            databaseConnection.close();
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    private void setupTables() {
        getConnection();

        try (PreparedStatement createStats = databaseConnection.prepareStatement("CREATE TABLE IF NOT EXISTS player_stats (" +
                "uuid UUID PRIMARY KEY, " +
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
                "usedkits ARRAY," +
                "playedmaps ARRAY," +
                "activeabilities INT)");
        PreparedStatement createSettings = databaseConnection.prepareStatement("CREATE TABLE IF NOT EXISTS player_settings (" +
                "uuid UUID PRIMARY KEY," +
                "showmain BOOLEAN," +
                "showhelp BOOLEAN," +
                "defaultkit VARCHAR(24)" +
                ")");
        PreparedStatement createVersionTable = databaseConnection.prepareStatement("CREATE TABLE IF NOT EXISTS db_version (" +
                "version TINYINT)");
        PreparedStatement createDataTable = databaseConnection.prepareStatement("CREATE TABLE IF NOT EXISTS player_data (" +
                "uuid UUID PRIMARY KEY," +
                "lastworld UUID," +
                "lastx INT," +
                "lasty INT," +
                "lastz INT," +
                "inventory TEXT," +
                "armor TEXT," +
                "enderchest TEXT)"))

        {
            createStats.execute();
            createSettings.execute();
            createVersionTable.execute();
            createDataTable.execute();

            PreparedStatement addVersion = databaseConnection.prepareStatement("INSERT INTO db_version (version) VALUES (?)");
            addVersion.setInt(1, CURRENT_DB_VERSION);
            addVersion.executeUpdate();
            databaseConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private BukkitTask databaseUpdater() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerData data : loadedPlayers) {
                    if (data.isModified()) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                data.save();
                            }
                        }.runTaskAsynchronously(plugin);
                    }
                }
            }
        };
        return task.runTaskTimer(plugin, 600L, 200L);
    }

    private BukkitTask playerDataUpdater() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                List<PlayerData> toRemove = new ArrayList<>();

                for (PlayerData data : loadedPlayers) {
                    Player p = data.getPlayer();
                    if (!data.isModified() && data.isLoaded() && (p == null || !p.isOnline())) {
                        toRemove.add(data);
                        continue;
                    }

                    if (p != null && !MapManager.getInstance().isThisAGameWorld(p.getWorld())) {
                        data.saveInventory(false);
                        data.setLastLocation(p.getLocation());
                    }
                }

                toRemove.forEach(loadedPlayers::remove);
            }
        };
        return task.runTaskTimer(plugin, 600L, 200L);
    }

}
