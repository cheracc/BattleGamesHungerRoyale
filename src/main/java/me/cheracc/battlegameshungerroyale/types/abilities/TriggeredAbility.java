package me.cheracc.battlegameshungerroyale.types.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.types.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.LootGenerateEvent;

public abstract class TriggeredAbility extends Ability implements Listener {
    public abstract Trigger getTrigger();
    public abstract void onTrigger(Player player, Event event);

    public TriggeredAbility() {
    }

    @Override
    public void setAssignedKit(Kit kit) {
        super.setAssignedKit(kit);
        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    public enum Trigger {
        DEAL_ANY_DAMAGE,
        DEAL_MELEE_HIT,
        DEAL_PROJECTILE_HIT,
        TAKE_ANY_DAMAGE,
        TAKE_MELEE_HIT,
        TAKE_PROJECTILE_HIT,
        KILL_ANYTHING,
        KILL_PLAYER,
        KILL_MONSTER,
        KILL_ANIMAL,
        DEATH_BY_ANY,
        DEATH_BY_PLAYER,
        OPEN_LOOT_CHEST
    }

    @EventHandler
    public void doDamageTriggers(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player || event.getDamager() instanceof Projectile) {
            Player p = null;
            if (event.getDamager() instanceof Player)
                p = (Player) event.getDamager();
            else {
                if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
                    p = (Player) ((Projectile) event.getDamager()).getShooter();
                    if (getTrigger() == Trigger.DEAL_PROJECTILE_HIT) {
                        onTrigger(p, event);
                        return;
                    }
                }
            }

            if (p != null) {
                if (PlayerManager.getInstance().getPlayerData(p).hasKit(this.getAssignedKit())) {
                    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
                            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                        if (getTrigger() == Trigger.DEAL_MELEE_HIT) {
                            onTrigger(p, event);
                            return;
                        }
                    }
                    if (getTrigger() == Trigger.DEAL_ANY_DAMAGE) {
                        onTrigger(p, event);
                        return;
                    }
                    if (event.getEntity() instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity) event.getEntity();
                        if (event.getFinalDamage() > entity.getHealth()) {
                            if (entity instanceof Player && getTrigger() == Trigger.KILL_PLAYER) {
                                onTrigger(p, event);
                                return;
                            }
                            if (entity instanceof Monster && getTrigger() == Trigger.KILL_MONSTER) {
                                onTrigger(p, event);
                                return;
                            }
                            if (entity instanceof Animals && getTrigger() == Trigger.KILL_ANIMAL) {
                                onTrigger(p, event);
                                return;
                            }
                            if (getTrigger() == Trigger.KILL_ANYTHING) {
                                onTrigger(p, event);
                                return;
                            }
                        }

                    }
                }
            }
        }
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();

            if (PlayerManager.getInstance().getPlayerData(p).hasKit(this.getAssignedKit())) {
                if (getTrigger() == Trigger.TAKE_ANY_DAMAGE) {
                    onTrigger(p, event);
                    return;
                }
                if (event.getCause().name().toLowerCase().contains("attack")) {
                    if (getTrigger() == Trigger.TAKE_MELEE_HIT) {
                        onTrigger(p, event);
                        return;
                    }
                }
                if (event.getDamager() instanceof Projectile && getTrigger() == Trigger.TAKE_PROJECTILE_HIT) {
                    onTrigger(p, event);
                    return;
                }
                if (event.getFinalDamage() > p.getHealth()) {
                    if (getTrigger() == Trigger.DEATH_BY_ANY) {
                        onTrigger(p, event);
                        return;
                    }
                    if (event.getDamager() instanceof Player && getTrigger() == Trigger.DEATH_BY_PLAYER) {
                        onTrigger(p, event);
                        return;
                    }
                    if (event.getDamager() instanceof Projectile) {
                        if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
                            if (getTrigger() == Trigger.DEATH_BY_PLAYER) {
                                onTrigger(p, event);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void doLootTrigger(LootGenerateEvent event) {
        for (Player p : event.getInventoryHolder().getInventory().getLocation().getNearbyPlayers(4)) {
            if (PlayerManager.getInstance().getPlayerData(p).hasKit(this.getAssignedKit()) &&
                    getTrigger() == Trigger.OPEN_LOOT_CHEST && p.getOpenInventory() != null) {
                onTrigger(p, event);
            }
        }
    }
}
