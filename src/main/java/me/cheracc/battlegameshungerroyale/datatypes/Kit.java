package me.cheracc.battlegameshungerroyale.datatypes;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.PassiveAbility;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Kit implements Cloneable {
    ConfigurationSection config;
    String id;
    String name;
    String description;
    String iconItemType;
    List<Ability> abilities = new ArrayList<>();
    EquipmentSet equipment;

    public Kit(String key, ConfigurationSection config) {
        this.id = key;
        this.config = config;
        this.equipment = (EquipmentSet) config.get("equipment", new EquipmentSet());
        name = config.getString("name", "Nameless Kit");
        description = config.getString("description", "Give this kit a description");
        iconItemType = config.getString("icon", "chest").toUpperCase();
        if (config.contains("abilities"))
            loadAbilities(Objects.requireNonNull(config.getConfigurationSection("abilities")));
    }

    public Kit copyThis() {
        try {
            return (Kit) this.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Kit(String id) {
        this.id = id;
        config = new YamlConfiguration();
        setName("Some New Kit");
        setDescription("Can't be sure what it does because nobody ever changed the default description!");
        setIcon(Material.STONE);
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
        config.set("name", name);
    }

    public void setDescription(String description) {
        this.description = description;
        config.set("description", description);
    }

    public void setIcon(Material material) {
        this.iconItemType = material.name().toLowerCase();
        config.set("icon", material.name().toLowerCase());
    }

    public void addAbility(Ability ability) {
        ability.setAssignedKit(this);
        abilities.add(ability);
    }

    public void removeAbility(Ability ability) {
        if (!abilities.contains(ability)) {
            Bukkit.getLogger().warning("Tried to remove " + ability.getName() + " from kit " + getName() + " but it isn't there");
            return;
        }
        abilities.remove(ability);
        saveConfig();
    }

    public void setEquipment(EquipmentSet equipment) {
        this.equipment = equipment;
        config.set("equipment", this.equipment);
        saveConfig();
    }

    public EquipmentSet getEquipment() {
        return equipment;
    }

    public void disrobePlayer(PlayerData data) {

    }

    public void outfitPlayer(Player p, PlayerData data) {
        for (Ability a : abilities) {
            if (a instanceof ActiveAbility) {
                ItemStack abilityItem = ((ActiveAbility) a).createAbilityItem();
                p.getInventory().setItem(getLastEmptyHotbarSlot(p), abilityItem);
                data.registerAbilityItem(a, abilityItem);
            }
            if (a instanceof PassiveAbility) {
                if (((PassiveAbility) a).hasToggleItem()) {
                    ItemStack toggleItem = ((PassiveAbility) a).makeToggleItem();
                    p.getInventory().setItem(getLastEmptyHotbarSlot(p), toggleItem);
                    data.registerAbilityItem(a, toggleItem);
                } else {
                    ((PassiveAbility) a).activate(p);
                }
            }
        }
        if (equipment != null) {
            equipment.equip(p);
        }
    }

    public Material getIcon() {
        return Material.valueOf(iconItemType.toUpperCase());
    }

    public String getDescription() {
        return description;
    }

    public List<Ability> getAbilities() {
        return new ArrayList<>(abilities);
    }

    private int getLastEmptyHotbarSlot(Player p) {
        for (int i = 8; i >= 0; i--) {
            ItemStack item = p.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR)
                return i;
        }
        return -1;
    }

    private void loadAbilities(ConfigurationSection section) {
        KitManager kits = BGHR.getKitManager();
        Set<String> keys = section.getKeys(false);

        for (String key : keys) {
            if (key != null) {
                for (Ability a : kits.getDefaultAbilities()) {
                    if (key.startsWith(a.getName())) {
                        Ability ability = a.newWithDefaults();
                        ability.loadFromConfig(section.getConfigurationSection(key));
                        addAbility(ability);
                    }
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public void saveConfig() {
        BGHR plugin = BGHR.getPlugin();
        File configFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!configFile.exists())
            plugin.saveResource("kits.yml", false);

        config.set("abilities", null);

        for (Ability a : abilities) {
            if (this.config.get("abilities." + a.getName()) == null)
                this.config.set("abilities." + a.getName(), a.getConfig());
            else
                this.config.set("abilities." + a.getName() + "_" + ThreadLocalRandom.current().nextInt(99999999), a.getConfig());

        }

        if (equipment != null)
            this.config.set("equipment", equipment);

        FileConfiguration kitsConfig = new YamlConfiguration();

        try {
            kitsConfig.load(configFile);
            kitsConfig.set(id, this.config);
            kitsConfig.save(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

}
