package me.cheracc.battlegameshungerroyale.managers;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.Hologram;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public class DisplayManager implements Listener {
    private final static String CONFIG_FILE = "displays.yml";
    private final Map<String, Supplier<String>> placeholders = new HashMap<>();
    private final List<Hologram> holograms;
    private final List<HologramTemplate> holoTemplates;
    private final BGHR plugin;
    private final Logr logr;
    private final FileConfiguration config;
    private final boolean useHolograms;
    private BukkitTask updater;

    public DisplayManager(BGHR plugin, Logr logr) {
        this.plugin = plugin;
        this.logr = logr;
        config = new YamlConfiguration();
        holograms = new ArrayList<>();
        holoTemplates = new ArrayList<>();
        useHolograms = plugin.getConfig().getBoolean("enable holograms", true);
        updater = updater().runTaskTimer(plugin, 100L, 10L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private BukkitRunnable updater() {
        logr.debug("Starting Hologram Updater...");
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (holograms.isEmpty()) {
                    cancel();
                    logr.debug("No Holograms, stopping Updater...");
                    return;
                }
                updateHolograms();
            }
        };
    }

    private void updateHolograms() {
        holograms.forEach(Hologram::update);
    }

    public List<HologramTemplate> getHoloTemplates() {
        return new ArrayList<>(holoTemplates);
    }

    public void loadFromConfig() {
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);

        if (!file.exists())
            return;
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        ConfigurationSection templates = config.getConfigurationSection("templates");
        if (templates != null) {
            for (String s : templates.getKeys(false)) {
                if (s != null && templates.getConfigurationSection(s) != null)
                    holoTemplates.add(new HologramTemplate(templates.getConfigurationSection(s)));
            }
        }

        // get configsection for holograms
        ConfigurationSection section = config.getConfigurationSection("holograms");
        if (section != null && !section.getKeys(false).isEmpty()) {
            logr.debug("Loading %s displays from displays.yml", section.getKeys(false).size());
            // destroy/remove/clear current holos
            holograms.forEach(Hologram::remove);
            holograms.clear();

            // load holograms (location, text, command when clicked)
            for (String s : section.getKeys(false)) {
                ConfigurationSection holoSection = section.getConfigurationSection(s);
                if (holoSection == null)
                    continue;
                UUID uuid = UUID.fromString(s);
                logr.debug(uuid.toString());
                Location loc = (Location) holoSection.get("location");
                logr.debug(holoSection.get("location").toString());
                List<String> text = holoSection.getStringList("text");
                logr.debug(text.toString());
                String command = holoSection.getString("command", "");
                logr.debug(command);

                addHologram(new Hologram(uuid, loc, text, command));
                logr.debug("Hologram registered at (%s,%s,%s) in %s", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
            }
        }
    }

    public void addHologram(Hologram holo) {
        holograms.add(holo);
        ConfigurationSection holograms = config.getConfigurationSection("holograms");
        if (holograms == null)
            holograms = config.createSection("holograms");
        ConfigurationSection section = holograms.getConfigurationSection(holo.getId().toString());
        if (section == null)
            section = holograms.createSection(holo.getId().toString());
        section.set("location", holo.getLocation());
        section.set("text", holo.getDisplayText());
        section.set("command", holo.getCommand());
        saveConfig();

        if (updater.isCancelled())
            updater = updater().runTaskTimer(plugin, 100L, 10L);
    }

    private void saveConfig() {
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);

        try {
            if (!file.exists())
                file.createNewFile();
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean removeHologram(Hologram holo) {
        boolean removed = holograms.remove(holo);
        holo.remove();
        if (removed) {
            config.set("holograms." + holo.getId().toString(), null);
            saveConfig();
        }
        return removed;
    }

    public boolean isHologramItem(ItemStack item) {
        return item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().has(BghrApi.HOLOGRAM_TEXT_KEY, PersistentDataType.BYTE_ARRAY);
    }

    public boolean isHologramEntity(Entity e) {
        return e.hasMetadata(BghrApi.HOLOGRAM_TAG) && e.hasMetadata(BghrApi.HOLOGRAM_ID_TAG);
    }

    public Hologram getHoloFromEntity(Entity e) {
        if (isHologramEntity(e)) {
            UUID uuid = (UUID) e.getMetadata(BghrApi.HOLOGRAM_ID_TAG).get(0).value();

            if (uuid == null)
                return null;

            for (Hologram h : getHolograms()) {
                if (h.getId().equals(uuid))
                    return h;
            }
            return new Hologram(e.getLocation());
        }
        return null;
    }

    public List<Hologram> getHolograms() {
        return new ArrayList<>(holograms);
    }

    @EventHandler
    public void watchForDetachedHolos(EntityAddToWorldEvent event) {
        if (isHologramEntity(event.getEntity())) {
            Hologram holo = getHoloFromEntity(event.getEntity());
            if (!getHolograms().contains(holo)) {
                holo.remove();
            }
        }
    }

    @EventHandler
    public void watchForClicks(PlayerInteractAtEntityEvent event) {
        if (isHologramEntity(event.getRightClicked())) {
            Hologram holo = getHoloFromEntity(event.getRightClicked());

            if (holo == null || !holograms.contains(holo)) {
                logr.debug("ERROR: Could not find hologram");
                return;
            }
            if (event.getPlayer().isSneaking() && event.getPlayer().hasPermission("bghr.admin.holograms")) {
                if (removeHologram(holo)) {
                    event.getPlayer().getInventory().addItem(holo.createItem("&e&lPlaceable Hologram"));
                    return;
                }
            }

            if (holo.isClickable())
                Bukkit.dispatchCommand(event.getPlayer(), holo.getCommand());
        }
    }

    @EventHandler
    public void placeHologramFromItem(PlayerInteractEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();

        if (event.getPlayer().hasMetadata("placed_holo"))
            return;

        if (!useHolograms) {
            event.getPlayer().sendMessage(Trans.lateToComponent("Holograms are currently disabled."));
            event.setCancelled(true);
            return;
        }

        if (item != null && event.getClickedBlock() != null) {
            Location loc = event.getClickedBlock().getLocation();

            if (isHologramItem(item)) {
                event.getPlayer().setMetadata("placed_holo", new FixedMetadataValue(plugin, true));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Hologram holo = new Hologram(loc, item);
                        addHologram(holo);
                        holo.build();
                        event.getPlayer().getInventory().remove(item);
                        event.getPlayer().removeMetadata("placed_holo", plugin);
                        logr.debug("Place hologram at (%s,%s,%s) in %s. Command: %s",
                                   loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName(), holo.getCommand());
                        event.setCancelled(true);
                    }
                }.runTaskLater(JavaPlugin.getPlugin(BGHR.class), 1L);
            }
        }
    }

    public String replacePlaceholders(String string) {
        if (string.contains("%")) {
            String[] split = string.split("%");
            String placeholder = split[1];

            return string.replace("%" + placeholder + "%", placeholderReplacement(placeholder).get());
        }
        return string;
    }

    private Supplier<String> placeholderReplacement(String string) {
        String[] split = string.split("_");
        if (split.length < 3 || !split[0].equals("bghr"))
            return () -> "";

        if (split[1].contains("game")) {
            GameManager gm = plugin.getApi().getGameManager();
            if (gm.getActiveGames().isEmpty())
                return () -> "";

            int gameNumber = Integer.parseInt(split[1].substring(4));
            if (gm.getActiveGames().size() < gameNumber)
                return () -> "";

            Game game = gm.getActiveGames().get(gameNumber - 1);
            if (game == null)
                return () -> "";

            switch (split[2]) {
                case "info":
                    return () -> String.format("&7[&e%s&7] &7(&6%s&7) &7[&3%s&7/&3%s&7]",
                                               game.getMap().getMapName(),
                                               game.getOptions().getStartType().prettyName(),
                                               game.getActivePlayers().size(),
                                               game.getStartingPlayersSize());
                case "map":
                    return () -> game.getMap().getMapName();
                case "type":
                    return () -> game.getOptions().getStartType().prettyName();
                case "phase":
                    return game::getPhase;
                case "playercount":
                    return () -> String.valueOf(game.getActivePlayers().size());
                case "elapsed":
                    return () -> Tools.secondsToAbbreviatedMinsSecs(game.getCurrentGameTime());
            }
        }
        return () -> "";
    }

    public class HologramTemplate {
        private final List<String> text;
        private final String command;
        private final String name;

        public HologramTemplate(ConfigurationSection section) {
            text = section.getStringList("text");
            command = section.getString("command", "");
            name = section.getName();
        }

        public String getName() {
            return StringUtils.capitalize(name);
        }

        public String getCommand() {
            return command;
        }

        public List<String> getText() {
            return new ArrayList<>(text);
        }
    }
}
