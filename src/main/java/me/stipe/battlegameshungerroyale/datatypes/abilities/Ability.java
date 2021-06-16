package me.stipe.battlegameshungerroyale.datatypes.abilities;

import me.stipe.battlegameshungerroyale.BGHR;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public abstract class Ability {
    public static final NamespacedKey ABILITY_KEY = new NamespacedKey(BGHR.getPlugin(), "ability_key");
    private final String description;
    private final int cooldown;
    private final String name;

    public Ability(String name, String description, int cooldown) {
        this.description = description;
        this.cooldown = cooldown;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getCooldown() {
        return cooldown;
    }

    public String getDescription() {
        return description;
    }

    public void attachNewUuid(ItemMeta meta, String value) {
        meta.getPersistentDataContainer().set(ABILITY_KEY, PersistentDataType.STRING, value);
    }

    public static UUID getUuid(ItemStack item) {
        if (item == null)
            return null;

        if (item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().has(Ability.ABILITY_KEY, PersistentDataType.STRING)) {
            String uuidString = item.getItemMeta().getPersistentDataContainer().get(Ability.ABILITY_KEY, PersistentDataType.STRING);
            if (uuidString == null)
                return null;
            return UUID.fromString(uuidString);
        }
        return null;
    }

    public abstract void load(ConfigurationSection section);

}
