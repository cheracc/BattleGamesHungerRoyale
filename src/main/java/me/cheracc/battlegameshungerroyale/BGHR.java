package me.cheracc.battlegameshungerroyale;

import me.cheracc.battlegameshungerroyale.commands.*;
import me.cheracc.battlegameshungerroyale.events.CustomEventsListener;
import me.cheracc.battlegameshungerroyale.guis.TextInputListener;
import me.cheracc.battlegameshungerroyale.guis.TopStatsGui;
import me.cheracc.battlegameshungerroyale.listeners.GeneralListeners;
import me.cheracc.battlegameshungerroyale.listeners.StatsListeners;
import me.cheracc.battlegameshungerroyale.managers.*;
import me.cheracc.battlegameshungerroyale.tools.Logr;
import me.cheracc.battlegameshungerroyale.tools.PluginUpdater;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.SoundEffect;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.TotemAttackType;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

public class BGHR extends JavaPlugin implements Listener {
    private static BGHR plugin;
    private PluginUpdater updater;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        Logr.initialize(this);
        Metrics metrics = new Metrics(this, 12102);
        plugin = this;
        saveDefaultConfig();
        MapManager.getInstance();
        ConfigurationSerialization.registerClass(SoundEffect.class);
        ConfigurationSerialization.registerClass(TotemAttackType.class);
        try {
            KitManager.getInstance().findAndLoadDefaultAbilities();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            Logr.info(e.getCause().toString());
        }
        KitManager.getInstance().loadKits();
        LootManager.getLootTables();
        DatabaseManager.initialize(this);

        getCommand("savemap").setExecutor(new SaveMap());
        getCommand("mapconfig").setExecutor(new MapConfig());
        getCommand("kit").setExecutor(new KitCommand());
        getCommand("kitmenu").setExecutor(new KitMenu());
        getCommand("games").setExecutor(new GamesCommand());
        getCommand("quit").setExecutor(new QuitCommand());
        getCommand("bghr").setExecutor(new BghrCommand());
        getCommand("settings").setExecutor(new SettingsCommand());
        getCommand("abilityitem").setExecutor(new AbilityItemCommand());
        getCommand("stats").setExecutor(new StatsCommand());
        getCommand("topstats").setExecutor(new TopStatsCommand());
        getCommand("vote").setExecutor(new VoteCommand());
        Bukkit.getPluginManager().registerEvents(TextInputListener.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(CustomEventsListener.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(new GeneralListeners(this), this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void runDelayedTasks(ServerLoadEvent event) {
        PlayerManager.getInstance().initialize(this);
        TopStatsGui.initialize(this);
        if (MapManager.getInstance().wasDatapackUpdated())
            Bukkit.reloadData();
        GameManager.initialize(this);
        if (getConfig().getBoolean("auto-update", true))
            updater = new PluginUpdater(this);
        Bukkit.getPluginManager().registerEvents(new StatsListeners(), this);
    }

    @Override
    public void onDisable() {
        for (Game game : GameManager.getInstance().getActiveGames()) {
            game.endGame();
        }
        saveConfig();
        HandlerList.unregisterAll((Plugin) this);
        ConfigurationSerialization.unregisterClass(SoundEffect.class);
        GameManager.getInstance().stopUpdater();
        PlayerManager.getInstance().disable();
        if (updater != null)
            updater.disable();
    }

    public String getJarFilename() {
        return super.getFile().getName();
    }

    public static BGHR getPlugin() {
        return plugin;
    }

}
