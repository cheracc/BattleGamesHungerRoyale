package me.cheracc.battlegameshungerroyale.types.games;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.types.MapData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.loot.LootTable;

import java.io.File;
import java.io.IOException;

public class GameOptions {
    private File configFile;
    private String gameType;
    private MapData map;
    private int livesPerPlayer;
    private int playersNeededToStart;
    private int pregameTime;
    private int invincibilityTime;
    private int mainPhaseTime;
    private int borderTime;
    private int postGameTime;
    private boolean allowRegularBuilding;
    private boolean generateChests;
    private boolean fillAllChests;
    private boolean loosenSearchRestrictions;
    private int maxChestsPerChunk;
    private int chestRespawnTime;
    private LootTable lootTable;

    public void loadConfig(File file, MapManager mapManager, GameManager gameManager) {
        FileConfiguration config = new YamlConfiguration();

        if (file != null) {

            try {
                config.load(file);
                configFile = file;
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }

        String mapName = config.getString("map");
        MapData map = mapManager.getMapByMapDirectoryName(mapName);
        if (mapName == null || map == null) {
            this.map = mapManager.getMaps().get(0);
        } else
            this.map = map;

        gameType = config.getString("game type", "me.cheracc.battlegameshungerroyale.types.games.FreeForAll");
        livesPerPlayer = config.getInt("lives per player", 1);
        playersNeededToStart = config.getInt("players needed to start", 2);
        allowRegularBuilding = config.getBoolean("allow regular building", false);
        pregameTime = config.getInt("timers.pregame", 45);
        invincibilityTime = config.getInt("timers.invincibility", 60);
        mainPhaseTime = config.getInt("timers.main", 600);
        borderTime = config.getInt("timers.border", 300);
        postGameTime = config.getInt("timers.postgame", 60);
        generateChests = config.getBoolean("loot.generate chests", true);
        loosenSearchRestrictions = config.getBoolean("loot.loosen search restrictions", true);
        maxChestsPerChunk = config.getInt("loot.max chests per chunk", 10);
        lootTable = gameManager.getLootTableFromKey(config.getString("loot.loot table", "default"));
        if (lootTable == null)
            lootTable = gameManager.getDefaultLootTable();
        chestRespawnTime = config.getInt("loot.chest respawn time", 45);
        fillAllChests = config.getBoolean("loot.fill all chests", true);
    }

    public void saveConfig(String configName, BGHR plugin) {
        String filename = configName;
        if (!configName.contains(".yml"))
            filename = configName + ".yml";

        File configFile = new File(plugin.getDataFolder().getAbsolutePath() + "/gameconfigs", filename);

        if (!configFile.exists()) {
            if (configFile.getParentFile().mkdirs())
                plugin.getLogr().warn("creating directory: " + configFile.getAbsolutePath());
        }

        FileConfiguration config = new YamlConfiguration();
        config.set("map", map.getMapDirectory().getName());
        config.set("game type", gameType);
        config.set("lives per player", livesPerPlayer);
        config.set("players needed to start", playersNeededToStart);
        config.set("allow regular building", allowRegularBuilding);
        config.set("timers.pregame", pregameTime);
        config.set("timers.invincibility", invincibilityTime);
        config.set("timers.main", mainPhaseTime);
        config.set("timers.border", borderTime);
        config.set("timers.postgame", postGameTime);
        config.set("loot.generate chests", generateChests);
        config.set("loot.loosen search restrictions", loosenSearchRestrictions);
        config.set("loot.max chests per chunk", maxChestsPerChunk);
        config.set("loot.loot table", lootTable.getKey().getKey());
        config.set("loot.chest respawn time", chestRespawnTime);
        config.set("loot.fill all chests", fillAllChests);

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType.getClassName();
    }

    public MapData getMap() {
        return map;
    }

    public void setMap(MapData map) {
        this.map = map;
    }

    public File getConfigFile() {
        return configFile;
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

    public boolean isGenerateChests() {
        return generateChests;
    }

    public void toggleGenerateChests() {
        generateChests = !generateChests;
    }

    public boolean isFillAllChests() {
        return fillAllChests;
    }

    public void toggleFillAllChests() {
        fillAllChests = !fillAllChests;
    }

    public boolean isLoosenSearchRestrictions() {
        return loosenSearchRestrictions;
    }

    public void toggleLoosenSearchRestrictions() {
        loosenSearchRestrictions = !loosenSearchRestrictions;
    }

    public int getMaxChestsPerChunk() {
        return maxChestsPerChunk;
    }

    public void setMaxChestsPerChunk(int value) {
        maxChestsPerChunk = value;
    }

    public int getChestRespawnTime() {
        return chestRespawnTime;
    }

    public void setChestRespawnTime(int value) {
        chestRespawnTime = value;
    }

    public LootTable getLootTable() {
        return lootTable;
    }

    public void setLootTable(LootTable table) {
        this.lootTable = table;
    }
}
