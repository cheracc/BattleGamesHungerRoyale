package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class KitManager {
    private static KitManager singletonInstance = null;
    private static BGHR plugin = null;
    private final List<Kit> loadedKits = new ArrayList<>();
    private final List<Ability> defaultAbilities = new ArrayList<>();

    private KitManager() {

    }

    public Set<Ability> getAllAbilitiesInUse() {
        Set<Ability> abilities = new HashSet<>();
        for (Kit kit : loadedKits) {
            abilities.addAll(kit.getAbilities());
        }
        return abilities;
    }

    public List<Kit> getLoadedKits() {
        return getLoadedKits(false);
    }

    public List<Kit> getLoadedKits(boolean includeDisabled) {
        if (includeDisabled)
            return new ArrayList<>(loadedKits);
        List<Kit> enabledKits = new ArrayList<>(loadedKits);
        enabledKits.removeIf(kit -> !kit.isEnabled());
        return enabledKits;
    }

    public Kit getKit(String name) {
        for (Kit k : loadedKits) {
            if (k.getName().equalsIgnoreCase(name))
                return k;
        }
        return null;
    }

    // TODO make this super clean - check for players with the kit and update them as well, make sure all items match etc
    public void replaceKit(Kit kit) {
        List<Kit> toRemove = new ArrayList<>();
        for (Kit k : getLoadedKits()) {
            if (kit.getId().equalsIgnoreCase(k.getId()))
                toRemove.add(k);
        }
        for (Kit k : toRemove) {
            loadedKits.remove(k);
        }
        loadedKits.add(kit);
        kit.saveConfig();
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

    public List<Ability> getDefaultAbilities() {
        return new ArrayList<>(defaultAbilities);
    }

    public void findAndLoadDefaultAbilities(BGHR plugin) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Set<Class<?>> abilityClasses = new HashSet<>((new Reflections("me.cheracc.battlegameshungerroyale.abilities", new SubTypesScanner(false))).getSubTypesOf(Ability.class));

        for (Class<?> c : abilityClasses) {
            if (c == null || Modifier.isAbstract(c.getModifiers()))
                continue;
            Constructor<?> con = c.getDeclaredConstructor();
            Object o = con.newInstance();
            if (o instanceof Ability) {
                Ability ability = (Ability) o;
                ability.initialize(plugin);
                defaultAbilities.add(ability);
            }
        }
    }

    public static void initialize(BGHR plugin) {
        KitManager.plugin = plugin;
    }

    public static KitManager getInstance() {
        if (singletonInstance == null)
            singletonInstance = new KitManager();
        return singletonInstance;
    }

    public Kit getRandomKit(boolean includeDisabled) {
        int random = ThreadLocalRandom.current().nextInt(loadedKits.size());
        int safetyCounter = 0;
        while (!(includeDisabled || loadedKits.get(random).isEnabled()) && safetyCounter < 100) {
            random = ThreadLocalRandom.current().nextInt(loadedKits.size());
        }
        return loadedKits.get(random);
    }
}
