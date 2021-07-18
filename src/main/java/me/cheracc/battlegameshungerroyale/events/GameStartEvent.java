package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameStartEvent extends Event implements GameEvent {
    Game game;
    private static final HandlerList handlerList = new HandlerList();

    public GameStartEvent(Game game) {
        this.game = game;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public Game getGame() {
        return game;
    }
}
