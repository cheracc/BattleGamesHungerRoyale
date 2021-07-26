package me.cheracc.battlegameshungerroyale.tools;

import me.cheracc.battlegameshungerroyale.BGHR;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Trans {
    private static final Map<String, Map<String, String>> translatables = new HashMap<>();
    private static BGHR plugin;

    public static void load(BGHR plugin) {
        Trans.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "translations.yml");
        FileConfiguration config = new YamlConfiguration();

        try {
            if (file.exists())
                config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        for (String classKey : config.getKeys(false)) {
            ConfigurationSection classSection = config.getConfigurationSection(classKey);
            Map<String, String> classMap = new HashMap<>();
            for (String methodKey : classSection.getKeys(false)) {
                classMap.put(methodKey, classMap.get(methodKey));
            }
        }
    }

    private static void saveFile() {
        File file = new File(plugin.getDataFolder(), "translations.yml");
        FileConfiguration config = new YamlConfiguration();

        try {
            if (file.exists())
                config.load(file);

            for (String className : translatables.keySet()) {
                config.createSection(className, translatables.get(className));
            }
            config.save(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static String late(String string) {
        String callingClass = getCallingClassName();
        String callingMethod = getCallingMethodName();

        if (translatables.containsKey(callingClass)) {
            Map<String, String> classStrings = translatables.get(callingClass);
            if (classStrings.containsKey(callingMethod))
                return classStrings.get(callingMethod);
            else {
                classStrings.put(callingMethod, string);
                translatables.put(callingClass, classStrings);
                saveFile();
            }
        }
        else {
            Map<String, String> newMap = new HashMap<>();
            newMap.put(callingMethod, string);
            translatables.put(callingClass, newMap);
            saveFile();
        }
        return string;
    }

    private static String getCallingClassName() {
        String[] fullClassName = Thread.currentThread().getStackTrace()[2].getClassName().split("\\.");
        return fullClassName[fullClassName.length - 1];
    }

    private static String getCallingMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }
}