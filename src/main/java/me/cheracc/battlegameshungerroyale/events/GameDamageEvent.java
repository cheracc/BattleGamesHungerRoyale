package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameDamageEvent extends Event implements Cancellable, GameEvent {
    private static final HandlerList handlerList = new HandlerList();
    private final Entity aggressor;
    private final Entity victim;
    private final EntityDamageEvent.DamageCause type;
    private final Game game;
    private final double damage;
    private boolean cancelled = false;

    public GameDamageEvent(Entity aggressor, Entity victim, Game game, double damage, EntityDamageEvent.DamageCause type) {
        this.aggressor = aggressor;
        this.victim = victim;
        this.game = game;
        this.damage = damage;
        this.type = type;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public @Nullable
    Entity getAggressor() {
        return aggressor;
    }

    public Entity getVictim() {
        return victim;
    }

    @Override
    public Game getGame() {
        return game;
    }

    public double getDamage() {
        return damage;
    }

    public EntityDamageEvent.DamageCause getType() {
        return type;
    }

    @Override
    public @NotNull
    HandlerList getHandlers() {
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
