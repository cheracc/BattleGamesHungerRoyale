package me.cheracc.battlegameshungerroyale.abilities;

import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
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
import org.bukkit.metadata.FixedMetadataValue;
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
            projectile.setShooter(owner);
            projectile.setMetadata("owner", new FixedMetadataValue(plugin, owner.getUniqueId()));
            projectile.setMetadata("ability_id", new FixedMetadataValue(plugin, this.getId()));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (projectile != null && projectile.isValid())
                        projectile.remove();
                }
            }.runTaskLater(plugin, (projectile instanceof Arrow) ? 30L : 200L);
        }
    }

    private boolean isMyProjectile(Projectile projectile) {
        if (projectile.hasMetadata("owner") && projectile.hasMetadata("ability_id")) {
            UUID ownerId = (UUID) projectile.getMetadata("owner").get(0).value();
            UUID abilityId = (UUID) projectile.getMetadata("ability_id").get(0).value();
            if (PlayerManager.getInstance().getPlayerData(ownerId).hasKit(getAssignedKit())) {
                return (abilityId.equals(getId()));
            }
        }
        return false;
    }

    private Player getOwner(Projectile projectile) {
        if (projectile.hasMetadata("owner")) {
            UUID uuid = (UUID) projectile.getMetadata("owner").get(0).value();
            return Bukkit.getPlayer(uuid);
        }
        return null;

    }

    @EventHandler
    public void stopExplosions(EntityExplodeEvent event) {
        int fireDuration = 100;
        event.setYield(0F);
        if (event.getEntity() instanceof WitherSkull && isMyProjectile((Projectile) event.getEntity())) {
            WitherSkull projectile = (WitherSkull) event.getEntity();
            event.blockList().clear();
            if (projectilesExplode) {
                Player owner = getOwner(projectile);
                projectile.getLocation().createExplosion(owner, attackDamage/2F, false, false);
                if (incendiaryProjectiles) {
                    for (LivingEntity e : projectile.getLocation().getNearbyLivingEntities(attackDamage/2F, attackDamage/2F, attackDamage/2F)) {
                        e.setFireTicks(fireDuration);
                        if (e instanceof Player)
                            new DamageSource(owner, EntityDamageEvent.DamageCause.FIRE_TICK, (int) (fireDuration * 1.1)).apply((Player) e);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onProjectileCollide(ProjectileCollideEvent event) {
        Projectile projectile = event.getEntity();
        int fireDuration = 100;

        if (!isMyProjectile(projectile))
            return;

        projectile.remove();
        if (projectilesExplode) {
            Player owner = getOwner(projectile);
            projectile.getLocation().createExplosion(owner, attackDamage, false, false);
            if (incendiaryProjectiles) {
                for (LivingEntity e : projectile.getLocation().getNearbyLivingEntities(attackDamage/2F, attackDamage/2F, attackDamage/2F)) {
                    e.setFireTicks(fireDuration);
                    if (e instanceof Player)
                        new DamageSource(owner, EntityDamageEvent.DamageCause.FIRE_TICK, (int) (fireDuration * 1.1)).apply((Player) e);
                }
            }
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
                if (target instanceof Player)
                    new DamageSource(owner, EntityDamageEvent.DamageCause.PROJECTILE, 20).apply((Player) target);
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
