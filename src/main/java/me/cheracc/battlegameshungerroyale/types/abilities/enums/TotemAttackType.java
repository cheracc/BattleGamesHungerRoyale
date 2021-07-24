package me.cheracc.battlegameshungerroyale.types.abilities.enums;

import me.cheracc.battlegameshungerroyale.types.abilities.AbilityOptionEnum;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum TotemAttackType implements AbilityOptionEnum, ConfigurationSerializable {
    FIREBALL, WITHER_SKULL, ITEM, ARROW;

    @Override
    public AbilityOptionEnum next() {
        int ordinal = this.ordinal() + 1;
        if (ordinal >= values().length)
            ordinal = 0;
        return values()[ordinal];
    }

    public static TotemAttackType deserialize(Map<String, Object> map) {
        return valueOf((String) map.get("name"));
    }

    public Projectile getProjectile(LivingEntity totem, Location target) {
        Projectile p;
        Vector direction = target.subtract(totem.getEyeLocation().clone().add(0,1,0)).toVector().normalize();
        totem.teleport(totem.getLocation().add(0,0.5,0));

        switch (this) {
            case WITHER_SKULL:
                WitherSkull skull = totem.launchProjectile(WitherSkull.class);
                skull.setCharged(true);
                skull.setYield(0F);
                p = skull;
                break;
            case ARROW:
                p = totem.launchProjectile(Arrow.class);
                p.setVelocity(p.getVelocity().multiply(2));
                ((ArmorStand) totem).setArms(true);
                ((ArmorStand) totem).setRightArmPose(new EulerAngle(-Math.PI/2, -Math.PI/8, 0));
                ((ArmorStand) totem).setItem(EquipmentSlot.HAND, new ItemStack(Material.CROSSBOW));
                break;
            case ITEM:
                p = totem.launchProjectile(Snowball.class, direction.multiply(2));
                ((Snowball) p).setItem(new ItemStack(Material.DIAMOND));
                break;
            default:
                p = totem.launchProjectile(SmallFireball.class);
                ((Fireball) p).setIsIncendiary(false);
                ((Fireball) p).setYield(0);
        }
        totem.teleport(totem.getLocation().add(0,-0.5,0));

        return p;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> value = new HashMap<>();
        value.put("name", name());
        return value;
    }
}
