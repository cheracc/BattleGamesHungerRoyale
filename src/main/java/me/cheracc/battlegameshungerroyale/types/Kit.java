package me.cheracc.battlegameshungerroyale.types;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Kit {
    private final ConfigurationSection config;
    private final String               id;
    private final List<Ability>        abilities = new ArrayList<>();
    private       boolean              enabled;
    private       String               name;
    private       String               description;
    private       String               iconItemType;
    private EquipmentSet equipment;

    public Kit(String key, ConfigurationSection config, KitManager kitManager) {
        this.id = key;
        this.config = config;
        this.equipment = EquipmentSet.newEquipmentSet();
        String equipmentBase64 = config.getString("equipment", "");
        if (!equipmentBase64.equals(""))
            equipment.loadItemsFromBase64(equipmentBase64);
        name = config.getString("name", "Nameless Kit");
        description = config.getString("description", "Give this kit a description");
        iconItemType = config.getString("icon", "chest").toUpperCase();
        if (config.getConfigurationSection("abilities") != null)
            loadAbilities(config.getConfigurationSection("abilities"), kitManager);
        enabled = config.getBoolean("enabled", true);
    }

    public Kit(String id) {
        this.id = id;
        config = new YamlConfiguration();
        setName("Some New Kit");
        setDescription("Can't be sure what it does because nobody ever changed the default description!");
        this.equipment = EquipmentSet.newEquipmentSet();
        setIcon(Material.STONE);
        enabled = false;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
        config.set("enabled", value);
    }

    public void removeAbility(Ability ability) {
        if (!abilities.contains(ability)) {
            return;
        }
        abilities.remove(ability);
    }

    public EquipmentSet getEquipment() {
        return equipment;
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

    public String getName() {
        return name;
    }

    public void setEquipment(EquipmentSet equipment) {
        this.equipment = equipment;
    }

    public void saveConfig(BGHR plugin) {
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

        if (equipment.isNotEmpty())
            this.config.set("equipment", equipment.serializeAsBase64());

        FileConfiguration kitsConfig = new YamlConfiguration();

        try {
            kitsConfig.load(configFile);
            kitsConfig.set(id, this.config);
            kitsConfig.save(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void loadAbilities(ConfigurationSection section, KitManager kitManager) {
        Set<String> keys = section.getKeys(false);

        for (String key : keys) {
            if (key != null) {
                for (Ability a : kitManager.getDefaultAbilities()) {
                    if (key.startsWith(a.getName())) {
                        Ability ability = a.newWithDefaults();
                        ability.loadFromConfig(section.getConfigurationSection(key));
                        addAbility(ability);
                    }
                }
            }
        }
    }

    public List<PlayerData> getMyPlayers() {
        PlayerManager playerManager = JavaPlugin.getPlugin(BGHR.class).getApi().getPlayerManager();
        List<PlayerData> players = playerManager.getLoadedPlayers();
        players.removeIf(data -> data.getKit() == null || !data.getKit().equals(this));

        return players;
    }
}
