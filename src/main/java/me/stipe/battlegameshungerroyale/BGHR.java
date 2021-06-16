package me.stipe.battlegameshungerroyale;

import me.stipe.battlegameshungerroyale.commands.*;
import me.stipe.battlegameshungerroyale.listeners.AbilityListeners;
import me.stipe.battlegameshungerroyale.listeners.GeneralPlayerEventListener;
import me.stipe.battlegameshungerroyale.managers.KitManager;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.managers.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BGHR extends JavaPlugin {
    private FileConfiguration mainConfig = new YamlConfiguration();
    private static BGHR plugin;
    private static MapManager mapManager;
    private static KitManager kitManager;
    private static PlayerManager playerManager;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        mainConfig = getConfig();
        mapManager = new MapManager();
        kitManager = new KitManager();
        kitManager.loadKits();
        playerManager = new PlayerManager();

        this.getCommand("maps").setExecutor(new Maps());
        this.getCommand("savemap").setExecutor(new SaveMap());
        this.getCommand("mapconfig").setExecutor(new MapConfig());
        this.getCommand("kit").setExecutor(new KitCommand());
        this.getCommand("kitmenu").setExecutor(new KitMenu());
        Bukkit.getPluginManager().registerEvents(new GeneralPlayerEventListener(), this);
        Bukkit.getPluginManager().registerEvents(new AbilityListeners(), this);
    }

    @Override
    public void onDisable() {
        saveConfig();
        mapManager.unloadAllMaps();
    }

    public static BGHR getPlugin() {
        return plugin;
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public static MapManager getMapManager() {
        return mapManager;
    }

    public static KitManager getKitManager() {
        return kitManager;
    }

    public static PlayerManager getPlayerManager() {
        return playerManager;
    }
}
