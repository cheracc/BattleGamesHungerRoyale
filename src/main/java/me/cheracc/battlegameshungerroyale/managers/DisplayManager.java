package me.cheracc.battlegameshungerroyale.managers;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.guis.TopStatsGui;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Hologram;
import me.cheracc.battlegameshungerroyale.types.Metadata;
import me.cheracc.battlegameshungerroyale.types.Skorbord;
import me.cheracc.battlegameshungerroyale.types.games.Game;
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
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class DisplayManager implements Listener {
    private final static String CONFIG_FILE = "displays.yml";
    private final List<Hologram> holograms;
    private final List<HologramTemplate> holoTemplates;
    private final BGHR plugin;
    private final Logr logr;
    private final FileConfiguration config;
    private final boolean useHolograms;
    private Skorbord skorbord = null;
    private BukkitTask updater;

    public DisplayManager(BGHR plugin, Logr logr) {
        this.plugin = plugin;
        this.logr = logr;
        config = new YamlConfiguration();
        holograms = new ArrayList<>();
        holoTemplates = new ArrayList<>();
        useHolograms = plugin.getConfig().getBoolean("enable holograms", true);
        updater = updater().runTaskTimer(plugin, 100L, 10L);
    }

    public Scoreboard getMainScoreboard() {
        if (skorbord == null)
            return Bukkit.getScoreboardManager().getMainScoreboard();
        return skorbord.getScoreboard();
    }

    private BukkitRunnable updater() {
        logr.debug("Starting DisplayManager Updater...");
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (holograms.isEmpty() && !plugin.getConfig().getBoolean("show main scoreboard", true)) {
                    cancel();
                    logr.debug("Nothing to update, stopping updater...");
                    return;
                }
                updateHolograms();
                if (skorbord != null)
                    skorbord.update();
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

        if (!file.exists()) {
            plugin.saveResource(CONFIG_FILE, false);
        }

        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        ConfigurationSection scoreboards = config.getConfigurationSection("scoreboards");
        if (scoreboards != null) {
            List<String> scoreboardText = scoreboards.getStringList("main");
            if (scoreboardText != null)
                skorbord = new Skorbord("&e&lBattle Games: Hunger Royale!", scoreboardText, plugin.getApi());
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
                Location loc = (Location) holoSection.get("location");
                List<String> text = holoSection.getStringList("text");
                String command = holoSection.getString("command", "");

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
        return e.hasMetadata(Metadata.HOLOGRAM_TAG.key()) || e.hasMetadata(Metadata.HOLOGRAM_ID_TAG.key());
    }

    public Hologram getHoloFromEntity(Entity e) {
        if (isHologramEntity(e)) {
            UUID uuid = (UUID) e.getMetadata(Metadata.HOLOGRAM_ID_TAG.key()).get(0).value();

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
                        Hologram holo = new Hologram(loc.add(0.5, 0, 0.5), item);
                        addHologram(holo);
                        holo.build();
                        event.getPlayer().getInventory().remove(item);
                        event.getPlayer().removeMetadata("placed_holo", plugin);
                        logr.debug("Placed new hologram at (%s,%s,%s) in %s. Command: %s",
                                   loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName(), holo.getCommand());
                        event.setCancelled(true);
                    }
                }.runTaskLater(JavaPlugin.getPlugin(BGHR.class), 1L);
            }
        }
    }

    public String replacePlaceholders(String string) {
        int count = 0;
        while (string.contains("%") && count < 5) {
            String[] split = string.split("%");
            string = string.replace("%" + split[1] + "%", placeholderReplacement(split[1]).get());
            count++;
        }
        return string;
    }

    private Supplier<String> placeholderReplacement(String string) {
        String[] split = string.replace("%", "").split("_");
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
                                               game.getGameTypeName(),
                                               game.getActivePlayers().size(),
                                               game.getStartingPlayersSize());
                case "map":
                    return () -> game.getMap().getMapName();
                case "type":
                    return game::getGameTypeName;
                case "phase":
                    return game::getPhase;
                case "playercount":
                    return () -> String.valueOf(game.getActivePlayers().size());
                case "elapsed":
                    return () -> Tools.secondsToAbbreviatedMinsSecs(game.getCurrentGameTime());
            }
            // %bghr_topstats_wins_name_1%
            // %bghr_topstats_kills_amount_3%
        } else if (split[1].contains("topstats")) {
            TopStatsGui.TopTenCategory result = plugin.getApi().getTopStatsGui().getTopTen(split[2]);
            if (result != null) {
                if (split.length > 4) {
                    try {
                        int place = Integer.parseInt(split[4]);
                        if (split[3].equalsIgnoreCase("name"))
                            return () -> result.getPlaceName(place);
                        else
                            return () -> String.valueOf(result.getPlaceValue(place));
                    } catch (NumberFormatException e) {
                        return () -> "";
                    }
                }
            }
            // %bghr_sb_game1_1%
        } else if (split[1].contains("sb"))
            return plugin.getApi().getGameManager().replaceScoreboardPlaceholders(split);
        return () -> "";
    }

    public static class HologramTemplate {
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
