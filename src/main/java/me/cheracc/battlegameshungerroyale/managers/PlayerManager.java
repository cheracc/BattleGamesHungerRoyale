package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.PlayerSettings;
import me.cheracc.battlegameshungerroyale.types.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerManager {
    private final static int CURRENT_DB_VERSION = 1;

    private static PlayerManager singletonInstance = null;
    private final List<PlayerData> loadedPlayers = new ArrayList<>();
    private final BukkitTask databaseUpdater;
    Connection databaseConnection;

    private PlayerManager() {
        if (!databaseIsCurrent())
            setupTables();
        databaseUpdater = databaseUpdater();
    }

    public @NotNull PlayerData getPlayerData(UUID uuid) {
        for (PlayerData d : loadedPlayers) {
            if (d.getUuid().equals(uuid))
                return d;
        }
        PlayerData data = new PlayerData(uuid);
        loadedPlayers.add(data);
        return data;
    }

    public @NotNull PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public static PlayerManager getInstance() {
        if (singletonInstance == null)
            singletonInstance = new PlayerManager();
        return singletonInstance;
    }

    public void initialize() {
        setupTables();
    }

    public void disable() {
        databaseUpdater.cancel();
        try {
            if (databaseConnection != null && !databaseConnection.isClosed())
                databaseConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getConnection() {
        try {
            if (databaseConnection != null && databaseConnection.isClosed())
                databaseConnection = null;
            if (databaseConnection == null) {
                BGHR plugin = BGHR.getPlugin();
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
                "version TINYINT)")) {
            createStats.execute();
            createSettings.execute();
            createVersionTable.execute();

            PreparedStatement addVersion = databaseConnection.prepareStatement("INSERT INTO db_version (version) VALUES (?)");
            addVersion.setInt(1, CURRENT_DB_VERSION);
            addVersion.executeUpdate();
            databaseConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void savePlayer(PlayerData data) {
        PlayerStats stats = data.getStats();
        PlayerSettings settings = data.getSettings();

        try {
            getConnection();
            PreparedStatement updateSettings = databaseConnection.prepareStatement(
                    "INSERT INTO player_settings (uuid,showmain,showhelp,defaultkit) VALUES (?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "showmain=VALUES(showmain)," +
                    "showhelp=VALUES(showhelp)," +
                    "defaultkit=VALUES(defaultkit);");
            updateSettings.setObject(1, data.getUuid());
            updateSettings.setBoolean(2, settings.isShowMainScoreboard());
            updateSettings.setBoolean(3, settings.isShowHelp());
            updateSettings.setString(4, settings.getDefaultKit());
            updateSettings.executeUpdate();

            PreparedStatement updateStats = databaseConnection.prepareStatement(
                    "INSERT INTO player_stats (uuid,played,kills,killstreak,deaths,wins,secondplaces,totaltime,quits,damagedealt,damagetaken,activeabilities,usedkits,playedmaps) VALUES " +
                    "(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                    "played=VALUES(played)," +
                    "kills=VALUES(kills)," +
                    "killstreak=VALUES(killstreak)," +
                    "deaths=VALUES(deaths)," +
                    "wins=VALUES(wins)," +
                    "secondplaces=VALUES(secondplaces)," +
                    "totaltime=VALUES(totaltime)," +
                    "quits=VALUES(quits)," +
                    "damagedealt=VALUES(damagedealt)," +
                    "damagetaken=VALUES(damagetaken)," +
                    "usedkits=VALUES(usedkits)," +
                    "playedmaps=VALUES(playedmaps)," +
                    "activeabilities=VALUES(activeabilities);");
            updateStats.setObject(1, data.getUuid());
            updateStats.setInt(2, stats.getPlayed());
            updateStats.setInt(3, stats.getKills());
            updateStats.setInt(4, stats.getKillStreak());
            updateStats.setInt(5, stats.getDeaths());
            updateStats.setInt(6, stats.getWins());
            updateStats.setInt(7, stats.getSecondPlaceFinishes());
            updateStats.setLong(8, stats.getTotalTimePlayed());
            updateStats.setInt(9, stats.getGamesQuit());
            updateStats.setInt(10, stats.getDamageDealt());
            updateStats.setInt(11, stats.getDamageTaken());
            updateStats.setInt(12, stats.getActiveAbilitiesUsed());
            updateStats.setObject(13, stats.getUsedKits());
            updateStats.setObject(14, stats.getPlayedMaps());
            updateStats.execute();
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
                            savePlayer(data);
                            data.setModified(false);
                        }
                    }.runTaskAsynchronously(BGHR.getPlugin());
                }
            }
            }
        };
        return task.runTaskTimer(BGHR.getPlugin(), 600L, 200L);
    }
}
