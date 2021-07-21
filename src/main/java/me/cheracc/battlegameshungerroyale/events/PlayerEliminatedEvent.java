package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerEliminatedEvent extends Event implements GameEvent {
    private final static HandlerList handlerList = new HandlerList();
    private final Game game;
    private final Player player;

    public PlayerEliminatedEvent(Player player, Game game) {
        this.game = game;
        this.player = player;
    }

    @Override
    public Game getGame() {
        return game;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
