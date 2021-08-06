package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.types.DamageSource;
import me.cheracc.battlegameshungerroyale.types.Metadata;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.UUID;

public class CustomEventsListener implements Listener {
    public final GameManager gameManager;

    public CustomEventsListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void callGameDeathEvent(GameDamageEvent event) {
        if (event.getVictim() instanceof Player) {
            Player victim = (Player) event.getVictim();
            if (event.getDamage() >= victim.getHealth()) {
                GameDeathEvent deathEvent = new GameDeathEvent(victim, event.getAggressor(), event.getGame(), event.getDamage(), event.getType());
                deathEvent.callEvent();
            }
        }
    }

    @EventHandler
    public void callPlayerEliminatedEvent(GameDeathEvent event) {
        if (event.getGame().getLivesLeft(event.getRecentlyDeceased().getUniqueId()) <= 1)
            new PlayerEliminatedEvent(event.getRecentlyDeceased(), event.getGame()).callEvent();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void callGameDamageEvent(EntityDamageEvent event) {
        LivingEntity aggressor = null;
        LivingEntity victim;
        Game game;

        // make sure this is happening inside a game
        if (!gameManager.isThisAGameWorld(event.getEntity().getWorld()))
            return;

        // get the victim
        if (event.getEntity() instanceof LivingEntity)
            victim = (LivingEntity) event.getEntity();
        else
            return;

        // get the aggressor if there is one
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
            Entity damager = e.getDamager();

            if (damager instanceof LivingEntity)
                aggressor = (LivingEntity) e.getDamager();
            else if (damager instanceof Projectile) {
                if (((Projectile) damager).getShooter() instanceof LivingEntity)
                    aggressor = (LivingEntity) ((Projectile) damager).getShooter();
            } else if (damager instanceof AreaEffectCloud) {
                if (((AreaEffectCloud) damager).getSource() instanceof LivingEntity)
                    aggressor = (LivingEntity) ((AreaEffectCloud) damager).getSource();
            }
        } else if (victim instanceof Player) {
            DamageSource ds = DamageSource.getFrom((Player) victim);
            if (ds != null && ds.isApplicable(null, event.getCause()))
                aggressor = ds.getSource();
        } else if (event instanceof EntityDamageByBlockEvent) {
            Block block = ((EntityDamageByBlockEvent) event).getDamager();
            if (block != null) {
                if (block.hasMetadata(Metadata.PLACED_BY_PLAYER.key())) {
                    for (MetadataValue v : block.getMetadata(Metadata.PLACED_BY_PLAYER.key())) {
                        Object o = v.value();
                        if (o instanceof UUID) {
                            aggressor = Bukkit.getPlayer((UUID) o);
                        }
                    }
                }
            }
        }

        // dont bother if neither is a player
        if (!(victim instanceof Player) && !(aggressor instanceof Player))
            return;

        // call the event
        if (event.getDamage() > 0) {
            game = gameManager.getGameFromWorld(event.getEntity().getWorld());

            if (game == null) // not playing a game
                return;

            GameDamageEvent gameDamageEvent = new GameDamageEvent(aggressor, victim, game, event.getFinalDamage(), event.getCause());
            gameDamageEvent.callEvent();

            if (gameDamageEvent.isCancelled())
                event.setCancelled(true);

            event.setDamage(gameDamageEvent.getDamage());
        }
    }
}
