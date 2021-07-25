package me.cheracc.battlegameshungerroyale.types.abilities.enums;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum EnchantWrapper implements AbilityOptionEnum, ConfigurationSerializable {
    AQUA_AFFINITY,
    BLAST_PROTECTION,
    CHANNELING,
    DEPTH_STRIDER,
    FEATHER_FALLING,
    FIRE_ASPECT,
    FIRE_PROTECTION,
    KNOCKBACK,
    LOYALTY,
    PIERCING,
    POWER,
    PROJECTILE_PROTECTION,
    PROTECTION,
    PUNCH,
    QUICK_CHARGE,
    RESPIRATION,
    RIPTIDE,
    SHARPNESS,
    SMITE,
    SWEEPING_EDGE,
    THORNS,
    UNBREAKING;

    public Enchantment getEnchantment() {
        switch (this) {
            case POWER:
                return Enchantment.ARROW_DAMAGE;
            case PUNCH:
                return Enchantment.ARROW_KNOCKBACK;
            case SMITE:
                return Enchantment.DAMAGE_UNDEAD;
            case THORNS:
                return Enchantment.THORNS;
            case LOYALTY:
                return Enchantment.LOYALTY;
            case RIPTIDE:
                return Enchantment.RIPTIDE;
            case PIERCING:
                return Enchantment.PIERCING;
            case KNOCKBACK:
                return Enchantment.KNOCKBACK;
            case SHARPNESS:
                return Enchantment.DAMAGE_ALL;
            case CHANNELING:
                return Enchantment.CHANNELING;
            case PROTECTION:
                return Enchantment.PROTECTION_ENVIRONMENTAL;
            case UNBREAKING:
                return Enchantment.DURABILITY;
            case FIRE_ASPECT:
                return Enchantment.FIRE_ASPECT;
            case RESPIRATION:
                return Enchantment.OXYGEN;
            case QUICK_CHARGE:
                return Enchantment.QUICK_CHARGE;
            case AQUA_AFFINITY:
                return Enchantment.WATER_WORKER;
            case DEPTH_STRIDER:
                return Enchantment.DEPTH_STRIDER;
            case SWEEPING_EDGE:
                return Enchantment.SWEEPING_EDGE;
            case FEATHER_FALLING:
                return Enchantment.PROTECTION_FALL;
            case FIRE_PROTECTION:
                return Enchantment.PROTECTION_FIRE;
            case BLAST_PROTECTION:
                return Enchantment.PROTECTION_EXPLOSIONS;
            case PROJECTILE_PROTECTION:
                return Enchantment.PROTECTION_PROJECTILE;
            default:
                return null;
        }
    }

    @Override
    public EnchantWrapper next() {
        int ordinal = this.ordinal() + 1;
        if (ordinal >= values().length)
            ordinal = 0;
        return values()[ordinal];
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name());
        return map;
    }

    public static EnchantWrapper deserialize(Map<String, Object> map) {
        return valueOf((String) map.get("name"));
    }
}
