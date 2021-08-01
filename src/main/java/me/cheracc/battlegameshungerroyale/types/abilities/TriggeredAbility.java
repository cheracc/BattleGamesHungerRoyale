package me.cheracc.battlegameshungerroyale.types.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.events.PlayerLootedChestEvent;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.AbilityTrigger;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class TriggeredAbility extends Ability implements Listener {
    public TriggeredAbility() {
    }

    public abstract AbilityTrigger getTrigger();

    public abstract void onTrigger(Player player, Event event);

    @EventHandler
    public void doDamageTriggers(EntityDamageByEntityEvent event) {
        PlayerManager playerManager = JavaPlugin.getPlugin(BGHR.class).getApi().getPlayerManager();

        if (event.getDamager() instanceof Player || event.getDamager() instanceof Projectile) {
            Player p = null;
            if (event.getDamager() instanceof Player)
                p = (Player) event.getDamager();
            else {
                if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
                    p = (Player) ((Projectile) event.getDamager()).getShooter();
                    if (getTrigger() == AbilityTrigger.DEAL_PROJECTILE_HIT) {
                        onTrigger(p, event);
                        return;
                    }
                }
            }

            if (p != null) {
                if (playerManager.getPlayerData(p).hasKit(this.getAssignedKit())) {
                    if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
                            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                        if (getTrigger() == AbilityTrigger.DEAL_MELEE_HIT) {
                            onTrigger(p, event);
                            return;
                        }
                    }
                    if (getTrigger() == AbilityTrigger.DEAL_ANY_DAMAGE) {
                        onTrigger(p, event);
                        return;
                    }
                    if (event.getEntity() instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity) event.getEntity();
                        if (event.getFinalDamage() > entity.getHealth()) {
                            if (entity instanceof Player && getTrigger() == AbilityTrigger.KILL_PLAYER) {
                                onTrigger(p, event);
                                return;
                            }
                            if (entity instanceof Monster && getTrigger() == AbilityTrigger.KILL_MONSTER) {
                                onTrigger(p, event);
                                return;
                            }
                            if (entity instanceof Animals && getTrigger() == AbilityTrigger.KILL_ANIMAL) {
                                onTrigger(p, event);
                                return;
                            }
                            if (getTrigger() == AbilityTrigger.KILL_ANYTHING) {
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

            if (playerManager.getPlayerData(p).hasKit(this.getAssignedKit())) {
                if (getTrigger() == AbilityTrigger.TAKE_ANY_DAMAGE) {
                    onTrigger(p, event);
                    return;
                }
                if (event.getCause().name().toLowerCase().contains("attack")) {
                    if (getTrigger() == AbilityTrigger.TAKE_MELEE_HIT) {
                        onTrigger(p, event);
                        return;
                    }
                }
                if (event.getDamager() instanceof Projectile && getTrigger() == AbilityTrigger.TAKE_PROJECTILE_HIT) {
                    onTrigger(p, event);
                    return;
                }
                if (event.getFinalDamage() > p.getHealth()) {
                    if (getTrigger() == AbilityTrigger.DEATH_BY_ANY) {
                        onTrigger(p, event);
                        return;
                    }
                    if (event.getDamager() instanceof Player && getTrigger() == AbilityTrigger.DEATH_BY_PLAYER) {
                        onTrigger(p, event);
                        return;
                    }
                    if (event.getDamager() instanceof Projectile) {
                        if (((Projectile) event.getDamager()).getShooter() instanceof Player) {
                            if (getTrigger() == AbilityTrigger.DEATH_BY_PLAYER) {
                                onTrigger(p, event);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void doLootTrigger(PlayerLootedChestEvent event) {
        BghrApi api = JavaPlugin.getPlugin(BGHR.class).getApi();
        for (Player p : event.getEvent().getInventoryHolder().getInventory().getLocation().getNearbyPlayers(4)) {
            if (api.getPlayerManager().getPlayerData(p).hasKit(this.getAssignedKit()) &&
                    getTrigger() == AbilityTrigger.OPEN_LOOT_CHEST && p.getOpenInventory() != null) {
                onTrigger(p, event);
            }
        }
    }
}
