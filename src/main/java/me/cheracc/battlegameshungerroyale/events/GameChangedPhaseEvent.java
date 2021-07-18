package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameChangedPhaseEvent extends Event implements GameEvent {
    public static final HandlerList handlerList = new HandlerList();
    Game game;
    String newPhase;

    public GameChangedPhaseEvent(Game game, String newPhase) {
        this.game = game;
        this.newPhase = newPhase;
    }

    @Override
    public Game getGame() {
        return game;
    }

    public String getPhase() {
        return newPhase;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
