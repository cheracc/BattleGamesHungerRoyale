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

public class KitManager {
    private final List<Kit> loadedKits = new ArrayList<>();
    private final List<Ability> genericAbilities = new ArrayList<>();

    public List<Ability> getGenericAbilities() {
        if (genericAbilities.isEmpty())
            loadGenericAbilities();
        return new ArrayList<>(genericAbilities);
    }

    public Kit getKit(String name) {
        for (Kit k : loadedKits) {
            if (k.getName().equalsIgnoreCase(name))
                return k;
        }
        return null;
    }

    private void loadGenericAbilities() {
        BGHR plugin = BGHR.getPlugin();
        File configFile = new File(plugin.getDataFolder(), "abilities.yml");

        if (!configFile.exists())
            plugin.saveResource("abilities.yml", false);

        FileConfiguration config = new YamlConfiguration();

        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        if (config.contains("abilities")) // TODO instead of this, use the key under abilities in kits.yml to look for the ability classes
            for (String s : config.getStringList("abilities")) {
                if (s == null || s.equals(""))
                    continue;
                try {
                    Class<?> c = Class.forName("me.stipe.battlegameshungerroyale.abilities." + s);
                    Constructor<?> con = c.getDeclaredConstructor();
                    Ability ability = (Ability) con.newInstance();
                    genericAbilities.add(ability);
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
    }

}
