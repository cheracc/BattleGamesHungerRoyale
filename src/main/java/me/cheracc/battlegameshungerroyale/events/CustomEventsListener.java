package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.DamageSource;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class CustomEventsListener implements Listener {
    public final GameManager gameManager;
    public CustomEventsListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void callGameDeathEvent(GameDamageEvent event) {
        if (event.getDamage() >= event.getVictim().getHealth()) {
            GameDeathEvent deathEvent = new GameDeathEvent(event.getVictim(), event.getAggressor(), event.getGame(), event.getDamage(), event.getType());
            deathEvent.callEvent();
        }
    }

    @EventHandler
    public void callPlayerEliminatedEvent(GameDeathEvent event) {
        if (event.getGame().getLivesLeft(event.getRecentlyDeceased().getUniqueId()) <= 1)
            new PlayerEliminatedEvent(event.getRecentlyDeceased(), event.getGame()).callEvent();
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void callGameDamageEvent(EntityDamageEvent event) {
        Player aggressor = null;
        Player victim;
        Game game;
        boolean directDamage = false;
        String bestGuess = null;

        if (event.getEntity() instanceof Player) {
            victim = (Player) event.getEntity();
        } else
            return;

        if (event instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
            if (damager instanceof Player) {
                aggressor = (Player) damager;
                directDamage = true;
            }

            else if (damager instanceof Projectile) {
                if (((Projectile) damager).getShooter() instanceof Player) {
                    aggressor = (Player) ((Projectile) damager).getShooter();
                    directDamage = true;
                }
            }

            else if (damager instanceof AreaEffectCloud) {
                if (((AreaEffectCloud) damager).getSource() != null) {
                    ProjectileSource e = ((AreaEffectCloud) damager).getSource();

                    if (e instanceof Player) {
                        aggressor = (Player) e;
                        directDamage = false;
                    }
                }
            }

            else if (DamageSource.getFrom(victim) != null) {
                DamageSource ds = DamageSource.getFrom(victim);
                if (ds.isApplicable(null, event.getCause())) {
                    aggressor = ds.getSource();
                    directDamage = true;
                }
            }

            else
                bestGuess = damager.getName();
        }

        // the rest of these would all be 'indirect damage' - damage from blocks placed by a player or poison/wither/etc.
        else if (event instanceof EntityDamageByBlockEvent) {
            Block block = ((EntityDamageByBlockEvent) event).getDamager();
            if (block != null) {
                if (block.hasMetadata("player")) {
                    for (MetadataValue v : block.getMetadata("player")) {
                        Object o = v.value();
                        if (o instanceof UUID) {
                            aggressor = Bukkit.getPlayer((UUID) o);
                            directDamage = false;
                        }
                    }
                }
                else
                    bestGuess = String.format("%s(%s,%s,%s)", block.getType().name(), block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ());
            }
    }

        // this damage wasn't caused by a block or an entity so must have been done by something even more indirect - hopefully if from a player it was tagged properly...
        else {
            switch (event.getCause()) {
                case FIRE_TICK:
                case POISON:
                case WITHER:
                case MAGIC:
                case FALL:
                    if (DamageSource.getFrom(victim) != null) {
                        DamageSource ds = DamageSource.getFrom(victim);
                        if (ds != null && ds.isApplicable(null, event.getCause())) {
                            aggressor = ds.getSource();
                            directDamage = false;
                        }
                    }
                    break;
            }
        }

        if (event.getDamage() > 0) {
            game = gameManager.getPlayersCurrentGame(victim);

            if (victim.isDead())
                return;

            if (game == null) // not playing a game
                return;

            GameDamageEvent gameDamageEvent = new GameDamageEvent(aggressor, victim, game, event.getFinalDamage(), directDamage, event.getCause());
            if (bestGuess != null)
                gameDamageEvent.setBestGuess(bestGuess);
            gameDamageEvent.callEvent();

            if (gameDamageEvent.isCancelled())
                event.setCancelled(true);

            event.setDamage(gameDamageEvent.getDamage());
        }
    }}
