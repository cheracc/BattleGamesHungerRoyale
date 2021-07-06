package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.MapData;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public class MapManager implements Listener {
    private final BGHR plugin;
    private static MapManager singletonInstance;
    private final FileConfiguration mainConfig;
    private final File mapsDirectory;
    private final File mainWorldFolder;
    private final File activeMapsDirectory;
    private final Map<MapData, List<World>> maps = new HashMap<>();
    private final Set<String> lootTableNames = new HashSet<>();
    private World lobbyWorld;
    private boolean canFlyInLobby = false;
    private boolean updatedDatapack = false;

    private MapManager() {
        plugin = BGHR.getPlugin();
        mainConfig = plugin.getConfig();
        mapsDirectory = new File(plugin.getDataFolder().getParentFile().getParent(), mainConfig.getString("maps directory", "BGHR_Maps/")).getAbsoluteFile();
        mainWorldFolder = getMainWorldFolder();
        activeMapsDirectory = new File(plugin.getDataFolder().getParentFile().getParent(), mainConfig.getString("loaded maps directory", "loaded_maps/")).getAbsoluteFile();

        if (!mapsDirectory.exists()) {
            Tools.extractZipResource(plugin.getClass(), "/BGHR_Maps.zip", mapsDirectory.getParentFile().toPath());
        }

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
        String relativePath = mainConfig.getString("lobby world to copy", "BGHR_Maps/islandtower");
        File lobby = new File(plugin.getDataFolder().getParentFile().getParent(), relativePath);
        boolean resetLobby = mainConfig.getBoolean("reset lobby on restart", false);

        if (resetLobby && lobby.exists() && lobby.isDirectory()) {
            copyMap(lobby, mainWorldFolder);
        }

        installDataPack();

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

                if (isLobby) {
                    for (MapData m : getMaps()) {
                        if (m.isLobby()) {
                            Bukkit.getLogger().warning("found multiple lobby.yml files in the maps directory. There can only be one lobby map.");
                            Bukkit.getLogger().warning("Existing: " + m.getMapDirectory().getPath() + " Tried to add: " + configFile.getParent());
                            Bukkit.getLogger().warning(configFile.getParent() + " was not loaded. Rename one of the lobby.yml files to mapconfig.yml. You may only have one lobby.yml file.");
                            return;
                        }
                    }
                }

                MapData mapData = new MapData(config, file, isLobby);
                maps.put(mapData, new ArrayList<>());
            }
        }
    }

    private void loadLobbyConfig(FileConfiguration config) {
        this.canFlyInLobby = config.getBoolean("players can fly", false);
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

    public boolean wasDatapackUpdated() {
        return updatedDatapack;
    }

    public Set<String> getLootTableNames() {
        return new HashSet<>(lootTableNames);
    }

    public World getLobbyWorld() {
        if (lobbyWorld == null)
            lobbyWorld = Bukkit.getWorlds().get(0);
        return lobbyWorld;
    }

    public MapData getLobbyMap() {
        for (MapData m : getMaps()) {
            if (m.isLobby())
                return m;
        }
        Bukkit.getLogger().warning("could not find lobby mapdata");
        return null;
    }

    public List<MapData> getMaps() {
        return new ArrayList<>(maps.keySet());
    }

    public MapData getMapByMapDirectoryName(String folderName) {
        for (MapData map : getMaps()) {
            if (map.getMapDirectory().getName().equalsIgnoreCase(folderName))
                return map;
        }
        return null;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (event.getWorld().getName().equalsIgnoreCase(mainWorldFolder.getName())) {
            this.lobbyWorld = event.getWorld();
            maps.get(getLobbyMap()).add(lobbyWorld);
            lobbyWorld.setAutoSave(false);
            lobbyWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            lobbyWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
            lobbyWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            lobbyWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
            lobbyWorld.setGameRule(GameRule.SPAWN_RADIUS, 0);
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

    public MapData getMapFromWorld(World world) {
        for (Map.Entry<MapData, List<World>> e : maps.entrySet())
            if (e.getValue().contains(world))
                return e.getKey();
        return null;
    }

    public void unloadWorld(World world) {
        MapData map = getMapFromWorld(world);
        Bukkit.unloadWorld(world, false);

        maps.get(map).remove(world);
    }

    public interface LoadedWorld {
        void loadedWorld(World world);
    }

    public void createNewWorldAsync(MapData mapData, LoadedWorld callback) {
        String uid = UUID.randomUUID().toString().split("-")[0];
        File loadedMap = new File(activeMapsDirectory, mapData.getMapDirectory().getName() + "_" + uid);
        asyncCopyMap(mapData.getMapDirectory(), loadedMap, () -> {
            World world = Bukkit.createWorld(new WorldCreator(activeMapsDirectory.getName() + "/" + loadedMap.getName()));
            if (world == null) {
                Bukkit.getLogger().warning("could not load world " + activeMapsDirectory.getName());
                return;
            }
            world.setAutoSave(false);
            world.setKeepSpawnInMemory(false);
            world.setClearWeatherDuration(Integer.MAX_VALUE);
            world.setTime(0);
            if (mapData.isUseBorder() && mapData.getBorderRadius() > 0) {
                world.getWorldBorder().setCenter(mapData.getBorderCenter(world));
                world.getWorldBorder().setSize(mapData.getBorderRadius() * 2);
            }
            maps.get(mapData).add(world);

            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.loadedWorld(world);
                }
            }.runTask(plugin);

        });
    }

    private interface ImFinished {
        void done();
    }

    private void asyncCopyMap(File mapSourceDirectory, File destination, ImFinished callback) {
        List<String> filesToIgnore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));

        if (destination.exists()) {
            try {
                FileUtils.deleteDirectory(destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (destination.mkdirs())
            Bukkit.getLogger().info("copying " + mapSourceDirectory.getAbsolutePath() + " to " + destination.getAbsolutePath());

        if (mapSourceDirectory.listFiles() == null)
            return;


        new BukkitRunnable() {
            @Override
            public void run() {
                List<File> mapFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(mapSourceDirectory.listFiles())));

                try {
                    for (File file : mapFiles) {
                        if (filesToIgnore.contains(file.getName()))
                            continue;
                        if (file.isDirectory())
                            FileUtils.copyDirectoryToDirectory(file, destination);
                        else
                            FileUtils.copyFileToDirectory(file, destination);
                    }

                    if (destination.equals(mainWorldFolder)) {
                        File playerdataDirectory = new File(destination, "playerdata");
                        if (!playerdataDirectory.exists())
                            if (!playerdataDirectory.mkdirs())
                                Bukkit.getLogger().warning("could not create empty playerdata directory");
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.done();
                        }
                    }.runTask(plugin);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void installDataPack() {
        File datapackDirectory = new File(mainWorldFolder, "datapacks/bghr_datapack");
        if (!datapackDirectory.exists())
            if (datapackDirectory.mkdirs())
                updatedDatapack = true;

        File datapackLootTablesDirectory = new File(datapackDirectory, "data/battlegameshungerroyale/loot_tables");
        if (!datapackLootTablesDirectory.exists())
            if (datapackLootTablesDirectory.mkdirs())
                updatedDatapack = true;

        File mcmeta = new File(datapackDirectory, "pack.mcmeta");
        if (!mcmeta.exists()) {
            try {
                mcmeta.createNewFile();
                OutputStream out = new FileOutputStream(mcmeta);
                plugin.getResource("datapack_files/pack.mcmeta").transferTo(out);
                out.close();
                updatedDatapack = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File pluginLootTablesDir = new File(plugin.getDataFolder(), "loot_tables");
        if (!pluginLootTablesDir.exists())
            if (pluginLootTablesDir.mkdirs())
                Bukkit.getLogger().info("creating directory " + pluginLootTablesDir.getAbsolutePath());

        File defaultLootTable = new File(pluginLootTablesDir, "default.json");
        if (!defaultLootTable.exists()) {
            try {
                defaultLootTable.createNewFile();
                OutputStream out = new FileOutputStream(defaultLootTable);
                plugin.getResource("datapack_files/default.json").transferTo(out);
                out.close();
                Bukkit.getLogger().info("creating file " + defaultLootTable.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (File file : pluginLootTablesDir.listFiles()) {
            lootTableNames.add(file.getName().split("\\.")[0]);
            try {
                File datapackFile = new File(datapackLootTablesDirectory, file.getName());
                if (!datapackFile.exists()) {
                    FileUtils.copyFileToDirectory(file, datapackLootTablesDirectory);
                    updatedDatapack = true;
                }
                if (FileUtils.contentEquals(file, datapackFile)) {
                    FileUtils.deleteQuietly(datapackFile);
                    FileUtils.copyFileToDirectory(file, datapackLootTablesDirectory);
                    updatedDatapack = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

            if (destination.equals(mainWorldFolder)) {
                File playerdataDirectory = new File(destination, "playerdata");
                if (!playerdataDirectory.exists())
                    if (!playerdataDirectory.mkdirs())
                        Bukkit.getLogger().warning("could not create empty playerdata directory");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveMap(MapData mapData, World world) {
        File oldVersionsDirectory = new File(mapsDirectory, "old_maps");
        if (!oldVersionsDirectory.exists() && oldVersionsDirectory.mkdirs())
            Bukkit.getLogger().info("created old_maps directory inside maps directory");
        String[] unwantedFiles = { "uid.dat", "session.lock", "level.dat_old", "playerdata", "advancements", "stats" };

        world.save();

        File loadedMap = world.getWorldFolder();

        if (loadedMap.exists()) {
            try {
                File archiveFolder = new File(oldVersionsDirectory, mapData.getMapDirectory().getName() + "_" + Tools.getTimestamp());
                FileUtils.moveDirectory(mapData.getMapDirectory(), archiveFolder);
                Bukkit.getLogger().info("archived " + mapData.getMapDirectory().getPath() + " as " + archiveFolder.getPath());
                FileUtils.deleteDirectory(mapData.getMapDirectory());
                Bukkit.getLogger().info("deleted " + mapData.getMapDirectory().getPath());
                FileUtils.copyDirectory(loadedMap, mapData.getMapDirectory());
                Bukkit.getLogger().info("copied " + loadedMap.getAbsolutePath() + " to " + mapData.getMapDirectory().getPath());
                for (String s : unwantedFiles) {
                    File delete = new File(mapData.getMapDirectory(), s);
                    FileUtils.deleteQuietly(delete);
                }
                mapData.saveConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
            Bukkit.getLogger().warning("Couldn't load the world folder for loaded world " + world.getName() + " map " + mapData.getMapName());

    }



}
