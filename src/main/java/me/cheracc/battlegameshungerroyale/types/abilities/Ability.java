package me.cheracc.battlegameshungerroyale.types.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class Ability implements Cloneable {
    private ConfigurationSection section;
    private UUID id;
    private Kit forKit = null;
    protected String description = "";
    protected String customName = null;
    protected SoundEffect sound = null;
    protected BGHR plugin = null;

    public Ability() {
        this.section = null;
        this.id = UUID.randomUUID();
        try {
            ConfigurationSection section = new YamlConfiguration();

            for (Field f : getClass().getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType()) || List.class.isAssignableFrom(f.getType()))
                    continue;
                f.setAccessible(true);
                section.addDefault(fieldNameToConfigOption(f.getName()), f.get(this));
                section.set(fieldNameToConfigOption(f.getName()), f.get(this));
            }
            this.section = section;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void initialize(BGHR plugin) {
        this.plugin = plugin;
    }

    public boolean hasMyAbility(Player player) {
        PlayerData data = PlayerManager.getInstance().getPlayerData(player);
        return data.getKit() != null && data.getKit().equals(getAssignedKit());
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
                Field f = null;
                try {
                    f = this.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(this, value);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    try {
                        f = this.getClass().getSuperclass().getDeclaredField(fieldName);
                        f.setAccessible(true);
                        f.set(this, value);
                    } catch (NoSuchFieldException | IllegalAccessException ex) {
                        try {
                            f = this.getClass().getSuperclass().getSuperclass().getDeclaredField(fieldName);
                            f.setAccessible(true);
                            f.set(this, value);
                        } catch (NoSuchFieldException | IllegalAccessException exc) {
                            Bukkit.getLogger().warning("no field found for config option. ability:" + getName() + " field:" + fieldName + " option:" + key);
                        }
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

    public SoundEffect getSound() {
        return sound;
    }

    public void setSound(SoundEffect sound) {
        this.sound = sound;
        getConfig().set("sound", sound);
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
        if (customName == null)
            return getName();
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
        if (this instanceof Listener) {
            Bukkit.getPluginManager().registerEvents((Listener) this, plugin);
        }
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

        lore.add(Component.text(""));
        lore.addAll(Tools.componentalize(Tools.wrapText(description, ChatColor.GRAY)));
        lore.add(Component.text(""));
        if (cooldown > 0)
            lore.add(Tools.componentalize("&7Cooldown: &f" + Tools.secondsToMinutesAndSeconds(cooldown)));

        meta.lore(lore);
        abilityItem.setItemMeta(meta);
        abilityItem.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        Tools.tagAsPluginItem(abilityItem);
        tagAbilityItem(abilityItem);

        return abilityItem;
    }

    public ConfigurationSection getConfig() {
        return section;
    }

    private String fieldNameToConfigOption(String string) {
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

    public void tagAbilityItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null)
            return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ABILITY_KEY, PersistentDataType.STRING, id.toString());
        item.setItemMeta(meta);
    }

    // static fields and methods
    private static final NamespacedKey ABILITY_KEY = new NamespacedKey(BGHR.getPlugin(), "ability_item");

    public static Ability getFromItem(ItemStack item) {
        if (!isAbilityItem(item))
            return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(ABILITY_KEY, PersistentDataType.STRING);
        for (Ability a : KitManager.getInstance().getAllAbilitiesInUse()) {
            if (a.getId().toString().equals(id))
                return a;
        }
        Bukkit.getLogger().warning("couldn't find ability with id " + id);
        return null;
    }

    public static boolean isAbilityItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null)
            return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return !pdc.isEmpty() && pdc.has(ABILITY_KEY, PersistentDataType.STRING);
    }


}
