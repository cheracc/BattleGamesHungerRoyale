package me.stipe.battlegameshungerroyale.managers;

import me.stipe.battlegameshungerroyale.datatypes.Game;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GameManager {
    private static GameManager singletonInstance = null;
    private final List<Game> activeGames = new ArrayList<>();

    private GameManager() {}

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
    }
}
