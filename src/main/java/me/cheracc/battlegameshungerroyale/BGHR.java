package me.cheracc.battlegameshungerroyale;

import me.cheracc.battlegameshungerroyale.commands.*;
import me.cheracc.battlegameshungerroyale.datatypes.EquipmentSet;
import me.cheracc.battlegameshungerroyale.datatypes.SoundEffect;
import me.cheracc.battlegameshungerroyale.events.CustomEventsListener;
import me.cheracc.battlegameshungerroyale.guis.TextInputListener;
import me.cheracc.battlegameshungerroyale.listeners.AbilityListeners;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

public class BGHR extends JavaPlugin {
    private FileConfiguration mainConfig;
    private static BGHR plugin;
    private static KitManager kitManager;
    private static PlayerManager playerManager;
    private static Permission perms = null;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        mainConfig = getConfig();
        MapManager.getInstance();
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

        this.getCommand("savemap").setExecutor(new SaveMap());
        this.getCommand("mapconfig").setExecutor(new MapConfig());
        this.getCommand("kit").setExecutor(new KitCommand());
        this.getCommand("kitmenu").setExecutor(new KitMenu());
        this.getCommand("kitconfig").setExecutor(new KitConfig());
        this.getCommand("newgame").setExecutor(new NewGameCommand());
        this.getCommand("games").setExecutor(new GamesCommand());
        this.getCommand("quit").setExecutor(new QuitCommand());
        Bukkit.getPluginManager().registerEvents(new AbilityListeners(), this);
        Bukkit.getPluginManager().registerEvents(TextInputListener.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(CustomEventsListener.getInstance(), this);
        setupPermissions();
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    private static void setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
    }

    public static Permission getPerms() {
        if (perms == null)
            setupPermissions();
        return perms;
    }

    public static BGHR getPlugin() {
        return plugin;
    }

    public FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public static KitManager getKitManager() {
        return kitManager;
    }

    public static PlayerManager getPlayerManager() {
        return playerManager;
    }

}
