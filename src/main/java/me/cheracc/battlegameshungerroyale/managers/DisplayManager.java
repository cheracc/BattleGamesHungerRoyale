package me.cheracc.battlegameshungerroyale.managers;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Hologram;
import me.cheracc.battlegameshungerroyale.types.Metadata;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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
    private Scoreboard mainScoreboard;
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

    @EventHandler
    public void delayedTasks(ServerLoadEvent event) {
        mainScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        setupScoreboard();
    }

    public Scoreboard getMainScoreboard() {
        return mainScoreboard;
    }

    private void setupScoreboard() {
        Objective obj = mainScoreboard.registerNewObjective("main", "dummy", Tools.componentalize(""));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.displayName(Tools.componentalize("&e&lBattle Games: Hunger Royale!"));
        for (int i = 15; i >= 0; i--) {
            String entry = ChatColor.values()[i] + "" + ChatColor.values()[i + 1];
            Team lineText = mainScoreboard.registerNewTeam(String.format("line%s", i));
            lineText.addEntry(entry);
            obj.getScore(entry).setScore(i);
        }
    }

    protected void updateScoreboard() {
        setScoreboardLine(14, centerLine(14, "&f&lCurrent Games:"));
        setScoreboardLine(13, "");
        int currentLine = 12;
        for (Game game : plugin.getApi().getGameManager().getActiveGames()) {
            if (currentLine > 2) {
                Component gameLine = Trans.lateToComponent("&e\u25BA &6&n%s &7[&a/join %s&7]",
                                                           game.getGameTypeName(), plugin.getApi().getGameManager().getActiveGames().indexOf(game) + 1);
                gameLine.hoverEvent(HoverEvent.showText(Tools.componentalize("Player List:")));
                setScoreboardLine(currentLine, gameLine);
                currentLine--;
                setScoreboardLine(currentLine, String.format("  &bMap&3: &f%s &bPhase&3: &f%s", game.getMap().getMapName(), game.getPhase()));
                currentLine--;
                if (game.getPhase().equalsIgnoreCase("pregame")) {
                    if (game.getActivePlayers().size() >= game.getOptions().getPlayersNeededToStart()) {
                        setScoreboardLine(currentLine, String.format("  &3Starting in %s", Math.abs(game.getCurrentGameTime())));
                    } else {
                        setScoreboardLine(currentLine, String.format("  &3Waiting for Players... &7(Need %s)", game.getOptions().getPlayersNeededToStart() - game.getActivePlayers().size()));
                    }
                } else {
                    setScoreboardLine(currentLine, String.format("  &bPlayers: &f%s &bTime:&f %s", game.getActivePlayers().size(), Tools.secondsToAbbreviatedMinsSecs(game.getCurrentGameTime())));
                }
                currentLine--;
                setScoreboardLine(currentLine, String.format("%s", spaces(currentLine)));
                currentLine--;
            }
        }
        setScoreboardLine(1, centerLine(1, "  &7(You can turn this off in &e&o/settings&7)"));
        setScoreboardLine(15, "&b" + equalsBar(getLongestScoreboardLine(15)));
    }

    private int getLongestScoreboardLine(int line) {
        String longest = "";
        for (Team t : mainScoreboard.getTeams()) {
            if (!t.equals(mainScoreboard.getTeam("line" + line))) {
                String lineText = ChatColor.stripColor(Tools.decomponentalize(t.prefix()));
                if (lineText.length() > longest.length())
                    longest = lineText;
            }
        }
        return longest.length() - 1;
    }

    private String equalsBar(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length * 0.8; i++)
            sb.append("=");
        return sb.toString();
    }

    private String spaces(int number) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number; i++)
            sb.append(" ");
        return sb.toString();
    }

    private String centerLine(int line, String text) {
        int fieldLength = getLongestScoreboardLine(line);
        String stripped = ChatColor.stripColor(text);
        int blankSpaceNeeded = fieldLength - stripped.length();
        blankSpaceNeeded *= 1.5;

        return spaces(blankSpaceNeeded / 2 + 1) + text + spaces(blankSpaceNeeded / 2);
    }

    protected void setScoreboardLine(int line, Component component) {
        mainScoreboard.getTeam("line" + line).prefix(component);
    }

    protected void setScoreboardLine(int line, String text) {
        mainScoreboard.getTeam("line" + line).prefix(Tools.componentalize(text));
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
                updateScoreboard();
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
        logr.debug("PIAEE");
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
        }
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
