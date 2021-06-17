package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.stipe.battlegameshungerroyale.datatypes.abilities.PassiveAbility;
import me.stipe.battlegameshungerroyale.managers.KitManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Kit {
    ConfigurationSection config;
    String id;
    String name;
    String description;
    Material icon;
    List<Ability> abilities = new ArrayList<>();

    public Kit(String key, ConfigurationSection config) {
        this.id = key;
        this.config = config;
        name = config.getString("name", "");
        description = config.getString("description", "");
        icon = Material.valueOf(config.getString("icon", "chest").toUpperCase());
        if (config.contains("abilities"))
            loadAbilities(Objects.requireNonNull(config.getConfigurationSection("abilities")));
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
        this.icon = material;
        config.set("icon", material.name().toLowerCase());
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
    }

    public Material getIcon() {
        return icon;
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
                Ability ability = kits.getGenericAbility(key);
                if (ability != null) {
                    ability.load(section.getConfigurationSection(key), this);
                    abilities.add(ability);
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
