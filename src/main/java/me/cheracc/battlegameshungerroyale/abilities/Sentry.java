package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.types.DamageSource;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.TotemAttackType;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Sentry extends Ability implements ActiveAbility, Listener {
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
        setDescription("Places a totem that attacks nearby enemies until it is destroyed or expires");
        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    private LivingEntity createBaseTotem(Location loc) {
        while (!loc.getBlock().isSolid()) {
            loc.add(0, -1, 0);
        }
        loc.add(0,1,0);
        ArmorStand totem = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        ItemStack head = new ItemStack(Material.valueOf(totemItemType.toUpperCase()));

        totem.setSmall(true);
        totem.setInvulnerable(true);
        totem.setCustomName(totemName);
        totem.setCustomNameVisible(true);
        totem.setBasePlate(false);
        totem.setArms(false);
        totem.setAI(false);
        totem.setHealth(4);
        totem.setSilent(true);
        totem.getEquipment().setHelmet(head, false);
        totem.setItem(EquipmentSlot.CHEST, new ItemStack(Material.IRON_CHESTPLATE));
        totem.setItem(EquipmentSlot.LEGS, new ItemStack(Material.IRON_LEGGINGS));
        totem.setItem(EquipmentSlot.FEET, new ItemStack(Material.IRON_BOOTS));
        totem.setDisabledSlots(EquipmentSlot.values());

        return totem;
    }

    private void startTotemWatcher(LivingEntity totem) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (totem == null || totem.isDead() || !totem.isValid()) {
                    cancel();
                }
                Player owner = Bukkit.getPlayer((UUID) totem.getMetadata("owner").get(0).value());
                if (owner == null || !owner.isOnline()) {
                    destroyTotem(totem);
                    cancel();
                }

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

                if (!targetsWithinRange.isEmpty() && (count % (2 * secondsBetweenAttacks)) == 0) {
                    Collections.shuffle(targetsWithinRange);
                    LivingEntity target = targetsWithinRange.get(0);
                    Location loc = totem.getLocation().setDirection(target.getLocation().subtract(totem.getLocation()).toVector());
                    totem.teleport(loc);
                    attack(totem, target, owner);
                }

                long expiry = totem.getMetadata("expires").get(0).asLong();
                if (expiry < System.currentTimeMillis()) {
                    destroyTotem(totem);
                    cancel();
                }
                count++;
            }
        }.runTaskTimer(BGHR.getPlugin(), 5L, 10L);
    }

    private void attack(LivingEntity totem, LivingEntity target, Player owner) {
        Projectile proj = attackType.getProjectile(totem, target.getEyeLocation());
        proj.setMetadata("owner", new FixedMetadataValue(BGHR.getPlugin(), owner.getUniqueId()));
    }

    private void destroyTotem(LivingEntity totem) {
        new BukkitRunnable() {
            @Override
            public void run() {
                totem.getWorld().playEffect(totem.getLocation(), Effect.LAVA_CONVERTS_BLOCK, null);
                totem.remove();
            }
        }.runTask(BGHR.getPlugin());
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
    public boolean doAbility(Player source) {
        BGHR plugin = BGHR.getPlugin();
        Location loc = source.getLocation();

        LivingEntity totem = createBaseTotem(loc);
        if (!totemIsDestroyable)
            totem.setInvulnerable(true);
        totem.setMetadata("expires", new FixedMetadataValue(plugin, System.currentTimeMillis() + (duration * 1000L)));
        if (!totemIsDestroyable)
            totem.setMetadata("invulnerable", new FixedMetadataValue(plugin, true));
        totem.setMetadata("owner", new FixedMetadataValue(plugin, source.getUniqueId()));

        startTotemWatcher(totem);
        return true;
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
