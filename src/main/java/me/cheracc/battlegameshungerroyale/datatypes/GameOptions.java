package me.cheracc.battlegameshungerroyale.datatypes;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameOptions {
    private File configFile;
    private final List<MapData> maps = new ArrayList<>();
    private int livesPerPlayer;
    private int playersNeededToStart;
    private int pregameTime;
    private int invincibilityTime;
    private int mainPhaseTime;
    private int borderTime;
    private int postGameTime;
    private boolean allowRegularBuilding;
    private StartType startType;
    public enum StartType { HUNGERGAMES, ELYTRA }

    public GameOptions() {
        loadConfig(null);
    }

    public void loadConfig(File file) {
        FileConfiguration config = new YamlConfiguration();

        if (file != null) {

            try {
                config.load(file);
                configFile = file;
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        maps.clear();
        for (String s : config.getStringList("maps")) {
            for (MapData map : MapManager.getInstance().getMaps()) {
                if (map.getMapName().equalsIgnoreCase(s))
                    maps.add(map);
            }
        }

        livesPerPlayer = config.getInt("lives per player", 1);
        playersNeededToStart = config.getInt("players needed to start", 2);
        allowRegularBuilding = config.getBoolean("allow regular building", false);
        pregameTime = config.getInt("timers.pregame", 45);
        invincibilityTime = config.getInt("timers.invincibility", 60);
        mainPhaseTime = config.getInt("timers.main", 600);
        borderTime = config.getInt("timers.border", 300);
        postGameTime = config.getInt("timers.postgame", 60);
        startType = StartType.valueOf(config.getString("start type", "elytra").toUpperCase());
    }

    public void saveConfig(String configName) {
        File configFile = new File(BGHR.getPlugin().getDataFolder().getAbsolutePath() + "/gameconfigs", configName + ".yml");

        if (!configFile.exists()) {
            if (configFile.getParentFile().mkdirs())
                Bukkit.getLogger().warning("creating directory: " + configFile.getAbsolutePath());
        }

        List<String> mapNames = new ArrayList<>();
        for (MapData map : maps) {
            mapNames.add(map.getMapName());
        }

        FileConfiguration config = new YamlConfiguration();
        config.set("maps", mapNames);
        config.set("lives per player", livesPerPlayer);
        config.set("players needed to start", playersNeededToStart);
        config.set("allow regular building", allowRegularBuilding);
        config.set("timers.pregame", pregameTime);
        config.set("timers.invincibility", invincibilityTime);
        config.set("timers.main", mainPhaseTime);
        config.set("timers.border", borderTime);
        config.set("timers.postgame", postGameTime);
        config.set("start type", startType.name().toLowerCase());

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<MapData> getMaps() {
        return new ArrayList<>(maps);
    }

    public void addMap(MapData map) {
        maps.add(map);
    }

    public File getConfigFile() {
        return configFile;
    }

    public void removeMap(MapData map) {
        maps.remove(map);
    }

    public void clearMaps() {
        maps.clear();
    }

    public StartType getStartType() {
        return startType;
    }

    public void toggleStartType() {
        if (startType == StartType.ELYTRA)
            startType = StartType.HUNGERGAMES;
        else
            startType = StartType.ELYTRA;
    }

    public int getLivesPerPlayer() {
        return livesPerPlayer;
    }

    public void setLivesPerPlayer(int livesPerPlayer) {
        this.livesPerPlayer = livesPerPlayer;
    }

    public int getPlayersNeededToStart() {
        return playersNeededToStart;
    }

    public void setPlayersNeededToStart(int playersNeededToStart) {
        this.playersNeededToStart = playersNeededToStart;
    }

    public int getPregameTime() {
        return pregameTime;
    }

    public void setPregameTime(int pregameTime) {
        this.pregameTime = pregameTime;
    }

    public int getInvincibilityTime() {
        return invincibilityTime;
    }

    public void setInvincibilityTime(int invincibilityTime) {
        this.invincibilityTime = invincibilityTime;
    }

    public int getMainPhaseTime() {
        return mainPhaseTime;
    }

    public void setMainPhaseTime(int mainPhaseTime) {
        this.mainPhaseTime = mainPhaseTime;
    }

    public int getBorderTime() {
        return borderTime;
    }

    public void setBorderTime(int borderTime) {
        this.borderTime = borderTime;
    }

    public int getPostGameTime() {
        return postGameTime;
    }

    public void setPostGameTime(int postGameTime) {
        this.postGameTime = postGameTime;
    }

    public boolean isAllowRegularBuilding() {
        return allowRegularBuilding;
    }

    public void setAllowRegularBuilding(boolean allowRegularBuilding) {
        this.allowRegularBuilding = allowRegularBuilding;
    }
}
