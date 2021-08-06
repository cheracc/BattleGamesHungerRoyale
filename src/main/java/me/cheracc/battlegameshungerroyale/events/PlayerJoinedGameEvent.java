package me.cheracc.battlegameshungerroyale.events;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerJoinedGameEvent extends Event implements GameEvent {
    private static final HandlerList handlerList = new HandlerList();
    private final Game game;
    private final Player player;
    private final boolean asSpectator;

    public PlayerJoinedGameEvent(Player player, Game game, boolean asSpectator) {
        this.player = player;
        this.game = game;
        this.asSpectator = asSpectator;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public boolean isJoiningAsSpectator() {
        return asSpectator;
    }

    public Player getPlayer() {
        return player;
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
