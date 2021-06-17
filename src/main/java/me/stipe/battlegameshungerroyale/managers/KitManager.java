package me.stipe.battlegameshungerroyale.managers;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KitManager {
    private final BGHR plugin = BGHR.getPlugin();
    private final List<Kit> loadedKits = new ArrayList<>();
    private final List<Ability> loadedAbilities = new ArrayList<>();

    public List<Kit> getLoadedKits() {
        return new ArrayList<>(loadedKits);
    }

    public Kit getKit(String name) {
        for (Kit k : loadedKits) {
            if (k.getName().equalsIgnoreCase(name))
                return k;
        }
        return null;
    }

    public Ability getGenericAbility(String name) {
        if (name == null || name.equals(""))
            return null;
        try {
            Class<?> c = Class.forName("me.stipe.battlegameshungerroyale.abilities." + name);
            Constructor<?> con = c.getDeclaredConstructor();
            return (Ability) con.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void registerAbility(Ability ability) {
        loadedAbilities.add(ability);
    }

    public void loadKits() {
        File configFile = new File(plugin.getDataFolder(), "kits.yml");
        FileConfiguration config = new YamlConfiguration();

        if (!configFile.exists())
            plugin.saveResource("kits.yml", false);

        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        for (String s : config.getKeys(false)) {
            if (s != null && config.getConfigurationSection(s) != null) {
                Kit kit = new Kit(s, Objects.requireNonNull(config.getConfigurationSection(s)));

                loadedKits.add(kit);
            }
        }
    }
}
