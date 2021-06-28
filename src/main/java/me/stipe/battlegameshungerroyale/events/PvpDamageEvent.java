package me.stipe.battlegameshungerroyale.events;

import me.stipe.battlegameshungerroyale.datatypes.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PvpDamageEvent extends Event implements Cancellable {
    private static final HandlerList handlerList = new HandlerList();
    private final Player aggressor;
    private final Player victim;
    private final Game game;
    private boolean cancelled = false;
    private double damage;
    private final boolean directDamage;

    public PvpDamageEvent(Player aggressor, Player victim, Game game, double damage, boolean directDamage) {
        this.aggressor = aggressor;
        this.victim = victim;
        this.game = game;
        this.damage = damage;
        this.directDamage = directDamage;
    }

    public Player getAggressor() {
        return aggressor;
    }

    public Player getVictim() {
        return victim;
    }

    public Game getGame() {
        return game;
    }

    public double getDamage() {
        return damage;
    }

    public boolean isDirectDamage() {
        return directDamage;
    }


    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
