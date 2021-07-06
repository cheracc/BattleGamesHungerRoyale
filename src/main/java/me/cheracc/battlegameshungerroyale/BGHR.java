package me.cheracc.battlegameshungerroyale;

import me.cheracc.battlegameshungerroyale.commands.*;
import me.cheracc.battlegameshungerroyale.datatypes.SoundEffect;
import me.cheracc.battlegameshungerroyale.events.CustomEventsListener;
import me.cheracc.battlegameshungerroyale.guis.TextInputListener;
import me.cheracc.battlegameshungerroyale.listeners.GeneralListeners;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.LootManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;

public class BGHR extends JavaPlugin implements Listener {
    private static BGHR plugin;
    private static Permission perms = null;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        MapManager.getInstance();
        ConfigurationSerialization.registerClass(SoundEffect.class);
        try {
            KitManager.getInstance().findAndLoadDefaultAbilities();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            System.out.println(e.getCause().toString());
        }
        KitManager.getInstance().loadKits();
        LootManager.getLootTables();
        GeneralListeners.start();

        getCommand("savemap").setExecutor(new SaveMap());
        getCommand("mapconfig").setExecutor(new MapConfig());
        getCommand("kit").setExecutor(new KitCommand());
        getCommand("kitmenu").setExecutor(new KitMenu());
        getCommand("kitconfig").setExecutor(new KitConfig());
        getCommand("games").setExecutor(new GamesCommand());
        getCommand("quit").setExecutor(new QuitCommand());
        getCommand("bghr").setExecutor(new BghrCommand());
        Bukkit.getPluginManager().registerEvents(TextInputListener.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(CustomEventsListener.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(this, this);
        setupPermissions();
    }

    @EventHandler
    public void runDelayedTasks(ServerLoadEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                GameManager.getInstance();
            }
        }.runTaskLater(this, 10L);
        if (MapManager.getInstance().wasDatapackUpdated())
            Bukkit.reloadData();
    }

    @Override
    public void onDisable() {
        saveConfig();
        HandlerList.unregisterAll((Plugin) this);
        ConfigurationSerialization.unregisterClass(SoundEffect.class);
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

}
