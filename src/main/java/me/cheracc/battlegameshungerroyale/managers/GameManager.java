package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.Game;
import me.cheracc.battlegameshungerroyale.datatypes.GameOptions;
import me.cheracc.battlegameshungerroyale.datatypes.MapData;
import org.apache.commons.io.FileUtils;
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

    public @Nullable Game getPlayersCurrentGame(Player player) {
        for (Game game : activeGames) {
            if (game.getWorld().getPlayers().contains(player))
                return game;
        }
        return null;
    }

    public void setupGame(Game game) {
        activeGames.add(game);
        game.setupGame();
    }

    public void gameOver(Game game) {
        activeGames.remove(game);
        if (activeGames.isEmpty())
            startWithRandomConfig();
    }

    private List<GameOptions> getAllConfigs() {
        File configDir = new File(BGHR.getPlugin().getDataFolder(), "gameconfigs/");
        List<GameOptions> configs = new ArrayList<>();

        if (!configDir.exists())
            configDir.mkdirs();

        if (configDir.listFiles().length == 0) {
            File baseConfig = new File(configDir, "default.yml");
            InputStream config = BGHR.getPlugin().getResource("gameconfig.yml");
            try {
                FileUtils.copyToFile(config, baseConfig);
            } catch (IOException e) {
                e.printStackTrace();
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
        int random = ThreadLocalRandom.current().nextInt(0, configs.size() - 1);

        startWithRandomMap(configs.get(random));
    }

    private void startWithRandomMap(GameOptions options) {
        List<MapData> maps = options.getMaps();
        Collections.shuffle(maps);
        int random;
        if (maps.size() > 1)
            random = ThreadLocalRandom.current().nextInt(0, maps.size() - 1);
        else
            random = 0;

        setupGame(new Game(maps.get(random), options));
    }
}
