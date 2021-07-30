package me.cheracc.battlegameshungerroyale;
import me.cheracc.battlegameshungerroyale.commands.*;
import me.cheracc.battlegameshungerroyale.events.CustomEventsListener;
import me.cheracc.battlegameshungerroyale.listeners.GeneralListeners;
import me.cheracc.battlegameshungerroyale.listeners.StatsListeners;
import me.cheracc.battlegameshungerroyale.listeners.TextInputListener;
import me.cheracc.battlegameshungerroyale.managers.*;
import me.cheracc.battlegameshungerroyale.tools.PluginUpdater;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.SoundEffect;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.*;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;

public class BghrApi implements Listener {
    public final static String HOLOGRAM_TAG = "hologram";
    public final static String HOLOGRAM_ID_TAG = "hologram_id";
    public final static String HOLOGRAM_CLICKABLE = "clickable_hologram";
    public static NamespacedKey EQUIPMENT_KEY;
    public static NamespacedKey ABILITY_KEY;
    public static NamespacedKey HOLOGRAM_TEXT_KEY;
    public static NamespacedKey HOLOGRAM_COMMAND_KEY;
    private final BGHR plugin;
    private final Logr logr;
    private final DatabaseManager databaseManager;
    private final KitManager kitManager;
    private final MapManager mapManager;
    private final DisplayManager displayManager;
    private GameManager gameManager;
    private PlayerManager playerManager;
    private PluginUpdater updater;
    private TextInputListener textInputListener;

    public BghrApi(BGHR plugin) {
        this.plugin = plugin;
        logr = new Logr(plugin);
        loadKeys();
        Trans.load(plugin);
        displayManager = new DisplayManager(plugin, logr);
        mapManager = new MapManager(plugin, logr);
        registerSerializers();
        kitManager = new KitManager(plugin, logr);
        databaseManager = new DatabaseManager(plugin, logr);
        if (plugin.getConfig().getBoolean("auto-update", true))
            updater = new PluginUpdater(plugin);

        // these listeners are needed early for loading
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadKeys() {
        EQUIPMENT_KEY = new NamespacedKey(plugin, "equipment");
        ABILITY_KEY = new NamespacedKey(plugin, "ability_item");
        HOLOGRAM_TEXT_KEY = new NamespacedKey(plugin, "hologram");
        HOLOGRAM_COMMAND_KEY = new NamespacedKey(plugin, "hologram_command");
    }

    private void registerSerializers() {
        ConfigurationSerialization.registerClass(DualWieldBonusType.class);
        ConfigurationSerialization.registerClass(SoundEffect.class);
        ConfigurationSerialization.registerClass(TotemAttackType.class);
        ConfigurationSerialization.registerClass(TotemType.class);
        ConfigurationSerialization.registerClass(UpgradeType.class);
        ConfigurationSerialization.registerClass(AbilityTrigger.class);
        ConfigurationSerialization.registerClass(EnchantWrapper.class);
        ConfigurationSerialization.registerClass(RemoteAbility.class);
    }

    @EventHandler
    public void runDelayedTasks(ServerLoadEvent event) {
        gameManager = new GameManager(plugin, logr, mapManager);
        playerManager = new PlayerManager(plugin, kitManager, gameManager, databaseManager, logr);
        displayManager.loadFromConfig();
        registerCommands();
        registerListeners();
        if (mapManager.wasDatapackUpdated())
            Bukkit.reloadData();
    }

    private void registerCommands() {
        plugin.getCommand("savemap").setExecutor(new SaveMap(mapManager, plugin));
        plugin.getCommand("mapconfig").setExecutor(new MapConfig(mapManager, textInputListener));
        plugin.getCommand("kit").setExecutor(new KitCommand(this));
        plugin.getCommand("kitmenu").setExecutor(new KitMenu(this));
        plugin.getCommand("games").setExecutor(new GamesCommand(this));
        plugin.getCommand("quit").setExecutor(new QuitCommand(gameManager, mapManager));
        plugin.getCommand("bghr").setExecutor(new BghrCommand(this));
        plugin.getCommand("settings").setExecutor(new SettingsCommand(this));
        plugin.getCommand("abilityitem").setExecutor(new AbilityItemCommand(this));
        plugin.getCommand("stats").setExecutor(new StatsCommand(playerManager, plugin));
        plugin.getCommand("topstats").setExecutor(new TopStatsCommand(this));
        plugin.getCommand("vote").setExecutor(new VoteCommand(this));
        plugin.getCommand("join").setExecutor(new JoinCommand(gameManager));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GeneralListeners(this), plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(new GeneralListeners(this), plugin);
        Bukkit.getPluginManager().registerEvents(new CustomEventsListener(gameManager), plugin);
        Bukkit.getPluginManager().registerEvents(new StatsListeners(gameManager, playerManager), plugin);
        Bukkit.getPluginManager().registerEvents(textInputListener = new TextInputListener(plugin), plugin);
    }

    public void shutdown() {
        for (Game game : gameManager.getActiveGames()) {
            game.endGame();
        }
        plugin.saveConfig();
        HandlerList.unregisterAll((Plugin) plugin);
        unregisterSerializers();
        gameManager.stopUpdater();
        playerManager.disable();
        if (updater != null)
            updater.disable();
    }

    private void unregisterSerializers() {
        ConfigurationSerialization.unregisterClass(DualWieldBonusType.class);
        ConfigurationSerialization.unregisterClass(SoundEffect.class);
        ConfigurationSerialization.unregisterClass(TotemAttackType.class);
        ConfigurationSerialization.unregisterClass(TotemType.class);
        ConfigurationSerialization.unregisterClass(UpgradeType.class);
        ConfigurationSerialization.unregisterClass(AbilityTrigger.class);
        ConfigurationSerialization.unregisterClass(EnchantWrapper.class);
        ConfigurationSerialization.unregisterClass(RemoteAbility.class);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public BGHR getPlugin() {
        return plugin;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public TextInputListener getTextInputListener() {
        return textInputListener;
    }

    public Logr logr() {
        return logr;
    }

    // utility methods
    public String replacePlaceholders(String string) {
        return displayManager.replacePlaceholders(string);
    }
}
