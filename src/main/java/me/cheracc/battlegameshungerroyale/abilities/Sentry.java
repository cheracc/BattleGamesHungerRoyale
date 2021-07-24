package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.types.DamageSource;
import me.cheracc.battlegameshungerroyale.types.abilities.Totem;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.TotemAttackType;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.TotemType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Sentry extends Totem implements Listener {
    private int cooldown;
    private int duration;
    private int attackRadius;
    private int attackDamage;
    private int secondsBetweenAttacks;
    private boolean totemIsDestroyable;
    private boolean incendiaryProjectiles;
    private boolean projectilesExplode;
    private String totemItemType;
    private String totemName;
    private String itemDescription;
    private TotemType totemType;
    private TotemAttackType attackType;

    public Sentry() {
        this.cooldown = 30;
        this.duration = 10;
        this.attackRadius = 8;
        this.attackDamage = 1;
        this.secondsBetweenAttacks = 2;
        this.totemIsDestroyable = true;
        this.incendiaryProjectiles = false;
        this.projectilesExplode = false;
        this.totemItemType = "target";
        this.totemName = "Attack Totem";
        this.itemDescription = "Use this to place your attack totem";
        this.attackType = TotemAttackType.FIREBALL;
        this.totemType = TotemType.SENTRY;
        setDescription("Places a totem that attacks nearby enemies until it is destroyed or expires");
        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    @Override
    public TotemType getTotemType() {
        return totemType;
    }

    @Override
    public boolean isDestroyable() {
        return totemIsDestroyable;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getAttackSpeed() {
        return secondsBetweenAttacks;
    }

    @Override
    public ItemStack getTotemHead() {
        return new ItemStack(Material.valueOf(totemItemType.toUpperCase()));
    }

    @Override
    public void doTotemAbility(LivingEntity totem, Player owner) {
        int scanRange = 1;
        List<LivingEntity> targetsWithinRange = new ArrayList<>(totem.getLocation().getNearbyPlayers(scanRange, scanRange, scanRange));
        targetsWithinRange.remove(owner);
        targetsWithinRange.removeIf(entity -> !totem.hasLineOfSight(entity));
        while (scanRange <= attackRadius && targetsWithinRange.isEmpty()) {
            scanRange++;
            targetsWithinRange.addAll(totem.getLocation().getNearbyPlayers(scanRange, scanRange, scanRange));
            targetsWithinRange.remove(owner);
            targetsWithinRange.removeIf(entity -> !totem.hasLineOfSight(entity));
        }

        if (targetsWithinRange.isEmpty()) {
            scanRange = 1;
            targetsWithinRange.addAll(totem.getLocation().getNearbyEntitiesByType(Monster.class, scanRange, scanRange, scanRange));
            targetsWithinRange.removeIf(entity -> !totem.hasLineOfSight(entity));
            while (scanRange <= attackRadius && targetsWithinRange.isEmpty()) {
                scanRange++;
                targetsWithinRange.addAll(totem.getLocation().getNearbyEntitiesByType(Monster.class, scanRange, scanRange, scanRange));
                targetsWithinRange.removeIf(entity -> !totem.hasLineOfSight(entity));
            }
        }

        if (!targetsWithinRange.isEmpty()) {
            Collections.shuffle(targetsWithinRange);
            LivingEntity target = targetsWithinRange.get(0);
            Location loc = totem.getLocation().setDirection(target.getLocation().subtract(totem.getLocation()).toVector());
            totem.teleport(loc);
            Projectile projectile = attackType.getProjectile(totem, target.getEyeLocation());
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (projectile != null && projectile.isValid())
                        projectile.remove();
                }
            }.runTaskLater(BGHR.getPlugin(), 40L);
        }
    }

    @EventHandler
    public void stopExplosions(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Projectile && event.getEntity().hasMetadata("owner")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Projectile && event.getEntity() instanceof LivingEntity) {
            Projectile proj = (Projectile) event.getDamager();
            LivingEntity target = (LivingEntity) event.getEntity();
            if (proj.hasMetadata("owner")) {
                Player owner = Bukkit.getPlayer((UUID) proj.getMetadata("owner").get(0).value());
                if (owner == null || !owner.isOnline() || !PlayerManager.getInstance().getPlayerData(owner).hasKit(this.getAssignedKit()))
                    return;
                event.setDamage(attackDamage);
                if (incendiaryProjectiles) {
                    target.setFireTicks(100);
                    if (target instanceof Player)
                        new DamageSource(owner, EntityDamageEvent.DamageCause.FIRE_TICK, 110).apply((Player) target);
                }
                if (target instanceof Player)
                    new DamageSource(owner, EntityDamageEvent.DamageCause.PROJECTILE, 20).apply((Player) target);

                if (projectilesExplode) {
                    target.getLocation().createExplosion(owner, attackDamage/4F, false, false);
                }

                proj.remove();
            }
        }
    }

    @Override
    public ItemStack createAbilityItem() {
        return makeItem(Material.valueOf(totemItemType.toUpperCase()), totemName, itemDescription, cooldown);
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }
}
