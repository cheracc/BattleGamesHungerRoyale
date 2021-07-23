package me.cheracc.battlegameshungerroyale.types.abilities.enums;

import me.cheracc.battlegameshungerroyale.types.abilities.AbilityOptionEnum;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

public enum TotemAttackType implements AbilityOptionEnum {
    ARROW, FIREBALL, TRIDENT, SHULKER_BULLET, WITHER_SKULL;

    @Override
    public AbilityOptionEnum next() {
        int ordinal = this.ordinal() + 1;
        if (ordinal >= values().length)
            ordinal = 0;
        return values()[ordinal];
    }

    public Projectile getProjectile(LivingEntity totem, Location target) {
        Projectile p;
        Vector direction = target.subtract(totem.getLocation()).toVector().normalize();
        totem.getLocation().setDirection(direction);

        switch (this) {
            case TRIDENT:
                p = totem.launchProjectile(Trident.class, direction);
                break;
            case FIREBALL:
                p = totem.launchProjectile(Fireball.class, direction);
                break;
            case SHULKER_BULLET:
                p = totem.launchProjectile(ShulkerBullet.class, direction);
                break;
            case WITHER_SKULL:
                p = totem.launchProjectile(WitherSkull.class, direction);
                break;
            default:
                p = totem.launchProjectile(Arrow.class, direction);
                p.setVelocity(direction.multiply(2));
        }
        //p.getLocation().setDirection(direction);
        return p;
    }
}
