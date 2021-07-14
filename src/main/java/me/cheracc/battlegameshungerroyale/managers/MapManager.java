package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.MapData;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;

import java.io.*;
import java.util.*;

// singleton class to handle maps and loaded worlds for the plugin
public class MapManager implements Listener {
    // singleton pattern
    private static MapManager singletonInstance;

    private MapManager() {
        plugin = BGHR.getPlugin();
        mainConfig = plugin.getConfig();
        mapsDirectory = new File(plugin.getDataFolder().getParentFile().getParent(), mainConfig.getString("maps directory", "BGHR_Maps/")).getAbsoluteFile();
        mainWorldFolder = getMainWorldFolder();
        activeMapsDirectory = new File(plugin.getDataFolder().getParentFile().getParent(), mainConfig.getString("loaded maps directory", "loaded_maps/")).getAbsoluteFile();

        if (!mapsDirectory.exists()) {
            if (mapsDirectory.mkdirs())
                Bukkit.getLogger().info("Unpacking sample maps into " + mapsDirectory.getPath());
            Tools.extractZipResource(plugin.getClass(), "/BGHR_Maps.zip", mapsDirectory.toPath());
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

    // private fields
    private final BGHR plugin;
    private final FileConfiguration mainConfig;
    private final File mapsDirectory;
    private final File mainWorldFolder;
    private final File activeMapsDirectory;
    private final Map<MapData, List<UUID>> maps = new HashMap<>();
    private final Set<String> lootTableNames = new HashSet<>();
    private World lobbyWorld;
    private boolean updatedDatapack = false;

    // public methods
    public boolean wasDatapackUpdated() {
        return updatedDatapack;
    }

    public boolean isThisAGameWorld(World world) {
        Set<UUID> worldIds = new HashSet<>();
        for (Collection<UUID> ids : maps.values())
            worldIds.addAll(ids);

        return worldIds.contains(world.getUID());
    }

    public Set<String> getLootTableNames() {
        return new HashSet<>(lootTableNames);
    }

    public World getLobbyWorld() {
        if (lobbyWorld == null)
            lobbyWorld = Bukkit.getWorlds().get(0);
        return lobbyWorld;
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

    public MapData getMapFromWorld(World world) {
        for (Map.Entry<MapData, List<UUID>> e : maps.entrySet())
            if (e.getValue().contains(world.getUID()))
                return e.getKey();
        return null;
    }

    public void unloadWorld(World world) {
        MapData map = getMapFromWorld(world);
        Bukkit.unloadWorld(world, false);

        maps.get(map).remove(world.getUID());
    }

    public void createNewWorldAsync(MapData mapData, Consumer<World> callback) {
        String uid = UUID.randomUUID().toString().split("-")[0];
        File loadedMap = new File(activeMapsDirectory, mapData.getMapDirectory().getName() + "_" + uid);
        asyncCopyMap(mapData.getMapDirectory(), loadedMap, success -> {
            if (!success)
                return;
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
            maps.get(mapData).add(world.getUID());

            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.accept(world);
                }
            }.runTask(plugin);

        });
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

    // private methods
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

    private void loadLobby() {
        String relativePath = mainConfig.getString("lobby world to copy", "BGHR_Maps/islandtower");
        File lobby = new File(plugin.getDataFolder().getParentFile().getParent(), relativePath);
        boolean resetLobby = mainConfig.getBoolean("reset lobby on restart", false);

        if (resetLobby && lobby.exists() && lobby.isDirectory()) {
            copyMap(lobby, mainWorldFolder);
        }

        installDataPack();

    }

    private void registerMaps() {
        for (File file : Objects.requireNonNull(mapsDirectory.listFiles())) {
            FileConfiguration config = new YamlConfiguration();
            if (file.isDirectory() && !file.getAbsolutePath().contains("old_maps")) {
                File configFile = new File(file, "mapconfig.yml");
                boolean saveNewConfig = true;

                if (configFile.exists()) {
                    try {
                        config.load(configFile);
                        saveNewConfig = false;
                    } catch (IOException | InvalidConfigurationException e) {
                        Bukkit.getLogger().warning("could not load config file " + configFile.getAbsolutePath());
                    }
                }
                MapData mapData = MapData.createFromConfig(config, file);
                maps.put(mapData, new ArrayList<>());
                if (saveNewConfig)
                    mapData.saveConfig();
            }
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

    private void asyncCopyMap(File mapSourceDirectory, File destination, Consumer<Boolean> callback) {
        List<String> filesToIgnore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));

        if (destination.exists()) {
            try {
                FileUtils.deleteDirectory(destination);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (destination.mkdirs())
            Bukkit.getLogger().info("copying " + mapSourceDirectory.getName() + " to active maps directory");

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
                            callback.accept(true);
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
                if (mcmeta.createNewFile())
                    Bukkit.getLogger().info("Installing plugin datapack files");
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
                if (defaultLootTable.createNewFile())
                    Bukkit.getLogger().info("Inserting default loot table");
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
                if (!FileUtils.contentEquals(file, datapackFile)) {
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
                Bukkit.getLogger().info("resetting main (lobby) world from " + mapSourceDirectory.getPath());

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
}
