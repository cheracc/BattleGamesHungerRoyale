package me.cheracc.battlegameshungerroyale.types.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.TotemType;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;

public abstract class Totem extends Ability implements ActiveAbility, Listener {
    public abstract TotemType getTotemType();
    public abstract boolean isDestroyable();
    public abstract int getDuration();
    public abstract int getAttackSpeed();
    public abstract ItemStack getTotemHead();
    public abstract void doTotemAbility(LivingEntity totem, Player owner);

    public UnaryOperator<LivingEntity> additionalOptions() {
        return livingEntity -> livingEntity;
    }

    public void startTotemWatcher(LivingEntity totem) {
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

                if (count % (2 * getAttackSpeed()) == 0)
                    doTotemAbility(totem, owner);

                long expiry = totem.getMetadata("expires").get(0).asLong();
                if (expiry < System.currentTimeMillis()) {
                    destroyTotem(totem);
                    cancel();
                }
                count++;
            }
        }.runTaskTimer(BGHR.getPlugin(), 5L, 10L);
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

    @Override
    public boolean doAbility(Player source) {
        BGHR plugin = BGHR.getPlugin();
        Location loc = source.getLocation();

        LivingEntity totem = getTotemType().createBaseTotem(loc, getTotemHead(), getCustomName());
        if (!isDestroyable()) {
            totem.setMetadata("invulnerable", new FixedMetadataValue(plugin, true));
        }
        totem.setMetadata("expires", new FixedMetadataValue(plugin, System.currentTimeMillis() + (getDuration() * 1000L)));
        totem.setMetadata("owner", new FixedMetadataValue(plugin, source.getUniqueId()));

        additionalOptions().apply(totem);
        startTotemWatcher(totem);
        return true;
    }

    @EventHandler (ignoreCancelled = true)
    public void noDropsFromTotems(EntityDeathEvent event) {
        if (event.getEntity() instanceof ArmorStand)
            event.getDrops().clear();
    }

    @EventHandler
    public void damageTotems(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        Entity e = p.getTargetEntity(2);

        if (e instanceof ArmorStand && !e.hasMetadata("invulnerable") && ((ArmorStand) e).getNoDamageTicks() <= 0) {
            if (e.hasMetadata("expires")) {
                ArmorStand totem = (ArmorStand) e;
                double damage = 1 + ThreadLocalRandom.current().nextDouble();
                totem.getWorld().playSound(totem.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 1F, 1F);
                totem.getWorld().spawnParticle(Particle.CRIT, totem.getLocation(), 4);
                if (totem.getHealth() - damage <= 0)
                    destroyTotem(totem);
                else {
                    totem.setHealth(totem.getHealth() - damage);
                    totem.setNoDamageTicks(8);
                }
            }
        }
    }


}
