package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class GameDamageEvent extends Event implements Cancellable {
    private static final HandlerList handlerList = new HandlerList();
    private final Player aggressor;
    private final Player victim;

    private final EntityDamageEvent.DamageCause type;
    private final Game game;
    private String bestGuess;
    private boolean cancelled = false;
    private double damage;
    private final boolean directDamage;

    public GameDamageEvent(Player aggressor, Player victim, Game game, double damage, boolean directDamage, EntityDamageEvent.DamageCause type) {
        this.aggressor = aggressor;
        this.victim = victim;
        this.game = game;
        this.damage = damage;
        this.directDamage = directDamage;
        this.type = type;
        this.bestGuess = null;
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

    public EntityDamageEvent.DamageCause getType() {
        return type;
    }

    public boolean isDirectDamage() {
        return directDamage;
    }

    public String getBestGuess() {
        return bestGuess;
    }

    public void setBestGuess(String bestGuess) {
        this.bestGuess = bestGuess;
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
