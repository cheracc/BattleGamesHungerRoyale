package me.stipe.battlegameshungerroyale;

import me.stipe.battlegameshungerroyale.commands.*;
import me.stipe.battlegameshungerroyale.datatypes.EquipmentSet;
import me.stipe.battlegameshungerroyale.datatypes.SoundEffect;
import me.stipe.battlegameshungerroyale.events.CustomEventsListener;
import me.stipe.battlegameshungerroyale.guis.TextInputListener;
import me.stipe.battlegameshungerroyale.listeners.AbilityListeners;
import me.stipe.battlegameshungerroyale.listeners.GeneralPlayerEventListener;
import me.stipe.battlegameshungerroyale.managers.KitManager;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.managers.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

public class BGHR extends JavaPlugin {
    private FileConfiguration mainConfig;
    private static BGHR plugin;
    private static MapManager mapManager;
    private static KitManager kitManager;
    private static PlayerManager playerManager;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        mainConfig = getConfig();
        mapManager = MapManager.getInstance();
        ConfigurationSerialization.registerClass(SoundEffect.class);
        ConfigurationSerialization.registerClass(EquipmentSet.class);
        kitManager = new KitManager();
        try {
            kitManager.findAndLoadDefaultAbilities();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            System.out.println(e.getCause().toString());
        }
        kitManager.loadKits();
        playerManager = new PlayerManager();

        this.getCommand("maps").setExecutor(new Maps());
        this.getCommand("savemap").setExecutor(new SaveMap());
        this.getCommand("mapconfig").setExecutor(new MapConfig());
        this.getCommand("kit").setExecutor(new KitCommand());
        this.getCommand("kitmenu").setExecutor(new KitMenu());
        this.getCommand("kitconfig").setExecutor(new KitConfig());
        this.getCommand("newgame").setExecutor(new NewGameCommand());
        this.getCommand("games").setExecutor(new GamesCommand());
        this.getCommand("quit").setExecutor(new QuitCommand());
        Bukkit.getPluginManager().registerEvents(new GeneralPlayerEventListener(), this);
        Bukkit.getPluginManager().registerEvents(new AbilityListeners(), this);
        Bukkit.getPluginManager().registerEvents(TextInputListener.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(CustomEventsListener.getInstance(), this);
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
