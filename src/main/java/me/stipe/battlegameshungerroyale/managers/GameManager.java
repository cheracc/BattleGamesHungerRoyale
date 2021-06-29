package me.stipe.battlegameshungerroyale.managers;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Game;
import me.stipe.battlegameshungerroyale.datatypes.GameOptions;
import me.stipe.battlegameshungerroyale.datatypes.MapData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
            if (game.getActivePlayers().contains(player))
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
        int random = ThreadLocalRandom.current().nextInt(0, maps.size() - 1);

        setupGame(new Game(maps.get(random), options));
    }
}
