package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameLoadedEvent extends Event implements GameEvent {
    private static final HandlerList handlerList = new HandlerList();
    Game game;

    public GameLoadedEvent(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
