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
    private static final Map<String, Map<String, Map<String, String>>> translatables = new HashMap<>();
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
            Map<String, Map<String, String>> classMap = new HashMap<>();
            for (String methodKey : classSection.getKeys(false)) {
                ConfigurationSection methodSection = config.getConfigurationSection(methodKey);
                Map<String, String> methodMap = new HashMap<>();
                for (String stringKey : methodSection.getKeys(false))
                    methodMap.put(stringKey, methodSection.getString(stringKey));
                classMap.put(methodKey, methodMap);
            }
            translatables.put(classKey, classMap);
        }
    }

    private static void saveFile() {
        File file = new File(plugin.getDataFolder(), "translations.yml");
        FileConfiguration config = new YamlConfiguration();

        try {
            if (file.exists())
                config.load(file);

            for (String className : translatables.keySet()) {
                Map<String, Map<String, String>> classMap = translatables.get(className);
                for (String methodName : classMap.keySet()) {
                    config.createSection(className + "." + methodName, classMap.get(methodName));
                }
            }
            config.save(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static String late(String string) {
        String callingClass = getCaller()[0];
        String callingMethod = getCaller()[1];

        if (translatables.containsKey(callingClass)) {
            Map<String, Map<String, String>> classMap = translatables.get(callingClass);
            if (classMap.containsKey(callingMethod)) {
                Map<String, String> methodMap = classMap.get(callingMethod);
                if (methodMap.containsKey(string))
                    return methodMap.get(string);
                else
                    methodMap.put(string, string);
            }
            else {
                Map<String, String> newMethodMap = new HashMap<>();
                newMethodMap.put(string, string);
                classMap.put(callingMethod, newMethodMap);
                translatables.put(callingClass, classMap);
                saveFile();
            }
        }
        else {
            Map<String, Map<String, String>> newClassMap = new HashMap<>();
            newClassMap.put(callingMethod, new HashMap<>());
            translatables.put(callingClass, newClassMap);
            saveFile();
        }
        return string;
    }

    private static String[] getCaller() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        int i = 1;

        for (; i < elements.length; i++) {
            StackTraceElement e = elements[i];
            if (!e.getClassName().equals(Trans.class.getName()))
                break;
        }
        String[] fullClassName = elements[i].getClassName().split("\\.");
        return new String[] {fullClassName[fullClassName.length - 1], elements[i].getMethodName()};
    }
}
