package me.cheracc.battlegameshungerroyale.types.abilities.enums;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum TotemType implements AbilityOptionEnum, ConfigurationSerializable {
    FIRE_TOTEM, SENTRY, BASIC, INVISIBLE;

    public LivingEntity createBaseTotem(Location loc, ItemStack head, String customName) {
        while (!loc.getBlock().isSolid()) {
            loc.add(0, -1, 0);
        }
        loc.add(0,1,0);
        ArmorStand totem = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

        totem.setSmall(true);
        totem.setInvulnerable(true);
        totem.setCustomName(customName);
        totem.setCustomNameVisible(true);
        totem.setBasePlate(false);
        totem.setArms(false);
        totem.setAI(false);
        totem.setHealth(4);
        totem.getEquipment().setHelmet(head, false);

        switch (this) {
            case FIRE_TOTEM:
                totem.setFireTicks(9999);
                totem.setInvisible(true);
                break;
            case SENTRY:
                totem.setItem(EquipmentSlot.CHEST, new ItemStack(Material.IRON_CHESTPLATE));
                totem.setItem(EquipmentSlot.LEGS, new ItemStack(Material.IRON_LEGGINGS));
                totem.setItem(EquipmentSlot.FEET, new ItemStack(Material.IRON_BOOTS));
                break;
            case BASIC:
                totem.setCustomNameVisible(false);
                break;
            case INVISIBLE:
                totem.setInvisible(true);
                totem.setCustomNameVisible(false);
        }
        totem.setDisabledSlots(EquipmentSlot.values());

        return totem;
    }

    @Override
    public AbilityOptionEnum next() {
        int ordinal = this.ordinal() + 1;
        if (ordinal >= values().length)
            ordinal = 0;
        return values()[ordinal];
    }

    public static TotemType deserialize(Map<String, Object> map) {
        return valueOf((String) map.get("name"));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> value = new HashMap<>();
        value.put("name", name());
        return value;
    }
}
