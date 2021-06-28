package me.stipe.battlegameshungerroyale.events;

import me.stipe.battlegameshungerroyale.datatypes.DamageSource;
import me.stipe.battlegameshungerroyale.datatypes.Game;
import me.stipe.battlegameshungerroyale.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class CustomEventsListener implements Listener {

    @EventHandler
    public void callPvpDamageEvent(EntityDamageEvent event) {
        Player aggressor = null;
        Player victim;
        Game game;
        boolean directDamage = false;

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

            else if (damager.hasMetadata("player")) {
                for (MetadataValue v : damager.getMetadata("player")) {
                    Object o = v.value();
                    if (o instanceof UUID) {
                        aggressor = Bukkit.getPlayer((UUID) o);
                        directDamage = true;
                    }
                }
            }
        }

        // the rest of these would all be 'indirect damage' - damage from blocks placed by a player or poison/wither/etc.
        else if (event instanceof EntityDamageByBlockEvent) {
            Block block = ((EntityDamageByBlockEvent) event).getDamager();

            if (block.hasMetadata("player")) {
                for (MetadataValue v : block.getMetadata("player")) {
                    Object o = v.value();
                    if (o instanceof UUID) {
                        aggressor = Bukkit.getPlayer((UUID) o);
                        directDamage = false;
                    }
                }
            }
        }

        // this damage wasn't caused by a block or an entity so must have been done by a damage tick
        else {
            switch (event.getCause()) {
                case FIRE_TICK:
                case POISON:
                case WITHER:
                    if (DamageSource.getFrom(victim) != null) {
                        DamageSource ds = DamageSource.getFrom(victim);
                        if (ds.getType() == event.getCause()) {
                            if (System.currentTimeMillis() - ds.getTimeApplied() > ds.getDuration())
                                ds.remove(victim);
                            else {
                                aggressor = ds.getSource();
                                directDamage = false;
                            }
                        }
                    }
                    break;
                default:
                    return;
            }
        }

        if (aggressor != null && victim != null && event.getDamage() > 0) {
            game = GameManager.getInstance().getPlayersCurrentGame(victim);

            if (game == null || !game.equals(GameManager.getInstance().getPlayersCurrentGame(aggressor))) {
                return;
            }

            PvpDamageEvent pvpDamageEvent = new PvpDamageEvent(aggressor, victim, game, event.getDamage(), directDamage);
            pvpDamageEvent.callEvent();

            if (pvpDamageEvent.isCancelled())
                event.setCancelled(true);

            event.setDamage(pvpDamageEvent.getDamage());
        }
    }}
