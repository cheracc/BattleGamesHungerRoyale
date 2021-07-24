package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.types.DamageSource;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;

public class EffectProjectile extends Ability implements ActiveAbility, Listener {
    String projectileItemType = "egg";
    PotionEffect potionEffect = PotionEffectType.SPEED.createEffect(60, 0);
    String projectileName = "Egg";
    boolean infiniteProjectiles = false;
    boolean affectedByGravity = true;
    int startItemCount = 3;
    int cooldown;

    public EffectProjectile() {
        setDescription("provides an item that can be thrown as a projectile (optionally consumable and/or regenerating) that causes a potion effect on the target it hits");
    }

    @Override
    public boolean doAbility(Player source) {
        ThrowableProjectile projectile = source.launchProjectile(Snowball.class);
        projectile.setItem(new ItemStack(Material.valueOf(projectileItemType.toUpperCase())));
        projectile.setShooter(source);
        projectile.setRotation(1,2);
        projectile.setGravity(affectedByGravity);
        projectile.setMetadata("effectprojectile", new FixedMetadataValue(plugin, getId()));
        projectile.setMetadata("thrower", new FixedMetadataValue(plugin, source.getUniqueId()));

        ItemStack kitItem = source.getInventory().getItemInMainHand();
        if (kitItem != null && !infiniteProjectiles) {
            kitItem.setAmount(kitItem.getAmount() - 1);
        }
        return true;
    }

    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof ThrowableProjectile) {
            ThrowableProjectile proj = (ThrowableProjectile) event.getDamager();
            List<MetadataValue> mv = proj.getMetadata("effectprojectile");
            if (mv != null && !mv.isEmpty()) {
                Object o = proj.getMetadata("effectprojectile").get(0).value();
                if (o instanceof UUID) {
                    if (getId().equals(o)) {
                        o = proj.getMetadata("thrower").get(0).value();
                        if (o instanceof UUID) {
                            Player damager = Bukkit.getPlayer((UUID) o);

                            if (damager != null && event.getEntity() instanceof Player && PlayerManager.getInstance().getPlayerData(damager).hasKit(this.getAssignedKit())) {
                                Player target = (Player) event.getEntity();
                                EntityDamageEvent.DamageCause type = null;

                                if (potionEffect.getType() == PotionEffectType.POISON)
                                    type = EntityDamageEvent.DamageCause.POISON;
                                if (potionEffect.getType() == PotionEffectType.WITHER)
                                    type = EntityDamageEvent.DamageCause.WITHER;

                                if (target != null && type != null)
                                    new DamageSource(damager, type, potionEffect.getDuration()).apply(target);
                            }

                            if (potionEffect != null && event.getEntity() instanceof LivingEntity)
                                potionEffect.apply((LivingEntity) event.getEntity());
                        }
                    }
                }
            }
        }
    }

    @Override
    public ItemStack createAbilityItem() {
        ItemStack ammo = makeItem(Material.valueOf(projectileItemType.toUpperCase()), projectileName, getDescription(), cooldown);
        if (!infiniteProjectiles)
            ammo.setAmount(startItemCount);
        return ammo;
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }
}
