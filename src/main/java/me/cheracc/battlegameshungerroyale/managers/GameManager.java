package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.GameOptions;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private static GameManager singletonInstance = null;
    private final List<Game> activeGames = new ArrayList<>();

    private GameManager() {
        startWithRandomConfig();
    }

    public static GameManager getInstance() {
        if (singletonInstance == null)
            singletonInstance = new GameManager();

        return singletonInstance;
    }

    public List<Game> getActiveGames() {
        return new ArrayList<>(activeGames);
    }

    public boolean isInAGame(Player player) {
        return MapManager.getInstance().isThisAGameWorld(player.getWorld());
    }

    public @Nullable Game getPlayersCurrentGame(Player player) {
        for (Game game : activeGames) {
            if (game.isPlaying(player) || game.isSpectating(player))
                return game;
        }
        return null;
    }

    public void setupGame(Game game) {
        activeGames.add(game);
    }

    public void gameOver(Game game) {
        activeGames.remove(game);
        if (activeGames.isEmpty())
            startWithRandomConfig();
    }

    public List<GameOptions> getAllConfigs() {
        File configDir = new File(BGHR.getPlugin().getDataFolder(), "gameconfigs/");
        List<GameOptions> configs = new ArrayList<>();

        if (!configDir.exists())
            if (configDir.mkdirs())
                Bukkit.getLogger().info("creating gameconfigs directory");

        if (configDir.listFiles().length == 0) {
            String[] defaultConfigNames = { "crystal_avalanche.yml", "horizon_city.yml", "island_tower.yml", "king_of_the_ring.yml" };

            boolean sent = false;
            for (String filename : defaultConfigNames) {
                File defaultConfig = new File(configDir, filename);
                try {
                    if (defaultConfig.createNewFile() && !sent) {
                        Bukkit.getLogger().info("creating default game configs");
                        sent = true;
                    }
                    InputStream in = BGHR.getPlugin().getResource("default_game_configs/" + filename);
                    FileUtils.copyToFile(in, defaultConfig);
                    in.close();
                } catch (IOException e) {
                    Bukkit.getLogger().warning("couldn't create file " + defaultConfig.getAbsolutePath());
                }
            }

        }

        for (File file : configDir.listFiles()) {
            if (file.exists() && file.getName().contains(".yml")) {
                GameOptions options = new GameOptions();
                options.loadConfig(file);
                configs.add(options);
            }
        }
        return configs;
    }

    private void startWithRandomConfig() {
        List<GameOptions> configs = getAllConfigs();
        Collections.shuffle(configs);
        int index = configs.size() > 1 ? ThreadLocalRandom.current().nextInt(0, configs.size() - 1) : 0;

        GameOptions options = configs.get(index);
        Game.createNewGame(options.getMap(), options);
    }
}
