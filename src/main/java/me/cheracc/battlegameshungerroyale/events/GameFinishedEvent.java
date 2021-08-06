package me.cheracc.battlegameshungerroyale.events;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameFinishedEvent extends Event implements GameEvent {
    private static final HandlerList handlerList = new HandlerList();
    Game game;
    Player winner;

    public GameFinishedEvent(Game game, Player winner) {
        this.game = game;
        this.winner = winner;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public Player getWinner() {
        return winner;
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public @NotNull
    HandlerList getHandlers() {
        return handlerList;
    }
}
