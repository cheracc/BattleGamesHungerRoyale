package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class KitManager {
    private final BGHR plugin;
    private final Logr logr;
    private final List<Kit> loadedKits = new ArrayList<>();
    private final List<Ability> defaultAbilities = new ArrayList<>();

    public KitManager(BGHR plugin, Logr logr) {
        this.plugin = plugin;
        this.logr = logr;
        findAndLoadDefaultAbilities();
        loadKits();
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

    public void replaceKit(Kit kit) {
        List<Kit> toRemove = new ArrayList<>();
        for (Kit k : getLoadedKits()) {
            if (kit.getId().equalsIgnoreCase(k.getId()) || kit.getName().equals(k.getName()))
                toRemove.add(k);
        }
        for (Kit k : toRemove) {
            k.getMyPlayers().forEach(data -> {
                data.assignKit(kit, false);
                plugin.getApi().getPlayerManager().outfitPlayer(data.getPlayer(), kit);
            });
            loadedKits.remove(k);
        }
        loadedKits.add(kit);
        kit.saveConfig(plugin);
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
                Kit kit = new Kit(s, config.getConfigurationSection(s), this);

                loadedKits.add(kit);
            }
        }
        logr.info("Loaded %s kits and %s abilities", loadedKits.size(), defaultAbilities.size());
    }

    public Ability getAbilityFromItem(ItemStack item) {
        if (!isAbilityItem(item))
            return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(BghrApi.ABILITY_KEY, PersistentDataType.STRING);
        for (Ability a : getAllAbilitiesInUse()) {
            if (a.getId().toString().equals(id))
                return a;
        }
        logr.warn("couldn't find ability with id %s", id);
        return null;
    }

    public boolean isAbilityItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null)
            return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return !pdc.isEmpty() && pdc.has(BghrApi.ABILITY_KEY, PersistentDataType.STRING);
    }

    public List<Ability> getDefaultAbilities() {
        return new ArrayList<>(defaultAbilities);
    }

    private void findAndLoadDefaultAbilities() {
        Set<Class<?>> abilityClasses = new HashSet<>((new Reflections("me.cheracc.battlegameshungerroyale.abilities", new SubTypesScanner(false))).getSubTypesOf(Ability.class));

        for (Class<?> c : abilityClasses) {
            if (c == null || Modifier.isAbstract(c.getModifiers()))
                continue;
            try {
                Constructor<?> con = c.getDeclaredConstructor();
                Object o = con.newInstance();
                if (o instanceof Ability) {
                    Ability ability = (Ability) o;
                    ability.initialize(plugin);
                    defaultAbilities.add(ability);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
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
