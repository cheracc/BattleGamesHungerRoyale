package me.stipe.battlegameshungerroyale;

import me.stipe.battlegameshungerroyale.commands.MapConfig;
import me.stipe.battlegameshungerroyale.commands.Maps;
import me.stipe.battlegameshungerroyale.commands.SaveMap;
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
    private MapManager mapManager;
    private KitManager kitManager;
    private PlayerManager playerManager;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        mainConfig = getConfig();
        mapManager = new MapManager();
        kitManager = new KitManager();
        playerManager = new PlayerManager();

        this.getCommand("maps").setExecutor(new Maps());
        this.getCommand("savemap").setExecutor(new SaveMap());
        this.getCommand("mapconfig").setExecutor(new MapConfig());
        Bukkit.getPluginManager().registerEvents(new GeneralPlayerEventListener(), this);
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

    public MapManager getMapManager() {
        return mapManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }
}
