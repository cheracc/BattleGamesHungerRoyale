package me.stipe.battlegameshungerroyale.datatypes.abilities;

import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Ability implements Cloneable {
    private ConfigurationSection section;
    private UUID id;
    private Kit forKit = null;
    String description = "";
    String customName = null;

    public Ability() {
        this.section = null;
        this.id = UUID.randomUUID();
        try {
            ConfigurationSection section = new YamlConfiguration();

            for (Field f : getClass().getDeclaredFields()) {
                f.setAccessible(true);
                    section.addDefault(fieldNameToConfigOption(f.getName()), f.get(this));
                    section.set(fieldNameToConfigOption(f.getName()), f.get(this));
            }
            this.section = section;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void generateConfigSection() {
        try {
            ConfigurationSection section = new YamlConfiguration();

            for (Field f : getClass().getDeclaredFields()) {
                f.setAccessible(true);
                section.addDefault(fieldNameToConfigOption(f.getName()), f.get(this));
                section.set(fieldNameToConfigOption(f.getName()), f.get(this));
            }
            this.section = section;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void loadFromConfig(ConfigurationSection section) {
        this.section = section;
        this.id = UUID.randomUUID();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String fieldName = Tools.configOptionToFieldName(key);
                Object value = section.get(key);
                try {
                    Field f = this.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(this, value);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    try {
                        Field f = this.getClass().getSuperclass().getDeclaredField(fieldName);
                        f.setAccessible(true);
                        f.set(this, value);
                    } catch (NoSuchFieldException | IllegalAccessException ex) {
                        Bukkit.getLogger().warning("no field found for config option. ability:" + getName() + " field:" + fieldName + " option:" + key);
                    }
                }
            }
        }
    }

    public Ability newWithDefaults() {
        try {
            Ability ability = (Ability) this.clone();
            ability.generateConfigSection();
            ability.newId();
            return ability;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public UUID getId() {
        return id;
    }

    public void newId() {
        id = UUID.randomUUID();
    }

    // writes an option to the stored config
    public void setOption(String configOption, Object value) {
        getConfig().set(configOption, value);
        loadFromConfig(getConfig());
    }

    // gets the custom name or the default name
    public String getName() {
        return getClass().getSimpleName();
    }

    public String getCustomName() {
        return customName;
    }

    // sets a custom name and saves the default class name
    public void setName(String name) {
            customName = name;
            getConfig().set("custom name", customName);
    }

    public String getDescription() {
        return description;
    }

    // sets the description field and config values
    public void setDescription(String text) {
        description = text;
        getConfig().set("description", description);
    }

    public Kit getAssignedKit() {
        return forKit;
    }

    public void setAssignedKit(Kit kit) {
        forKit = kit;
    }

    public boolean isActive() {
        return this instanceof ActiveAbility;
    }

    public boolean isPassive() {
        return this instanceof PassiveAbility;
    }

    public ItemStack makeItem(Material type, String name, String description, int cooldown) {
        ItemStack abilityItem = new ItemStack(type);
        ItemMeta meta = abilityItem.getItemMeta();
        List<Component> lore = new ArrayList<>();

        if (meta == null)
            return abilityItem;

        meta.displayName(Component.text(ChatColor.WHITE + name));
        Tools.saveUuidToItemMeta(getId(), meta);

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

    public ConfigurationSection getConfig() {
        return section;
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
