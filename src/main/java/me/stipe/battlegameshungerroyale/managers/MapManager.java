package me.stipe.battlegameshungerroyale.managers;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.MapData;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MapManager implements Listener {
    private final BGHR plugin;
    private static MapManager singletonInstance;
    private final FileConfiguration mainConfig;
    private final File mapsDirectory;
    private final File mainWorldFolder;
    private final File activeMapsDirectory;
    private final List<MapData> maps = new ArrayList<>();
    private World lobbyWorld = null;
    private boolean canBuildInLobby = false;
    private boolean canFlyInLobby = false;

    private MapManager() {
        plugin = BGHR.getPlugin();
        mainConfig = plugin.getMainConfig();
        mapsDirectory = new File(plugin.getDataFolder().getParentFile().getParent(), mainConfig.getString("maps directory", "maps/")).getAbsoluteFile();
        mainWorldFolder = getMainWorldFolder();
        activeMapsDirectory = new File(plugin.getDataFolder().getParentFile().getParent(), mainConfig.getString("loaded maps directory", "loaded_maps/")).getAbsoluteFile();

        deleteCompletedMaps();
        loadLobby();
        registerMaps();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static MapManager getInstance() {
        if (singletonInstance == null)
            singletonInstance = new MapManager();
        return singletonInstance;
    }

    private void loadLobby() {
        String relativePath = mainConfig.getString("lobby world", "maps/lobby");
        File lobby = new File(plugin.getDataFolder().getParentFile().getParent(), relativePath);
        boolean resetLobby = mainConfig.getBoolean("reset lobby on restart", false);

        if (resetLobby && lobby.exists() && lobby.isDirectory()) {
            copyMap(lobby, mainWorldFolder);
        }

        File lobbyConfig = new File(lobby, "lobby.yml");
        if (lobbyConfig.exists()) {
            try {
                FileConfiguration config = new YamlConfiguration();
                config.load(lobbyConfig);
                loadLobbyConfig(config);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerMaps() {
        for (File file : Objects.requireNonNull(mapsDirectory.listFiles())) {
            boolean isLobby = false;
            if (file.isDirectory()) {
                File configFile = new File(file, "mapconfig.yml");
                if (!configFile.exists()) {
                    configFile = new File(file, "lobby.yml");
                    if (!configFile.exists())
                        continue;
                    else
                        isLobby = true;
                }

                FileConfiguration config = new YamlConfiguration();
                try {
                    config.load(configFile);
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                }

                MapData mapData = new MapData(config, file, isLobby);
                if (isLobby && lobbyWorld != null)
                    mapData.setWorld(lobbyWorld);
                maps.add(mapData);
            }
        }
    }

    private void loadLobbyConfig(FileConfiguration config) {
        this.canBuildInLobby = config.getBoolean("players can build", false);
        this.canFlyInLobby = config.getBoolean("players can fly", false);

    }

    public MapData getPlayersCurrentMap(Player p) {
        for (MapData mapData : maps) {
            if (mapData.isLoaded() && mapData.getWorld().equals(p.getWorld()))
                return mapData;
        }
        Bukkit.getLogger().warning("Couldn't find the map data for " + p.getName() + "'s current world");
        return null;
    }

    private Properties getServerProperties() {
        Properties properties = new Properties();
        try {
            BufferedReader is = new BufferedReader(new FileReader("server.properties"));

            properties.load(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return properties;
        }
        return properties;
    }

    private File getMainWorldFolder() {
        String worldName = getServerProperties().getProperty("level-name", "world");
        return new File(plugin.getDataFolder().getParentFile().getParentFile(), worldName);
    }

    public World getLobbyWorld() {
        if (lobbyWorld != null)
            return lobbyWorld;
        else
            return Bukkit.getWorlds().get(0);
    }

    public List<MapData> getMaps() {
        return new ArrayList<>(maps);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (event.getWorld().getName().equalsIgnoreCase(mainWorldFolder.getName())) {
            this.lobbyWorld = event.getWorld();
            for (MapData mapData : maps) {
                if (mapData.isLobby())
                    mapData.setWorld(lobbyWorld);
            }
            lobbyWorld.setAutoSave(false);
            WorldLoadEvent.getHandlerList().unregister(this);
        }
    }

    private void deleteCompletedMaps() {
        if (activeMapsDirectory.exists())
            try {
                FileUtils.deleteDirectory(activeMapsDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void loadMap(MapData mapData) {
        File loadedMap = new File(activeMapsDirectory, mapData.getMapDirectory().getName() + "_" + System.currentTimeMillis());
        copyMap(mapData.getMapDirectory(), loadedMap);
        World world = Bukkit.createWorld(new WorldCreator(mapsDirectory.getName() + "/" + mapData.getMapDirectory().getName()));
        if (world == null) return;
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setClearWeatherDuration(Integer.MAX_VALUE);
        world.setTime(0);
        if (mapData.isUseBorder() && !(mapData.getCenterX() != 0 && mapData.getCenterZ() != 0)) {
            world.getWorldBorder().setCenter(mapData.getCenterX(), mapData.getCenterZ());
            world.getWorldBorder().setSize(mapData.getBorderRadius() * 2);
        }
        mapData.setWorld(world);
    }

    public void unloadMap(MapData mapData) {
        // TODO make sure a game isn't going on
        World world = mapData.getWorld();

        for (Player p : world.getPlayers())
            p.teleport(getLobbyWorld().getSpawnLocation());

        Bukkit.unloadWorld(world, false);
        mapData.setWorld(null);
    }

    private void copyMap(File mapSourceDirectory, File destination) {
        try {
            List<String> filesToIgnore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));

            if (destination.exists()) {
                FileUtils.deleteDirectory(destination);
            }
            if (destination.mkdirs())
                Bukkit.getLogger().info("copying " + mapSourceDirectory.getAbsolutePath() + " to " + destination.getAbsolutePath());

            if (mapSourceDirectory.listFiles() == null)
                return;


            List<File> mapFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(mapSourceDirectory.listFiles())));

            for (File file : mapFiles) {
                if (filesToIgnore.contains(file.getName()))
                    continue;
                if (file.isDirectory())
                    FileUtils.copyDirectoryToDirectory(file, destination);
                else
                    FileUtils.copyFileToDirectory(file, destination);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unloadAllMaps() {
        for (MapData mapData : maps) {
            if (mapData.isLoaded() && !mapData.isLobby()) {
                mapData.getWorld().setAutoSave(false);
                mapData.getWorld().setKeepSpawnInMemory(false);
                Bukkit.unloadWorld(mapData.getWorld(), false);
                Bukkit.getLogger().info("unloaded " + mapData.getMapName());
            }
        }
    }

    public void saveMap(MapData mapData) {
        File oldVersionsDirectory = new File(mapsDirectory, "old_maps");
        if (!oldVersionsDirectory.exists() && oldVersionsDirectory.mkdirs())
            Bukkit.getLogger().info("created old_maps directory inside maps directory");

        if (mapData.getWorld() != null) {
            mapData.getWorld().save();
        }

        File loadedMap = new File(activeMapsDirectory, mapData.getMapDirectory().getName());

        if (loadedMap.exists()) {
            try {
                FileUtils.moveDirectory(mapData.getMapDirectory(), new File(oldVersionsDirectory, mapData.getMapDirectory().getName() + System.currentTimeMillis()));
                FileUtils.copyDirectoryToDirectory(loadedMap, mapsDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public boolean canBuildInLobby() {
        return canBuildInLobby;
    }


}
