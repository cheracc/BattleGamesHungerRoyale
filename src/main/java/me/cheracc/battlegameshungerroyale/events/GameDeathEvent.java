package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.datatypes.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class GameDeathEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    private final Game game;
    private final Player recentlyDeceased;
    private final Player killer;
    private final double killingBlowDamage;
    private final EntityDamageEvent.DamageCause killingBlowCause;

    public GameDeathEvent(Player dead, Player killer, Game game, double damage, EntityDamageEvent.DamageCause type) {
        this.recentlyDeceased = dead;
        this.killer = killer;
        this.killingBlowDamage = damage;
        this.killingBlowCause = type;
        this.game = game;

    }

    public Player getRecentlyDeceased() {
        return recentlyDeceased;
    }

    public Player getKiller() {
        return killer;
    }

    public double getKillingBlowDamage() {
        return killingBlowDamage;
    }

    public EntityDamageEvent.DamageCause getKillingBlowCause() {
        return killingBlowCause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
