package me.cheracc.battlegameshungerroyale.events;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerQuitGameEvent extends Event implements GameEvent {
    private final static HandlerList handlerList = new HandlerList();
    private final Game game;
    private final Player player;
    private final int livesRemaining;

    public PlayerQuitGameEvent(Player player, Game game, int livesRemaining) {
        this.game = game;
        this.player = player;
        this.livesRemaining = livesRemaining;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public Game getGame() {
        return game;
    }

    public Player getPlayer() {
        return player;
    }

    public int getLivesRemaining() {
        return livesRemaining;
    }

    @Override
    public @NotNull
    HandlerList getHandlers() {
        return handlerList;
    }
}
