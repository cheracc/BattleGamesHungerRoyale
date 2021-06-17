package me.stipe.battlegameshungerroyale.datatypes.abilities;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;

public abstract class Ability {
    private static final NamespacedKey ABILITY_KEY = new NamespacedKey(BGHR.getPlugin(), "ability_key");
    private final ConfigurationSection section;
    String description = "";
    String name = getClass().getSimpleName();
    private Kit forKit = null;

    public Ability() {
        this.section = null;
    }

    public static NamespacedKey getAbilityKey() {
        return ABILITY_KEY;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String text) {
        description = text;
    }

    public Kit getAssignedKit() {
        return forKit;
    }

    public boolean isAssignedToAKit() {
        return forKit != null;
    }

    public void attachNewUuid(ItemMeta meta, String value) {
        meta.getPersistentDataContainer().set(ABILITY_KEY, PersistentDataType.STRING, value);
    }

    public static boolean isThisAnAbilityItem(ItemStack item) {
        return getUuid(item) != null;
    }

    public boolean isActive() {
        return this instanceof ActiveAbility;
    }

    public boolean isPassive() {
        return this instanceof PassiveAbility;
    }

    public static @Nullable UUID getUuid(ItemStack item) {
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

    public ItemStack makeItem(Material type, String name, String description, int cooldown) {
        ItemStack abilityItem = new ItemStack(type);
        ItemMeta meta = abilityItem.getItemMeta();
        List<Component> lore = new ArrayList<>();

        if (meta == null)
            return abilityItem;

        meta.displayName(Component.text(ChatColor.WHITE + name));
        attachNewUuid(meta, UUID.randomUUID().toString());

        lore.add(Component.text(""));
        lore.addAll(Tools.componentalize(Tools.wrapText(description, ChatColor.GRAY)));
        lore.add(Component.text(""));
        if (cooldown > 0)
            lore.add(Tools.componentalize("&7Cooldown: &f" + Tools.secondsToMinutesAndSeconds(cooldown)));

        meta.lore(lore);
        abilityItem.setItemMeta(meta);
        abilityItem.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        return abilityItem;
    }

    public abstract void load(ConfigurationSection section);

    public void load(ConfigurationSection section, Kit forKit) {
        this.forKit = forKit;
        for (String s : section.getKeys(false)) {
            String fieldName = Tools.configOptionToFieldName(s);
            try {
                Field f = getClass().getField(fieldName);
                setFieldToValue(f, section.get(s));
            } catch (NoSuchFieldException e) {
                Bukkit.getLogger().warning("no field found for config option. ability:" + getName() + " field:" + fieldName + " option:" + s);
            }
        }
        load(section);
    }

    private void setFieldToValue(Field field, Object value) {
        try {
            field.set(this, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        getConfig().set(Tools.fieldNameToConfigOption(field.getName()), value);
    }

    public Map<String, Type> getConfigurableOptions() {
        Map<String, Type> availableOptions = new HashMap<>();

        for (Field f : this.getClass().getFields()) {
            availableOptions.put(Tools.fieldNameToConfigOption(f.getName()), f.getType());
        }
        return availableOptions;
    }

    public void setConfigurableOption(String option, String value) {
        String type = getConfigurableOptions().get(option).getTypeName();

        if (type == null) {
            Bukkit.getLogger().warning("There is no configurable option: " + option);
            return;
        }

        if (type.toLowerCase().contains("boolean"))
            getConfig().set(option, Boolean.valueOf(value));
        if (type.toLowerCase().contains("int"))
            getConfig().set(option, Integer.valueOf(value));
        if (type.toLowerCase().contains("double"))
            getConfig().set(option, Double.valueOf(value));
        else
            getConfig().set(option, value);
    }

    public ConfigurationSection getConfig() {
        if (section == null)
            return getDefaultConfig();
        return section;
    }

    public ConfigurationSection getDefaultConfig() {
        try {
            ConfigurationSection section = new YamlConfiguration();

            for (Field f : getClass().getDeclaredFields()) {
                f.setAccessible(true);
                section.addDefault(fieldNameToConfigOption(f.getName()), f.get(this));
                section.set(fieldNameToConfigOption(f.getName()), f.get(this));
            }

            return section;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String fieldNameToConfigOption(String string) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            Character c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(" ");
                sb.append(Character.toLowerCase(c));
            }
            else
                sb.append(c);
        }
        return sb.toString();
    }

}
