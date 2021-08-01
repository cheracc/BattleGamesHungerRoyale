package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import me.cheracc.battlegameshungerroyale.types.games.GameOptions;
import me.cheracc.battlegameshungerroyale.types.games.GameType;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.ScoreboardManager;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class GameManager {
    private final List<Game> activeGames = new ArrayList<>();
    private final List<GameType> loadedGameTypes = new ArrayList<>();
    private final MapDecider mapDecider;
    private final MapManager mapManager;
    private final BGHR plugin;
    private final Logr logr;
    private final Collection<LootTable> lootTables = new HashSet<>();

    public GameManager(BGHR plugin, Logr logr, MapManager mapManager) {
        this.plugin = plugin;
        this.logr = logr;
        this.mapManager = mapManager;
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        mapDecider = new MapDecider();
        getLootTables();
        getValidGameTypes();
        createNewGame(mapDecider.selectNextMap());
    }

    public boolean isThisAGameWorld(World world) {
        for (Game game : getActiveGames()) {
            if (game.getWorld().equals(world))
                return true;
        }
        return false;
    }

    public void createNewGameWithCallback(GameOptions options, Consumer<Game> callback) {
        String gameType = options.getGameType();
        try {
            Class<?> gameClass = Class.forName(gameType);
            Object o = gameClass.getConstructor(GameOptions.class).newInstance(options);

            if (o instanceof Game) {
                Game game = (Game) o;
                mapManager.createNewWorldAsync(options.getMap(), w -> {
                    game.initializeGame(w, plugin.getApi());
                    if (callback != null)
                        callback.accept(game);
                });
            }
        } catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            logr.warn("Unknown game type found in %s (%s)", options.getConfigFile().getName(), gameType);
            if (callback != null)
                callback.accept(null);
        }
    }

    public void createNewGame(GameOptions options) {
        createNewGameWithCallback(options, null);
    }

    public List<Game> getActiveGames() {
        return new ArrayList<>(activeGames);
    }

    public boolean isActivelyPlayingAGame(Player player) {
        if (isInAGame(player)) {
            Game game = getPlayersCurrentGame(player);
            return game.isPlaying(player) && !game.getPhase().toLowerCase().contains("game");
        }
        return false;
    }

    public boolean isInAGame(Player player) {
        return isThisAGameWorld(player.getWorld());
    }

    public @Nullable
    Game getPlayersCurrentGame(Player player) {
        for (Game game : activeGames) {
            if (game.isPlaying(player) || game.isSpectating(player))
                return game;
        }
        return null;
    }

    public void makeThisGameAvailable(Game game) {
        activeGames.add(game);
    }

    public void gameIsEnding() {
        if (activeGames.size() <= 1 && plugin.isEnabled())
            createNewGame(mapDecider.selectNextMap());
    }

    public void gameOver(Game game, Consumer<Game> callback) {
        activeGames.remove(game);
        mapDecider.setLastMap(game.getMap().getMapName());
        if (activeGames.isEmpty() && plugin.isEnabled())
            if (callback != null)
                createNewGameWithCallback(mapDecider.selectNextMap(), callback);
            else
                createNewGame(mapDecider.selectNextMap());
    }

    public void addVote(Player player, String mapName) {
        mapDecider.addVote(player, mapName);
    }

    public int getVotes(String mapName) {
        return mapDecider.getVotes(mapName);
    }

    public List<GameType> getValidGameTypes() {
        if (loadedGameTypes.isEmpty()) {
            Set<Class<?>> gameSubTypes = new HashSet<>((new Reflections("me.cheracc.battlegameshungerroyale.types.games", new SubTypesScanner(false))).getSubTypesOf(Game.class));

            for (Class<?> c : gameSubTypes) {
                if (c == null || Modifier.isAbstract(c.getModifiers()))
                    continue;
                Object o = null;
                try {
                    o = c.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
                if (o instanceof Game) {
                    Game game = (Game) o;
                    loadedGameTypes.add(new GameType(c.getName(), game.getGameIcon(), game.getGameTypeName(), game.getGameDescription()));
                    logr.debug("Added GameType %s with icon %s and description %s", game.getGameType(), game.getGameIcon().name(), game.getGameDescription());
                }
            }
            logr.debug("Loaded %s GameTypes", loadedGameTypes.size());
        }
        return new ArrayList<>(loadedGameTypes);
    }

    public GameType getGameType(String string) {
        for (GameType type : getValidGameTypes()) {
            if (type.getClassName().equals(string)) {
                return type;
            }
        }
        return null;
    }

    public void addGameType(GameType type) {
        loadedGameTypes.add(type);
        logr.debug("Added GameType: %s", type.getClassName());
    }

    public List<GameOptions> getAllConfigs() {
        File configDir = new File(plugin.getDataFolder(), "gameconfigs/");
        List<GameOptions> configs = new ArrayList<>();

        if (!configDir.exists())
            if (configDir.mkdirs())
                logr.info("Creating new directory for game config files...");

        if (configDir.listFiles().length == 0) {
            String[] defaultConfigNames = {"crystal_avalanche.yml", "horizon_city.yml", "island_tower.yml", "king_of_the_ring.yml"};

            boolean sent = false;
            for (String filename : defaultConfigNames) {
                File defaultConfig = new File(configDir, filename);
                try {
                    if (defaultConfig.createNewFile() && !sent) {
                        logr.info("Placing the default game config files...");
                        sent = true;
                    }
                    InputStream in = plugin.getResource("default_game_configs/" + filename);
                    FileUtils.copyToFile(in, defaultConfig);
                    in.close();
                } catch (IOException e) {
                    logr.warn("couldn't create file " + defaultConfig.getAbsolutePath());
                }
            }
        }

        for (File file : configDir.listFiles()) {
            if (file.exists() && file.getName().contains(".yml")) {
                GameOptions options = new GameOptions();
                options.loadConfig(file, mapManager, this);
                configs.add(options);
            }
        }
        return configs;
    }

    private void loadLootTables() {

        for (String s : mapManager.getLootTableNames()) {
            LootTable t = Bukkit.getLootTable(new NamespacedKey(plugin, s));
            if (t != null)
                lootTables.add(t);
        }

        if (!lootTables.isEmpty()) {
            StringBuilder names = new StringBuilder(" ");
            for (LootTable t : lootTables) {
                names.append(t.getKey().getKey());
                names.append(" ");
            }
            logr.info("Loaded custom loot tables: [%s]", names);
        } else {
            // default loot tables
            for (LootTables t : LootTables.values()) {
                if (t.getKey().getKey().contains("chest"))
                    lootTables.add(t.getLootTable());
            }
        }
    }

    public List<LootTable> getLootTables() {
        if (lootTables.isEmpty())
            loadLootTables();
        return new ArrayList<>(lootTables);
    }

    public LootTable getDefaultLootTable() {
        LootTable lt = Bukkit.getLootTable(new NamespacedKey(plugin, "default"));
        if (lt == null) {
            logr.warn("Could not find default loot table");
        }
        return lt;
    }

    public LootTable getLootTableFromKey(String key) {
        if (lootTables.isEmpty())
            loadLootTables();
        for (LootTable lt : lootTables) {
            if (lt.getKey().getKey().equalsIgnoreCase(key))
                return lt;
        }
        return null;
    }

    private class MapDecider {
        private final Map<UUID, String> outstandingVotes = new HashMap<>();
        private final BukkitTask voteCleaner;
        private String lastMap = null;

        public MapDecider() {
            this.voteCleaner = voteCleaner();
        }

        private BukkitTask voteCleaner() {
            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    outstandingVotes.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
                }
            };
            return task.runTaskTimer(plugin, 200L, 200L);
        }

        public int getVotes(String mapName) {
            return (int) outstandingVotes.values().stream().filter(s -> s.equals(mapName)).count();
        }

        public void addVote(Player player, String mapName) {
            if (outstandingVotes.containsKey(player.getUniqueId()))
                player.sendMessage(Trans.lateToComponent("Your vote has been changed to %s", mapName));
            else
                player.sendMessage(Trans.lateToComponent("Your vote has been cast for %s", mapName));
            outstandingVotes.put(player.getUniqueId(), mapName);
        }

        public GameOptions selectNextMap() {
            GameOptions winner = null;
            if (outstandingVotes.isEmpty()) {
                winner = selectRandomConfig();
            }

            Map<GameOptions, Integer> voteCounts = new HashMap<>();
            for (GameOptions opts : getAllConfigs()) {
                for (String s : outstandingVotes.values()) {
                    if (s.equals(opts.getMap().getMapName()) && !s.equals(lastMap)) {
                        Integer count = voteCounts.get(opts);
                        voteCounts.put(opts, count == null ? 1 : count + 1);
                    }
                }
            }

            for (Map.Entry<GameOptions, Integer> e : voteCounts.entrySet()) {
                if (winner == null)
                    winner = e.getKey();
                if (e.getValue() > voteCounts.get(e.getKey()))
                    winner = e.getKey();
            }

            if (winner == null)
                winner = getAllConfigs().get(0);

            String winnerMapName = winner.getMap().getMapName();
            outstandingVotes.values().removeIf(s -> s.equals(winnerMapName));
            return winner;
        }

        private GameOptions selectRandomConfig() {
            List<GameOptions> configs = getAllConfigs();

            if (mapDecider.getLastMap() != null && configs.size() > 2)
                configs.removeIf(opts -> opts.getMap().getMapName().equals(mapDecider.getLastMap()));

            Collections.shuffle(configs);
            int index = configs.size() > 1 ? ThreadLocalRandom.current().nextInt(0, configs.size() - 1) : 0;

            return configs.get(index);
        }

        public String getLastMap() {
            return lastMap;
        }

        public void setLastMap(String mapName) {
            this.lastMap = mapName;
        }
    }
}
