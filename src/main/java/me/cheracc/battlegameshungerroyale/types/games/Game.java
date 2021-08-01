package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.events.*;
import me.cheracc.battlegameshungerroyale.managers.LootManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.MapData;
import me.cheracc.battlegameshungerroyale.types.Metadata;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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
    protected BghrApi api;
    protected GamePhase currentPhase;
    protected LootManager lootManager;
    private World world;
    private GameLog gameLog;
    private boolean openToPlayers;
    private double gameTime;
    private long startTime;
    private long lastChestRespawn;
    private Player winner = null;

    public Game(GameOptions options) {
        currentPhase = GamePhase.PREGAME;
        this.map = options.getMap();
        this.options = options;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.participants = new HashMap<>();
        this.spawnPoints = new ArrayList<>();
        this.tasks = new ArrayList<>();
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
        gameTime = -Integer.MAX_VALUE;
    }

    public abstract String getGameTypeName();
    public abstract String getGameDescription();
    public abstract Material getGameIcon();

    // to allow overrides if necessary
    protected void runGameSetupProcedures() {
    }

    protected void runExtraMainPhaseProcedures() {
    }

    // this is called by the game manager after the world is copied and loaded
    public void initializeGame(World world, BghrApi api) {
        Bukkit.getPluginManager().registerEvents(this, api.getPlugin());
        this.world = world;
        this.api = api;
        gameLog = new GameLog(this, api.getPlugin());
        gameLog.addLogEntry("Starting " + options.getConfigFile().getName());
        if (options.isGenerateChests() || options.isFillAllChests())
            this.lootManager = new LootManager(this, api.getPlugin(), api.logr(), api.getGameManager());
        else
            this.lootManager = null;
        openToPlayers = true;
        spawnPoints.addAll(getSpawnPoints(map.getBorderRadius() / 10));
        if (map.isUseBorder() && map.getBorderRadius() > 0) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(map.getBorderCenter(world));
            border.setSize(map.getBorderRadius() * 2);
        }
        // always
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setDifficulty(Difficulty.NORMAL);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);

        // for pregame only
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

        api.getGameManager().makeThisGameAvailable(this);
        tasks.add(startPregameTimer().runTaskTimer(api.getPlugin(), 20L, 4L));
        runGameSetupProcedures();

        new GameLoadedEvent(this).callEvent();
    }

    // returns the full class name
    public String getGameType() {
        return getClass().getName();
    }

    private void startGame() {
        openToPlayers = false;
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, true);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        lastChestRespawn = startTime = System.currentTimeMillis();

        if (this instanceof InvincibilityPhase) {
            ((InvincibilityPhase) this).startInvincibilityPhase();
            currentPhase = GamePhase.INVINCIBILITY;
            for (Player p : getActivePlayers()) {
                p.setInvulnerable(true);
                PlayerData data = api.getPlayerManager().getPlayerData(p);
                if (data.getKit() == null || (!data.getKit().isEnabled() && !p.hasPermission("bghr.admin.kits.disabled"))) {
                    data.registerKit(api.getKitManager().getRandomKit(false), false);
                }
            }
        } else {
            startMainPhase();
        }
        new GameStartEvent(this).callEvent();
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

    public void setOpenToPlayer(boolean value) {
        openToPlayers = value;
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

    private Location randomSpawnPoint() {
        Location loc = getSpawnPoints(getActivePlayers().size()).get(ThreadLocalRandom.current().nextInt(spawnPoints.size() - 1));

        int tries = 0;
        while (!loc.getNearbyPlayers(1).isEmpty() && tries < 10) {
            loc = getSpawnPoints(getActivePlayers().size()).get(ThreadLocalRandom.current().nextInt(spawnPoints.size() - 1));
            tries++;
        }
        return loc;
    }

    public void join(Player player) {
        Metadata.removeAll(player);
        participants.put(player.getUniqueId(), options.getLivesPerPlayer());
        if (options.isAllowRegularBuilding())
            player.setGameMode(GameMode.SURVIVAL);
        else
            player.setGameMode(GameMode.ADVENTURE);
        player.teleport(randomSpawnPoint(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        bar.addPlayer(player);
        if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.INVINCIBILITY)
            player.setInvulnerable(true);

        if (api.getPlayerManager().getPlayerData(player).getKit() == null) {
            player.sendMessage(Trans.lateToComponent("&fYou haven't selected a kit! Type &e/kit &fto open the menu!"));
        } else {
            api.getPlayerManager().outfitPlayer(player, api.getPlayerManager().getPlayerData(player).getKit());
        }
        if (api.getPlayerManager().getPlayerData(player).getSettings().isShowGameScoreboard()) {
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
        if (participants.containsKey(player.getUniqueId()) && participants.get(player.getUniqueId()) > 0) {
            livesRemaining = participants.get(player.getUniqueId());
            participants.replace(player.getUniqueId(), 0);
        }
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
        player.setAllowFlight(false);
        player.setInvulnerable(false);
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
            if (lives > 0)
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

    protected List<Location> getSpawnPoints(int number) {
        Location center = map.getSpawnCenter(world);
        int scanRadius = map.getSpawnRadius() + 1;
        int startSize = spawnPoints.size();

        // TODO - get a large number of positions (~200 maybe), remove the ones that aren't 'spawnable', then return
        // TODO a list that is evenly spaced about those positions (every n/rth)

        if (startSize < number && map.getSpawnBlockType() != null) {
            for (int x = -scanRadius; x <= scanRadius; x++) {
                for (int y = -scanRadius; y <= scanRadius; y++)
                    for (int z = -scanRadius; z <= scanRadius; z++) {
                        Location l = center.clone().add(x, y, z);
                        Block b = l.getBlock();

                        if (b.getType() == map.getSpawnBlockType()) {
                            l.add(0.5, 1, 0.5);
                            l.setDirection(l.clone().subtract(center).toVector().multiply(-1));
                            spawnPoints.add(l);
                        }
                    }
            }
            if (spawnPoints.size() > 0)
                api.logr().info("Found " + spawnPoints.size() + " spawn points on " + map.getSpawnBlockType().name());
        }

        int spawnPointsNeeded = Math.max(getActivePlayers().size(), number);
        int extra = 2;
        if (spawnPoints.size() < spawnPointsNeeded) {
            for (int i = 0; i < spawnPointsNeeded + extra; i++) {
                double angle = (Math.PI * 2 * i) / spawnPointsNeeded + Math.PI / (ThreadLocalRandom.current().nextInt(16, 32));
                int radius = map.getSpawnRadius();
                Location spawnPoint = center.clone().add(radius * Math.cos(angle), 2, radius * Math.sin(angle)).toHighestLocation().add(0, 1, 0);
                if (Math.abs(spawnPoint.getY() - center.getY()) > 5) {
                    extra++;
                    continue;
                }
                spawnPoint.setDirection(spawnPoint.clone().subtract(center).toVector().multiply(-1));
                spawnPoints.add(spawnPoint);
            }
        }
        return spawnPoints;
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
        if (this instanceof InvincibilityPhase)
            ((InvincibilityPhase) this).endInvincibilityPhase();

        currentPhase = GamePhase.MAIN;
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
        runExtraMainPhaseProcedures();
    }

    private void doPostGame() {
        map.addGamePlayed((int) gameTime);
        new GameFinishedEvent(this, getWinner()).callEvent();
        tasks.add(startPostGameTimer());
        currentPhase = GamePhase.POSTGAME;
        new GameChangedPhaseEvent(this, "postgame").callEvent();
        api.getGameManager().gameIsEnding();
        world.getWorldBorder().setSize(world.getWorldBorder().getSize() + 4);
    }

    // just ends the game - used at end of postgame and when disabling the plugin
    public void endGame() {
        endGame(null);
    }

    // ends the game and calls back when everything is unloaded
    public void endGame(Consumer<Game> callback) {
        for (BukkitTask task : tasks)
            task.cancel();

        if (lootManager != null)
            lootManager.close();
        gameLog.finalizeLog();
        for (Player p : getCurrentPlayersAndSpectators())
            quit(p);

        if (winner != null) {
            map.addGamePlayed((int) (System.currentTimeMillis() - startTime / 1000));
        }

        bar.setVisible(false);
        bar.removeAll();

        gameLog = null;
        lootManager = null;

        winner = null;
        participants.clear();
        spawnPoints.clear();
        api.getMapManager().unloadWorld(world);
        api.getGameManager().gameOver(this, callback);
        HandlerList.unregisterAll(this);
        world = null;
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
                if (currentPhase != GamePhase.PREGAME)
                    cancel();

                if (gameTime >= 0) {
                    // check if there's enough players to start
                    if (getActivePlayers().size() >= options.getPlayersNeededToStart()) {
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

    public long getLastChestRespawn() {
        return lastChestRespawn;
    }

    public void setLastChestRespawn(long value) {
        lastChestRespawn = value;
    }

    BukkitTask startGameTick() {
        tasks.add(respawner());

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (checkForWinner() || currentPhase == GamePhase.POSTGAME) {
                    cancel();
                    return;
                }

                gameTime = (System.currentTimeMillis() - startTime) / 1000D;

                if (currentPhase == GamePhase.INVINCIBILITY && gameTime >= options.getInvincibilityTime()) {
                    startMainPhase();
                }
                if (Game.this instanceof BorderPhase && currentPhase == GamePhase.MAIN && gameTime >= options.getInvincibilityTime() + options.getMainPhaseTime()) {
                    ((BorderPhase) Game.this).startBorderPhase(Game.this);
                }

                if (lootManager != null) {
                    if ((System.currentTimeMillis() - lastChestRespawn) / 1000 / 60 >= options.getChestRespawnTime()) {
                        // TODO add the ability to modify this 'density' number for loot chests
                        lootManager.placeLootChests((int) (getActivePlayers().size() * 5 * Math.sqrt(getMap().getBorderRadius())));
                        lastChestRespawn = System.currentTimeMillis();
                    }
                }

                updateBossBar();
                updateScoreboard();
            }
        };
        return task.runTaskTimer(api.getPlugin(), 20L, 4L);
    }

    // respawns dead players after 5 seconds
    // TODO - make it instantly respawn, set to spectator, put a few blocks above place of death facing down
    // TODO use title/subtitle to display a counter that will count down to respawn
    // TODO customizable ways to respawn (elytra, random, place of death, etc)
    private BukkitTask respawner() {
        Map<UUID, Long> theDeceased = new HashMap<>();
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : getActivePlayers()) {
                    if (p.isDead()) {
                        if (theDeceased.containsKey(p.getUniqueId())) {
                            if (System.currentTimeMillis() - theDeceased.get(p.getUniqueId()) > 5000) {
                                p.spigot().respawn();
                                theDeceased.remove(p.getUniqueId());
                            }
                        } else {
                            theDeceased.put(p.getUniqueId(), System.currentTimeMillis());
                        }
                    } else theDeceased.remove(p.getUniqueId());
                }
            }
        };
        return task.runTaskTimer(api.getPlugin(), getOptions().getInvincibilityTime(), 10L);
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

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (isPlaying(event.getPlayer())) {
            event.setRespawnLocation(randomSpawnPoint());
            if (getOptions().isAllowRegularBuilding())
                event.getPlayer().setGameMode(GameMode.SURVIVAL);
            else
                event.getPlayer().setGameMode(GameMode.ADVENTURE);
            api.getPlayerManager().outfitPlayer(event.getPlayer(), api.getPlayerManager().getPlayerData(event.getPlayer()).getKit());
        }
        if (isSpectating(event.getPlayer())) {
            event.setRespawnLocation(randomSpawnPoint());
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
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
        Game game = api.getGameManager().getPlayersCurrentGame(dead);

        if (game != null && game.equals(this)) {
            if (participants.containsKey(dead.getUniqueId())) {
                int livesLeft = Math.max(0, participants.get(dead.getUniqueId()) - 1);

                participants.replace(dead.getUniqueId(), livesLeft);
                if (livesLeft > 0)
                    dead.sendMessage(Trans.lateToComponent("&cYou died! &fYou may respawn &e%s &fmore time%s. You will be &eautomatically respawned &e in &a5 &eseconds.", livesLeft, livesLeft > 1 ? "s" : ""));
                else
                    dead.sendMessage(Tools.componentalize("&cYou died! &fYou have no respawns left."));
            }
        }
    }

    enum GamePhase {
        PREGAME, INVINCIBILITY, MAIN, BORDER, POSTGAME;

        String prettyName() {
            return Trans.late(StringUtils.capitalize(name().toLowerCase()));
        }
    }
}
