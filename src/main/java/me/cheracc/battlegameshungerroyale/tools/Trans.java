package me.cheracc.battlegameshungerroyale.tools;

import me.cheracc.battlegameshungerroyale.BGHR;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Trans {
    private static FileConfiguration config;
    private static BGHR plugin;

    public static void load(BGHR plugin) {
        Trans.plugin = plugin;
        File file = new File(plugin.getDataFolder(), "translations.yml");
        config = new YamlConfiguration();

        try {
            if (file.exists())
                config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }


    }

    private static void saveFile() {
        File file = new File(plugin.getDataFolder(), "translations.yml");

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String late(String string) {
        if (string == null || string.isEmpty() || string.equals(""))
            return string;
        int stringAsKey = string.hashCode();
        String callingClass = getCaller()[0];
        String callingMethod = getCaller()[1];

        if (config.contains(callingClass + "." + callingMethod + "." + stringAsKey))
            return config.getString(callingClass + "." + callingMethod + "." + stringAsKey);
        else {
            config.set(callingClass + "." + callingMethod + "." + stringAsKey, string);
        }
        saveFile();
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
