package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.events.*;
import me.cheracc.battlegameshungerroyale.managers.LootManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.MapData;
import me.cheracc.battlegameshungerroyale.types.Metadata;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public abstract class Game implements Listener {
    protected final List<Location> spawnPoints;
    protected final List<BukkitTask> tasks;
    private final MapData map;
    private final GameOptions options;
    private final BossBar bar;
    private final Scoreboard scoreboard;
    private final Map<UUID, Integer> participants;
    private final boolean useLootManager;
    protected BghrApi api;
    protected Respawner respawner;
    private GamePhase currentPhase;
    private LootManager lootManager;
    private World world;
    private GameLog gameLog;
    private boolean openToPlayers;
    private double gameTime;
    private long startTime;
    private Player winner = null;

    public Game(GameOptions options) {
        currentPhase = GamePhase.PREGAME;
        this.map = options.getMap();
        this.options = options;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.participants = new HashMap<>();
        this.spawnPoints = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.respawner = new Respawner();
        useLootManager = options.isGenerateChests() || options.isFillAllChests();
        setupScoreboard();
        bar = Bukkit.createBossBar("Pregame", BarColor.WHITE, BarStyle.SOLID);
        bar.setVisible(true);
        openToPlayers = false;
        gameTime = -1;
    }

    // protected constructor only used to get icons/descriptions from subtypes
    protected Game() {
        currentPhase = GamePhase.PREGAME;
        this.map = null;
        this.options = null;
        this.scoreboard = null;
        this.participants = new HashMap<>();
        this.spawnPoints = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.bar = null;
        openToPlayers = false;
        useLootManager = false;
        gameTime = -Integer.MAX_VALUE;
    }

    public abstract String getGameTypeName();
    public abstract String getGameDescription();
    public abstract Material getGameIcon();

    // hook methods
    protected void runGameSetupProcedures() {
    }

    protected void runExtraGameStartProcedures() {
    }

    // this is called by the game manager after the world is copied and loaded
    public void initializeGame(World world, BghrApi api) {
        Bukkit.getPluginManager().registerEvents(this, api.getPlugin());

        this.world = world;
        this.api = api;

        gameLog = new GameLog(this, api.getPlugin());
        gameLog.addLogEntry("Starting " + options.getConfigFile().getName());

        if (useLootManager)
            this.lootManager = new LootManager(this, api.getPlugin(), api.logr(), api.getGameManager());
        else
            this.lootManager = null;

        if (map.isUseBorder() && map.getBorderRadius() > 0) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(map.getBorderCenter(world));
            border.setSize(map.getBorderRadius() * 2);
        }

        setPregameGameRules();
        loadPossibleSpawnPoints();
        openGameToPlayers();

        api.getGameManager().makeThisGameAvailable(this);

        tasks.add(startPregameTimer().runTaskTimer(api.getPlugin(), 20L, 4L));

        runGameSetupProcedures();

        new GameLoadedEvent(this).callEvent();
    }

    protected LootManager getLootManager() {
        return lootManager;
    }

    protected void setPregameGameRules() {
        // always
        getWorld().setDifficulty(Difficulty.NORMAL);
        getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        getWorld().setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        getWorld().setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);

        // for pregame only
        getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        getWorld().setGameRule(GameRule.DO_FIRE_TICK, false);
        getWorld().setGameRule(GameRule.DO_MOB_SPAWNING, false);
        getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    }

    // returns the full class name
    public String getGameType() {
        return getClass().getName();
    }

    private void startGame() {
        setGameRulesForStartOfGame();
        startTime = System.currentTimeMillis();

        sendAllPlayersToSpawn();

        forEachActivePlayer(p -> {
            PlayerData data = api.getPlayerManager().getPlayerData(p);
            if (data.getKit() == null || (!data.getKit().isEnabled() && !p.hasPermission("bghr.admin.kits.disabled"))) {
                data.assignKit(api.getKitManager().getRandomKit(false), false);
            }
            api.getPlayerManager().outfitPlayer(p, data.getKit());
        });

        new GameStartEvent(this).callEvent();

        api.logr().debug("Starting game tick task");
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                if (getPhase().equalsIgnoreCase("postgame")) {
                    closeGameToPlayers();
                    api.logr().debug("Stopping game tick task");
                    cancel();
                    return;
                }
                doMainGameTick();
            }
        }.runTaskTimer(api.getPlugin(), 10L, 10L));
    }

    protected void forEachActivePlayer(Consumer<Player> doThis) {
        for (Player p : getActivePlayers()) {
            doThis.accept(p);
        }
    }

    protected void setGameRulesForStartOfGame() {
        getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        getWorld().setGameRule(GameRule.DO_FIRE_TICK, true);
        getWorld().setGameRule(GameRule.DO_MOB_SPAWNING, true);
        getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, true);
    }

    protected void sendAllPlayersToSpawn() {
        List<Player> players = new ArrayList<>(getActivePlayers());
        List<Location> spawns = new ArrayList<>(getXSpawnPoints(players.size()));

        if (players.size() > spawns.size()) {
            int startSize = spawns.size();
            api.logr().warn("Could not find enough spawns. Re-using spawnpoints as needed (you should fix this by re-defining your spawn center and radius)");
            while (spawns.size() < players.size()) {
                spawns.add(spawns.get(ThreadLocalRandom.current().nextInt(spawns.size() - 1)));
            }
            api.logr().warn("Added %s duplicate spawn points (this means players will spawn on top of each other)", spawns.size() - startSize);
        }

        Collections.shuffle(players);
        Collections.shuffle(spawns);

        for (Player p : players) {
            Tools.uncheckedTeleport(p, spawns.get(players.indexOf(p)));
        }
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public int getCurrentGameTime() {
        return (int) gameTime;
    }

    // just used for displaying game info - can be overridden if necessary
    public String getPhase() {
        return currentPhase.prettyName();
    }

    protected void setPhase(GamePhase phase) {
        currentPhase = phase;
    }

    public MapData getMap() {
        return map;
    }

    public World getWorld() {
        return world;
    }

    public int getLivesLeft(UUID uuid) {
        if (participants.containsKey(uuid))
            return participants.get(uuid);
        return 0;
    }

    public boolean isActive() {
        return gameTime > 0;
    }

    public boolean isOpenToPlayers() {
        return openToPlayers;
    }

    protected void openGameToPlayers() {
        openToPlayers = true;
    }

    protected void closeGameToPlayers() {
        openToPlayers = false;
    }

    public boolean isPlaying(Player player) {
        if (!player.isOnline())
            return false;
        if (!participants.containsKey(player.getUniqueId()))
            return false;
        if (!player.getWorld().equals(world))
            return false;
        return participants.get(player.getUniqueId()) > 0 && world.equals(player.getWorld());
    }

    public boolean isSpectating(Player player) {
        if (player.getWorld().equals(world)) {
            if (player.getGameMode() == GameMode.SPECTATOR)
                return true;
            if (participants.get(player.getUniqueId()) == null)
                return true;
            return participants.get(player.getUniqueId()) == 0;
        }
        return false;
    }

    public void joinAsSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(randomSpawnPoint(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        bar.addPlayer(player);
        new PlayerJoinedGameEvent(player, this, true).callEvent();
    }

    protected Location randomSpawnPoint() {
        Location loc = spawnPoints.get(ThreadLocalRandom.current().nextInt(spawnPoints.size() - 1));

        int tries = 0;
        while (!loc.getNearbyPlayers(1).isEmpty() && tries < 10) {
            loc = spawnPoints.get(ThreadLocalRandom.current().nextInt(spawnPoints.size() - 1));
            tries++;
        }
        return loc;
    }

    public void join(Player player) {
        PlayerData data = api.getPlayerManager().getPlayerData(player);
        data.saveLocationAndInventory(true);

        player.teleport(randomSpawnPoint(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        Metadata.removeAll(player);
        participants.put(player.getUniqueId(), options.getLivesPerPlayer());

        if (options.isAllowRegularBuilding())
            player.setGameMode(GameMode.SURVIVAL);
        else
            player.setGameMode(GameMode.ADVENTURE);

        bar.addPlayer(player);

        if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.INVINCIBILITY)
            player.setInvulnerable(true);

        if (data.getKit() == null) {
            player.sendMessage(Trans.lateToComponent("&fYou haven't selected a kit! Type &e/kit &fto open the menu!"));
        } else {
            player.sendMessage(Trans.lateToComponent("&fYour selected kit is &e%s&f. It will be equipped when the game starts", data.getKit().getName()));
        }

        if (data.getSettings().isShowGameScoreboard()) {
            player.setMetadata(Metadata.PREGAME_SCOREBOARD.key(), new FixedMetadataValue(api.getPlugin(), player.getScoreboard()));
            player.setScoreboard(getScoreboard());
        }

        player.setMetadata(Metadata.PREGAME_HEALTH.key(), new FixedMetadataValue(api.getPlugin(), player.getHealth()));
        player.setHealth(20);
        player.setMetadata(Metadata.PREGAME_FOOD_LEVEL.key(), new FixedMetadataValue(api.getPlugin(), player.getFoodLevel()));
        player.setFoodLevel(20);
        new PlayerJoinedGameEvent(player, this, false).callEvent();
    }

    public void quit(Player player) {
        if (currentPhase == GamePhase.PREGAME) {
            participants.remove(player.getUniqueId());
        }
        int livesRemaining = 0;
        if (participants.containsKey(player.getUniqueId()))
            livesRemaining = participants.get(player.getUniqueId());

        participants.replace(player.getUniqueId(), 0);
        player.getPassengers().forEach(player::removePassenger);
        new PlayerQuitGameEvent(player, this, livesRemaining).callEvent();

        PlayerData data = api.getPlayerManager().getPlayerData(player);

        bar.removePlayer(player);

        if (api.getPlugin().getConfig().getBoolean("main world.place players at spawn on join", false))
            player.teleport(api.getMapManager().getLobbyWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        else
            player.teleport(data.getLastLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);

        restorePlayer(player);
    }

    protected void restorePlayer(Player player) {
        PlayerData data = api.getPlayerManager().getPlayerData(player);

        api.getPlayerManager().restorePlayerFromSavedData(player, data);

        player.setGameMode(Bukkit.getDefaultGameMode());
        player.setAllowFlight(false);
        player.setInvulnerable(false);
        Arrays.stream(PotionEffectType.values()).forEach(player::removePotionEffect);

        if (player.hasMetadata(Metadata.PREGAME_HEALTH.key())) {
            player.setHealth((double) player.getMetadata(Metadata.PREGAME_HEALTH.key()).get(0).value());
        }
        if (player.hasMetadata(Metadata.PREGAME_FOOD_LEVEL.key())) {
            player.setFoodLevel((int) player.getMetadata(Metadata.PREGAME_FOOD_LEVEL.key()).get(0).value());
        }

        Metadata.removeAll(player);
    }

    public Player getWinner() {
        if (checkForWinner()) {
            return winner;
        }
        return null;
    }

    public GameOptions getOptions() {
        return options;
    }

    public int getStartingPlayersSize() {
        return participants.size();
    }

    public List<String> getFullPlayerList() {
        List<String> list = new ArrayList<>();
        for (UUID u : participants.keySet()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline())
                list.add(p.getName());
            else
                list.add(Bukkit.getOfflinePlayer(u).getName());
        }
        return list;
    }

    public Set<Player> getCurrentPlayersAndSpectators() {
        Set<Player> players = getActivePlayers();

        if (world.getPlayers() != null)
            players.addAll(world.getPlayers());
        return players;
    }

    public Set<Player> getActivePlayers() {
        Set<UUID> activePlayerIds = new HashSet<>();

        participants.forEach((id, lives) -> {
            Player p = Bukkit.getPlayer(id);
            if (lives > 0 &&
                    p != null &&
                    isPlaying(p))
                activePlayerIds.add(id);
        });

        Set<Player> activePlayers = new HashSet<>();
        activePlayerIds.forEach(id -> {
            Player p = Bukkit.getPlayer(id);
            if (winner != null && (p == null || !p.isOnline() || !isPlaying(p))) {
                quit(p);
            } else
                activePlayers.add(p);
        });

        return activePlayers;
    }

    protected void loadPossibleSpawnPoints() {
        long start = System.currentTimeMillis();
        // first only load spawn blocks if the map is configured for them
        if (getMap().getSpawnBlockType() == null || findSpawnBlocks() == 0) {
            // if none were found, find our own
            Location center = map.getSpawnCenter(world);
            int numberOfPotentialSpawnToLocate = 200;
            int radius = map.getSpawnRadius();

            // yay some basic trig and linear algebra! gets x locations evenly spaced around the radius of the spawn circle and sets their facing towards the center
            for (float angle = 0; angle < 2 * Math.PI; angle += 2 * Math.PI / numberOfPotentialSpawnToLocate) {
                Location loc = center.clone().add(radius * Math.cos(angle), 0, radius * Math.sin(angle));
                loc = loc.getWorld().getHighestBlockAt(loc).getLocation();

                // since we got the highest block, we want to make sure its not on a roof or something
                // this also makes sure there is no bridge or overhang incase of elytra spawns (like in horizon city map)
                if (Math.abs(loc.getY() - center.getY()) > 5) {
                    continue;
                }
                loc.add(0, 1.5, 0);
                loc.setDirection(center.clone().subtract(loc).toVector());
                spawnPoints.add(loc);
            }
            api.logr().debug("Found %s possible spawn locations %s blocks from the center of spawn (took %s ms)",
                             spawnPoints.size(), radius, System.currentTimeMillis() - start);
        }
    }

    protected int findSpawnBlocks() {
        Location center = map.getSpawnCenter(world);
        int scanRadius = map.getSpawnRadius() + 1;

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++)
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    Location l = center.clone().add(x, y, z);
                    Block b = l.getBlock();

                    if (b.getType() == map.getSpawnBlockType()) {
                        l.add(0.5, 0, 0.5);
                        l.setDirection(center.subtract(l).toVector());
                        spawnPoints.add(l);
                    }
                }
        }
        if (spawnPoints.size() > 0)
            api.logr().debug("Found " + spawnPoints.size() + " spawn points on " + map.getSpawnBlockType().name());
        return spawnPoints.size();
    }

    protected List<Location> getXSpawnPoints(int number) {
        List<Location> spawns = new ArrayList<>();

        if (number > spawnPoints.size()) {
            spawns.addAll(spawnPoints);
            api.logr().warn("There are not enough spawn points. Either this map is misconfigured or it does not have enough defined spawn point blocks for the amount of players. Players are going to spawn on top of each other. You should fix this.");
        }
        int skip = spawnPoints.size() / ((number == 0) ? 1 : number);
        if (skip == 0)
            skip++;
        spawns.add(spawnPoints.get(0));
        while (spawns.size() < number) {
            Location last = spawns.get(spawns.size() - 1);
            spawns.add(getNextCircular(last, spawnPoints, skip));
        }
        return spawns;
    }

    private <T> T getNextCircular(T element, List<T> list, int skip) {
        if (list == null || list.isEmpty())
            return null;
        if (list.size() == 1)
            return list.get(0);

        int currentIndex = list.indexOf(element);
        int nextIndex = currentIndex + skip;

        if (nextIndex > list.size() - 1)
            nextIndex -= list.size();

        return list.get(nextIndex);
    }

    protected BossBar getBossBar() {
        return bar;
    }

    // default boss bar, can be overridden by children
    protected void updateBossBar() {
        int[] length = {options.getPregameTime(), options.getInvincibilityTime(), options.getMainPhaseTime(), options.getBorderTime(), options.getPostGameTime()};
        BarColor[] color = {BarColor.WHITE, BarColor.PINK, BarColor.BLUE, BarColor.RED, BarColor.GREEN};
        String[] names = {"Pregame", "Invincibility", "Main Phase", "Border Shrinking", "Postgame"};

        bar.setColor(color[currentPhase.ordinal()]);
        bar.setTitle(names[currentPhase.ordinal()]);

        double phaseProgress;
        if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.POSTGAME)
            phaseProgress = options.getPregameTime() + gameTime;
        else {
            phaseProgress = gameTime;
            if (currentPhase.ordinal() > 1)
                for (int i = 1; i < currentPhase.ordinal(); i++) {
                    phaseProgress -= length[i];
                }
        }
        phaseProgress = Math.max(0, Math.min(1, phaseProgress / length[currentPhase.ordinal()]));
        bar.setProgress(1 - phaseProgress);
    }

    public boolean checkForWinner() {
        if (winner != null)
            return true;

        int count = (int) participants.values().stream().filter(lives -> lives > 0).count();

        if (count <= 1 && gameTime > 0) {
            participants.forEach((key, value) -> {
                if (value > 0)
                    winner = Bukkit.getPlayer(key);
            });
            gameLog.addLogEntry(winner.getName() + " won!");
            doPostGame();
        }

        return winner != null;
    }

    protected void startMainPhase() {
        setPhase(GamePhase.MAIN);
        new GameChangedPhaseEvent(this, "main").callEvent();
        List<UUID> toRemove = new ArrayList<>();
        for (UUID uuid : participants.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                toRemove.add(uuid);
                continue;
            }
            if (isPlaying(p)) {
                p.setInvulnerable(false);
            }
        }
        for (UUID id : toRemove)
            participants.remove(id);
        runExtraGameStartProcedures();
    }

    private void doPostGame() {
        updateScoreboard();
        map.addGamePlayed((int) gameTime);
        new GameFinishedEvent(this, getWinner()).callEvent();
        tasks.add(startPostGameTimer());
        currentPhase = GamePhase.POSTGAME;
        new GameChangedPhaseEvent(this, "postgame").callEvent();
        api.getGameManager().gameIsEnding(this);
        world.getWorldBorder().setSize(world.getWorldBorder().getSize() + 4);
    }

    // just ends the game - used at end of postgame and when disabling the plugin
    public void endGame() {
        endGame(null);
    }

    // ends the game and calls back when everything is unloaded
    public void endGame(Consumer<Game> callback) {
        for (BukkitTask task : tasks) {
            api.logr().debug("Canceling task %s", task.getTaskId());
            task.cancel();
        }

        if (lootManager != null)
            lootManager.close();

        gameLog.finalizeLog();

        for (Player p : getCurrentPlayersAndSpectators())
            quit(p);

        if (winner != null) {
            map.addGamePlayed((int) (System.currentTimeMillis() - startTime / 1000));
        }

        gameLog = null;
        lootManager = null;
        winner = null;

        bar.setVisible(false);
        bar.removeAll();
        participants.clear();
        spawnPoints.clear();
        api.getMapManager().unloadWorld(world);
        api.getGameManager().gameOver(this, callback);
        HandlerList.unregisterAll(this);
    }

    protected void setupScoreboard() {
        Objective obj = getScoreboard().registerNewObjective("main", "dummy", Tools.componentalize(""));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.displayName(Tools.componentalize(String.format("&f&l%s", getGameTypeName())));
        for (int i = 15; i >= 0; i--) {
            String entry = ChatColor.values()[i] + "" + ChatColor.values()[i + 1];
            Team lineText = getScoreboard().registerNewTeam(String.format("line%s", i));
            lineText.addEntry(entry);
            obj.getScore(entry).setScore(i);
        }
    }

    protected void updateScoreboard() {
        setScoreboardLine(15, "&6Phase: &e" + getPhase());
        setScoreboardLine(14, String.format("&6Border Size: &e%s", (int) getWorld().getWorldBorder().getSize()));
        if (gameTime > 0)
            setScoreboardLine(13, String.format("&6Time Elapsed: &e%s", Tools.secondsToAbbreviatedMinsSecs((int) gameTime)));
        else
            setScoreboardLine(13, String.format("&6Starts In: &e%s", Tools.secondsToAbbreviatedMinsSecs((int) -gameTime)));
        setScoreboardLine(12, "&nPlayer&f                 &nLives");
        int line = 11;
        List<Map.Entry<UUID, Integer>> living = new LinkedList<>(participants.entrySet());
        living.sort(Map.Entry.comparingByValue());
        Collections.reverse(living);

        for (Map.Entry<UUID, Integer> entry : living) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null && p.isOnline() && isPlaying(p)) {
                quit(p);
                continue;
            }
            if (line <= 0)
                break;

            ChatColor[] colors;
            if (entry.getValue() > 0)
                colors = new ChatColor[]{ChatColor.WHITE, ChatColor.GREEN};
            else
                colors = new ChatColor[]{ChatColor.GRAY, ChatColor.RED};

            setScoreboardLine(line, String.format("%s%-24s %s%d", colors[0], p.getName(), colors[1], entry.getValue()));
            line--;
        }
    }

    protected void setScoreboardLine(int line, String text) {
        getScoreboard().getTeam("line" + line).prefix(Tools.componentalize(text));
    }

    // game tasks
    private BukkitTask startPostGameTimer() {
        gameTime = (-1 * options.getPostGameTime());

        BukkitRunnable postGameTimer = new BukkitRunnable() {
            long last = System.currentTimeMillis();

            @Override
            public void run() {
                gameTime += (System.currentTimeMillis() - last) / 1000D;

                if (gameTime >= 0) {
                    api.logr().debug("Stopping postGameTimer");
                    cancel();
                    endGame();
                    return;
                }
                updateBossBar();
                last = System.currentTimeMillis();
            }
        };

        return postGameTimer.runTaskTimer(api.getPlugin(), 20L, 4L);
    }

    private BukkitRunnable startPregameTimer() {
        gameTime = -options.getPregameTime();
        return new BukkitRunnable() {
            double last = System.currentTimeMillis();

            @Override
            public void run() {
                if (currentPhase != GamePhase.PREGAME) {
                    api.logr().debug("Stopping pregame timer because it is not pregame");
                    cancel();
                }

                if (gameTime >= 0) {
                    // check if there's enough players to start
                    if (getActivePlayers().size() >= options.getPlayersNeededToStart()) {
                        api.logr().debug("Stopping pregame timer to start the game");
                        cancel();
                        startGame();
                    } else {
                        gameTime = -options.getPregameTime();
                    }
                } else
                    gameTime += (System.currentTimeMillis() - last) / 1000D;

                updateBossBar();
                updateScoreboard();
                last = System.currentTimeMillis();
            }
        };
    }

    protected void setGameTime(double time) {
        gameTime = time;
    }

    protected long getStartTime() {
        return startTime;
    }

    // runs every 0.5 seconds
    protected void doMainGameTick() {
        gameTime = (System.currentTimeMillis() - startTime) / 1000D;

        if (lootManager != null)
            lootManager.tick();

        if (respawner != null)
            respawner.tick();

        onTick();
        updateBossBar();
        updateScoreboard();
        new GameTickEvent(this).callEvent();
    }

    // hook method
    protected void onTick() {
    }

    private void doThisInOneTick(Consumer<Boolean> func) {
        new BukkitRunnable() {
            @Override
            public void run() {
                func.accept(true);
            }
        }.runTaskLater(api.getPlugin(), 1L);
    }

    // listeners
    @EventHandler
    public void handleQuits(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (p.getWorld().equals(getWorld())) {
            p.setAllowFlight(false);
            p.setInvulnerable(false);
            if (isSpectating(p) || isPlaying(p))
                quit(p);
        }
    }

    // happens immediately after death
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final Player p = event.getPlayer();
        final Entity killer;

        if (!p.getWorld().equals(getWorld()))
            return;

        if (p.hasMetadata(Metadata.KILLER.key())) {
            killer = (LivingEntity) p.getMetadata(Metadata.KILLER.key()).get(0).value();
            if (killer != null) {
                p.removeMetadata(Metadata.KILLER.key(), api.getPlugin());
            }
        } else
            killer = p;

        event.setRespawnLocation(killer.getLocation());

        doThisInOneTick(b -> {
            p.setGameMode(GameMode.SPECTATOR);
            if (!killer.equals(p))
                p.setSpectatorTarget(killer);
        });

        Component subtitle;
        if (isPlaying(p))
            subtitle = Trans.lateToComponent("&eYou will respawn in &f%s &eseconds", respawner.respawnTime);
        else
            subtitle = Trans.lateToComponent("&7You have no lives remaining");

        p.showTitle(Title.title(Trans.lateToComponent("&c&lYou Died!"), subtitle, Title.Times.of(Duration.ofSeconds(0), Duration.ofSeconds(1), Duration.ofSeconds(0))));

        respawner.add(p);
    }

    @EventHandler
    public void handlePvp(GameDamageEvent event) {
        if (!event.getGame().equals(this))
            return;

        switch (currentPhase) {
            case PREGAME:
            case INVINCIBILITY:
            case POSTGAME:
                event.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void handleDeaths(GameDeathEvent event) {
        Player dead = event.getRecentlyDeceased();
        Game game = event.getGame();

        if (game != null && game.equals(this)) {
            int livesLeft = Math.max(0, participants.get(dead.getUniqueId()) - 1);
            participants.replace(dead.getUniqueId(), livesLeft);

            LivingEntity killer = event.getRecentlyDeceased();
            if (event.getKiller() instanceof LivingEntity)
                killer = (LivingEntity) event.getKiller();
            dead.setMetadata(Metadata.KILLER.key(), new FixedMetadataValue(api.getPlugin(), killer));
        }
    }

    enum GamePhase {
        PREGAME, INVINCIBILITY, MAIN, BORDER, POSTGAME;

        String prettyName() {
            return Trans.late(StringUtils.capitalize(name().toLowerCase()));
        }
    }

    protected class Respawner {
        int respawnTime = 8;
        Map<Player, Long> watching = new HashMap<>();

        void add(Player player) {
            watching.put(player, System.currentTimeMillis());
        }

        void tick() {
            if (watching.isEmpty())
                return;
            Iterator<Map.Entry<Player, Long>> iter = watching.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Player, Long> e = iter.next();
                if (!e.getKey().getWorld().equals(getWorld())) {
                    // player left before respawning
                    iter.remove();
                    continue;
                }
                if (System.currentTimeMillis() - e.getValue() > respawnTime * 1000L) {
                    iter.remove();
                    api.logr().debug("Respawning %s", e.getKey().getName());
                    respawn(e.getKey());
                    continue;
                }
                int timeLeft = respawnTime - ((int) (System.currentTimeMillis() - e.getValue()) / 1000);
                e.getKey().showTitle(Title.title(
                        Trans.lateToComponent("&c&lYou Died!"),
                        Trans.lateToComponent("&eYou will respawn in &f%s &eseconds", timeLeft),
                        Title.Times.of(Duration.ofSeconds(0), Duration.ofSeconds(1), Duration.ofSeconds(0))));
            }
        }

        private void respawn(Player player) {
            Tools.uncheckedTeleport(player, Game.this.randomSpawnPoint());
            doThisInOneTick(b -> {
                if (getOptions().isAllowRegularBuilding())
                    player.setGameMode(GameMode.SURVIVAL);
                else
                    player.setGameMode(GameMode.ADVENTURE);
            });
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setInvulnerable(false);
            api.getPlayerManager().clearInventoryAndRestoreKit(player);
        }
    }
}
