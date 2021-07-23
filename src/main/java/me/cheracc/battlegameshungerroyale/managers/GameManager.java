package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Logr;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.GameOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {
    private static GameManager singletonInstance = null;
    private final List<Game> activeGames = new ArrayList<>();
    private final Scoreboard mainScoreboard;
    private final BukkitTask scoreboardUpdater;
    private final BGHR plugin;
    private final MapDecider mapDecider;

    private GameManager(BGHR plugin) {
        this.plugin = plugin;
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        mainScoreboard = scoreboardManager.getNewScoreboard();
        mapDecider = new MapDecider();
        setupScoreboard();
        setupScoreboardTeams();
        scoreboardUpdater = scoreboardUpdater();
        Game.createNewGame(mapDecider.selectNextMap(), plugin);
    }

    public static GameManager getInstance() {
        if (singletonInstance == null) {
            throw new InstantiationError("must initialize first using initialize(BGHR plugin)");
        }

        return singletonInstance;
    }

    public static void initialize(BGHR plugin) {
        singletonInstance = new GameManager(plugin);
    }

    public Scoreboard getMainScoreboard() {
        return mainScoreboard;
    }

    public List<Game> getActiveGames() {
        return new ArrayList<>(activeGames);
    }

    public boolean isInAGame(Player player) {
        return MapManager.getInstance().isThisAGameWorld(player.getWorld());
    }

    public BGHR getPlugin() {
        return plugin;
    }

    public boolean isActivelyPlayingAGame(Player player) {
        if (isInAGame(player)) {
            Game game = getPlayersCurrentGame(player);
            return game.isPlaying(player) && !game.getPhase().toLowerCase().contains("game");
        }
        return false;
    }

    public @Nullable Game getPlayersCurrentGame(Player player) {
        for (Game game : activeGames) {
            if (game.isPlaying(player) || game.isSpectating(player))
                return game;
        }
        return null;
    }

    public void setupGame(Game game) {
        activeGames.add(game);
        updateScoreboard();
    }

    public void gameOver(Game game) {
        activeGames.remove(game);
        mapDecider.setLastMap(game.getMap().getMapName());
        if (activeGames.isEmpty() && plugin.isEnabled())
            Game.createNewGame(mapDecider.selectNextMap(), plugin);
        updateScoreboard();
    }

    public void addVote(Player player, String mapName) {
        mapDecider.addVote(player, mapName);
    }

    public int getVotes(String mapName) {
        return mapDecider.getVotes(mapName);
    }

    public void stopUpdater() {
        scoreboardUpdater.cancel();
    }

    public List<GameOptions> getAllConfigs() {
        File configDir = new File(plugin.getDataFolder(), "gameconfigs/");
        List<GameOptions> configs = new ArrayList<>();

        if (!configDir.exists())
            if (configDir.mkdirs())
                Logr.info("Creating new directory for game config files...");

        if (configDir.listFiles().length == 0) {
            String[] defaultConfigNames = { "crystal_avalanche.yml", "horizon_city.yml", "island_tower.yml", "king_of_the_ring.yml" };

            boolean sent = false;
            for (String filename : defaultConfigNames) {
                File defaultConfig = new File(configDir, filename);
                try {
                    if (defaultConfig.createNewFile() && !sent) {
                        Logr.info("Placing the default game config files...");
                        sent = true;
                    }
                    InputStream in = plugin.getResource("default_game_configs/" + filename);
                    FileUtils.copyToFile(in, defaultConfig);
                    in.close();
                } catch (IOException e) {
                    Bukkit.getLogger().warning("couldn't create file " + defaultConfig.getAbsolutePath());
                }
            }

        }

        for (File file : configDir.listFiles()) {
            if (file.exists() && file.getName().contains(".yml")) {
                GameOptions options = new GameOptions();
                options.loadConfig(file);
                configs.add(options);
            }
        }
        return configs;
    }

    private void setupScoreboard() {
        Objective mainObj = mainScoreboard.registerNewObjective("mainSb", "dummy", Tools.componentalize("&e&lBattle Games: Hunger Royale!").hoverEvent(HoverEvent.showText(Tools.componentalize("Test"))));
        mainObj.setDisplaySlot(DisplaySlot.SIDEBAR);
        mainObj.getScore(ChatColor.AQUA + "  =======================").setScore(15);
        mainObj.getScore(ChatColor.translateAlternateColorCodes('&', "        &l&nCurrent Games")).setScore(14);
        mainObj.getScore("  " + ChatColor.MAGIC).setScore(13);

    }

    public void updateScoreboard() {
        int lineNumber = 12;
        for (Game game : GameManager.getInstance().getActiveGames()) {
            if (lineNumber < 0)
                break;
            mainScoreboard.getTeam(String.format("line%s", lineNumber)).prefix(Tools.componentalize(
                    String.format(" &a%s &3[&b%s&3]", game.getMap().getMapName(), StringUtils.capitalize(game.getPhase()))));
            lineNumber--;
            String line2;
            int totalGameTime = game.getOptions().getInvincibilityTime() + game.getOptions().getMainPhaseTime() + game.getOptions().getBorderTime();
            switch (game.getPhase()) {
                case "Pregame":
                    int needed = game.getOptions().getPlayersNeededToStart() - game.getStartingPlayersSize();
                    if (needed > 0)
                        line2 = String.format("&7    (Need &f%s &7more to start!&7)", needed);
                    else
                        line2 = String.format("&7    Starting in &f%s", Tools.secondsToAbbreviatedMinsSecs((int) game.getPregameTime()));
                    break;
                case "Invincibility":
                case "Main":
                case "Border":
                    line2 = String.format("    &3[&f%s/%s&3] &7(&f%s left&7)", game.getActivePlayers().size(), game.getStartingPlayersSize(),
                        Tools.secondsToAbbreviatedMinsSecs(totalGameTime - game.getCurrentGameTime()));
                    break;
                case "Postgame":
                    line2 = String.format("    &fWinner: &e%s&f! &7[&fCloses in %s&7]", game.getWinner() != null ? game.getWinner().getName() : "&7nobody",
                            Tools.secondsToAbbreviatedMinsSecs((int) game.getPostgameTime()));
                    break;
                default:
                    line2 = "";
            }
            mainScoreboard.getTeam(String.format("line%s", lineNumber)).prefix(Tools.componentalize(line2));
            lineNumber--;
        }
        if (lineNumber > 3)
            for (; lineNumber > 0; lineNumber--) {
                mainScoreboard.getTeam(String.format("line%s", lineNumber)).prefix(Component.space());
            }
        mainScoreboard.getTeam("line2").prefix(Tools.componentalize(" &e/games &7to join or watch a game").hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Tools.componentalize("/games"))));
        mainScoreboard.getTeam("line1").prefix(Tools.componentalize(" &e/settings &7to turn this off"));
    }

    public void setupScoreboardTeams() {
        for (int i = 12; i >= 0; i--) {
            String entry = ChatColor.values()[i] + "" + ChatColor.values()[i+1];
            Team lineText = mainScoreboard.registerNewTeam(String.format("line%s", i));
            lineText.addEntry(entry);
            mainScoreboard.getObjective("mainSb").getScore(entry).setScore(i);
        }
    }

    private BukkitTask scoreboardUpdater() {
        BukkitRunnable updater = new BukkitRunnable() {
            @Override
            public void run() {
                updateScoreboard();
            }
        };
        return updater.runTaskTimer(plugin, 100L, 20L);
    }

    private class MapDecider {
        private final Map<UUID, String> outstandingVotes = new HashMap<>();
        private String lastMap = null;

        public int getVotes(String mapName) {
            return (int) outstandingVotes.values().stream().filter(s -> s.equals(mapName)).count();
        }

        public void addVote(Player player, String mapName) {
            if (outstandingVotes.containsKey(player.getUniqueId()))
                player.sendMessage(Tools.componentalize("Your vote has been changed to " + mapName));
            else
                player.sendMessage(Tools.componentalize("Your vote has been cast for " + mapName));
            outstandingVotes.put(player.getUniqueId(), mapName);
        }

        public void setLastMap(String mapName) {
            this.lastMap = mapName;
        }

        public String getLastMap() {
            return lastMap;
        }

        public GameOptions selectNextMap() {
            GameOptions winner = null;
            if (outstandingVotes.isEmpty()) {
                winner = selectRandomConfig();
            }

            Map<GameOptions, Integer> voteCounts = new HashMap<>();
            for (GameOptions opts : getAllConfigs()) {
                for (String s : outstandingVotes.values()) {
                    if (s.equals(opts.getMap().getMapName()) && !s.equals(lastMap)) {
                        Integer count = voteCounts.get(opts);
                        voteCounts.put(opts, count == null ? 1 : count + 1);
                    }
                }
            }

            for (Map.Entry<GameOptions, Integer> e : voteCounts.entrySet()) {
                if (winner == null)
                    winner = e.getKey();
                if (e.getValue() > voteCounts.get(e.getKey()))
                    winner = e.getKey();
            }

            if (winner == null)
                winner = getAllConfigs().get(0);

            String winnerMapName = winner.getMap().getMapName();
            outstandingVotes.values().removeIf(s -> s.equals(winnerMapName));
            return winner;
        }

        private GameOptions selectRandomConfig() {
            List<GameOptions> configs = getAllConfigs();

            if (mapDecider.getLastMap() != null && configs.size() > 2)
                configs.removeIf(opts -> opts.getMap().getMapName().equals(mapDecider.getLastMap()));

            Collections.shuffle(configs);
            int index = configs.size() > 1 ? ThreadLocalRandom.current().nextInt(0, configs.size() - 1) : 0;

            return configs.get(index);
        }
    }
}
