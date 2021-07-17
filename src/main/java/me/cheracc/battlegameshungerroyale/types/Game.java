package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.events.CustomEventsListener;
import me.cheracc.battlegameshungerroyale.events.GameDamageEvent;
import me.cheracc.battlegameshungerroyale.events.GameDeathEvent;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.LootManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class Game implements Listener {
    private final MapData map;
    private GameLog gameLog;
    private final GameOptions options;
    private final BossBar bar;
    private World world;
    private LootManager lootManager;
    private final Scoreboard scoreboard;

    private boolean openToPlayers;

    private double postgameTime;
    private double pregameTime;
    private double gameTime;
    private long startTime;
    private Player winner = null;
    private BukkitTask gameTick = null;
    private BukkitTask pregameTimer = null;
    private BukkitTask postgameTimer = null;
    private BukkitTask respawner = null;
    private final Map<UUID, Integer> participants = new HashMap<>();
    private final List<Location> spawnPoints = new ArrayList<>();
    private GamePhase currentPhase;
    private long lastChestRespawn;
    enum GamePhase { PREGAME, INVINCIBILITY, MAIN, BORDER, POSTGAME }

    private Game(MapData map, GameOptions options, Consumer<Game> callback) {
        currentPhase = GamePhase.PREGAME;
        this.map = map;
        this.options = options;
        this.lootManager = new LootManager(this);
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        setupScoreboard();
        bar = Bukkit.createBossBar("Pregame", BarColor.WHITE, BarStyle.SOLID);
        bar.setVisible(true);
        openToPlayers = false;
        pregameTime = -1;
        gameTime = -1;
        postgameTime = -1;

        long time = System.currentTimeMillis();
        MapManager.getInstance().createNewWorldAsync(map, w -> {
            setGameWorld(w);
            setupGame();
            Bukkit.getLogger().info("started new game. elapsed time: " + (System.currentTimeMillis() - time));
            gameLog = new GameLog(this);
            gameLog.addPhaseEntry(currentPhase);
            GameManager.getInstance().updateScoreboard();
            if (callback != null)
                callback.accept(this);
        });
        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    // public methods
    public void setupGame() {
        openToPlayers = true;
        spawnPoints.addAll(getSpawnPoints(map.getBorderRadius() / 10));
        if (map.isUseBorder() && map.getBorderRadius() > 0) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(map.getBorderCenter(world));
            border.setSize(map.getBorderRadius() * 2);
        }
        // always
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);

        // for pregame only
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);


        pregameTimer = startPregameTimer();
        GameManager.getInstance().setupGame(this);
    }

    public void startGame() {
        CustomEventsListener.getInstance();
        openToPlayers = false;
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, true);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        lootManager.placeLootChests((int) (getActivePlayers().size() * 5 * Math.sqrt(getMap().getBorderRadius())));
        lastChestRespawn = startTime = System.currentTimeMillis();


        if (options.getStartType() == GameOptions.StartType.ELYTRA)
            doElytraSpawn(success -> {
                gameTick = startGameTick();
                gameLog.addPhaseEntry(currentPhase);
                currentPhase = GamePhase.INVINCIBILITY;
            });
        else if (options.getStartType() == GameOptions.StartType.HUNGERGAMES) {
            doHungergamesSpawn(success -> {
                gameTick = startGameTick();
                gameLog.addPhaseEntry(currentPhase);
                currentPhase = GamePhase.INVINCIBILITY;
            });
        }
        for (Player p : getActivePlayers()) {
            p.setInvulnerable(true);
            //p.setBedSpawnLocation(p.getLocation(), true);
        }
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public int getCurrentGameTime() {
        return (int) gameTime;
    }

    public double getPostgameTime() {
        return postgameTime;
    }

    public double getPregameTime() {
        return pregameTime;
    }

    public String getPhase() {
        return StringUtils.capitalize(currentPhase.name().toLowerCase());
    }

    public MapData getMap() {
        return map;
    }

    public World getWorld() {
        return world;
    }

    public boolean isOpenToPlayers() {
        return openToPlayers;
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
        player.teleport(map.getSpawnCenter(world), PlayerTeleportEvent.TeleportCause.PLUGIN);
        bar.addPlayer(player);
    }

    public void join(Player player) {
        participants.put(player.getUniqueId(), options.getLivesPerPlayer());
        player.setGameMode(GameMode.ADVENTURE);
        Location spawnPoint = spawnPoints.get(ThreadLocalRandom.current().nextInt(spawnPoints.size() - 1));
        player.teleport(spawnPoint, PlayerTeleportEvent.TeleportCause.PLUGIN);
        bar.addPlayer(player);
        if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.INVINCIBILITY)
            player.setInvulnerable(true);
        gameLog.addLogEntry(String.format("%s joined (%s/%s)", player.getName(), getActivePlayers().size(), getStartingPlayersSize()));

        if (PlayerManager.getInstance().getPlayerData(player).getKit() == null) {
            player.sendMessage(Tools.componentalize("&fYou haven't selected a kit! Type &e/kitmenu &for &e/kit <name> &fto select one!"));
        } else {
            PlayerManager.getInstance().getPlayerData(player).getKit().outfitPlayer(player);
        }
        player.setScoreboard(scoreboard);
    }

    public void quit(Player player) {
        if (currentPhase == GamePhase.PREGAME) {
            participants.remove(player.getUniqueId());
        }
        if (participants.containsKey(player.getUniqueId()) && participants.get(player.getUniqueId()) > 0) {
            participants.replace(player.getUniqueId(), 0);
            gameLog.addLogEntry(String.format("%s left (%s/%s)", player.getName(), getActivePlayers().size(), getStartingPlayersSize()));
        }
        PlayerData data = PlayerManager.getInstance().getPlayerData(player);
        player.setAllowFlight(false);
        if (BGHR.getPlugin().getConfig().getBoolean("main world.place players at spawn on join", false))
            player.teleport(MapManager.getInstance().getLobbyWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        else
            player.teleport(data.getLastLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        bar.removePlayer(player);
        if (PlayerManager.getInstance().getPlayerData(player).getSettings().isShowMainScoreboard())
            player.setScoreboard(GameManager.getInstance().getMainScoreboard());
        else
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public Player getWinner() {
        if (currentPhase != GamePhase.POSTGAME)
            return null;

        if (this.winner != null)
            return this.winner;

        Player winner = null;
        for (Map.Entry<UUID, Integer> e : participants.entrySet()) {
            if (e.getValue() > 0) {
                if (winner != null) {
                    Bukkit.getLogger().info("tried to find a winner but there seems to be more than one");
                    return null;
                }
                winner = Bukkit.getPlayer(e.getKey());
            }
        }
        this.winner = winner;
        return winner;
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

        players.addAll(world.getPlayers());
        return players;
    }

    public Set<Player> getActivePlayers() {
        Set<Player> players = new HashSet<>();

        for (Map.Entry<UUID, Integer> e : participants.entrySet()) {
            if (e.getValue() > 0) { // these will be players with lives remaining
                Player p = Bukkit.getPlayer(e.getKey());
                if (p != null && p.isOnline() && isPlaying(p))
                    players.add(p);
                else { // where'd they go?
                    Bukkit.getLogger().info(String.format("%s has %s lives but is not here", e.getKey().toString(), participants.get(e.getKey())));
                    quit(p);
                }
            }
        }
        return players;
    }

    public int getTimeLeftInCurrentPhase() {
        switch (currentPhase) {
            case PREGAME:
                return getOptions().getPregameTime() - (int) pregameTime;
            case INVINCIBILITY:
                return getOptions().getInvincibilityTime() - (int) gameTime;
            case MAIN:
                return getOptions().getMainPhaseTime() + getOptions().getInvincibilityTime() - (int) gameTime;
            case BORDER:
                return getOptions().getBorderTime() + getOptions().getInvincibilityTime() +
                        getOptions().getMainPhaseTime() - (int) gameTime;
            default:
                return -1;
        }
    }

    // private methods
    private void setGameWorld(World world) {
        this.world = world;
    }

    private List<Location> getSpawnPoints(int number) {
        Location center = map.getSpawnCenter(world);
        int scanRadius = map.getSpawnRadius() + 1;
        int startSize = spawnPoints.size();

        if (spawnPoints.isEmpty() && map.getSpawnBlockType() != null) {
            for (int x = -scanRadius; x <= scanRadius; x++) {
                for (int y = -scanRadius; y <= scanRadius; y++)
                    for (int z = -scanRadius; z <= scanRadius; z++) {
                        Location l = center.clone().add(x, y, z);
                        Block b = l.getBlock();

                        if (b.getType() == map.getSpawnBlockType()) {
                            l.add(0.5, 1, 0.5);
                            l.setDirection(l.clone().subtract(center).toVector().multiply(-1));
                            spawnPoints.add(l);
                            Bukkit.getLogger().info(String.format("registered spawn block(%s)@(%.0f,%.0f,%.0f)", b.getType().name(), l.getX(), l.getY(), l.getZ()));
                        }
                    }
            }
            if (spawnPoints.size() > 0)
                Bukkit.getLogger().info("found " + spawnPoints.size() + " spawn points on " + map.getSpawnBlockType().name());
        }


        int spawnPointsNeeded = Math.max(getActivePlayers().size(), number);
        if (spawnPoints.size() < spawnPointsNeeded) {

            for (int i = 0; i < spawnPointsNeeded; i++) {
                double angle = (Math.PI * 2 * i) / spawnPointsNeeded;
                int radius = map.getSpawnRadius();
                Location spawnPoint = center.clone().add(radius * Math.cos(angle), 2, radius * Math.sin(angle)).toHighestLocation().add(0,2,0);
                spawnPoint.setDirection(spawnPoint.clone().subtract(center).toVector().multiply(-1));
                spawnPoints.add(spawnPoint);
            }
        }
        Bukkit.getLogger().info(String.format("created %s spawn points around defined spawn center", spawnPoints.size() - startSize));
        return spawnPoints;
    }

    private void doElytraSpawn(Consumer<Boolean> callback) {
        Vector boost = new Vector(0, 1, 0);

        if (spawnPoints.size() < getActivePlayers().size()) {
            Bukkit.getLogger().warning("not enough spawns");
            getSpawnPoints(getActivePlayers().size());
        }

        Collections.shuffle(spawnPoints);
        int count = 0;
        for (Player p : getActivePlayers()) {
            p.teleport(spawnPoints.get(count), PlayerTeleportEvent.TeleportCause.PLUGIN);
            count++;
        }

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                for (Player p : getActivePlayers()) {
                    if (!isPlaying(p))
                        return;

                    if (count == 0) {
                        p.setAllowFlight(true);
                        p.setVelocity(boost);
                    } else {
                        p.setVelocity(p.getVelocity().add(boost));
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1F, 1F + (0.1F * 2 * count));

                    if (count == 2) {
                        ItemStack elytra = new ItemStack(Material.ELYTRA);
                        elytra.addEnchantment(Enchantment.BINDING_CURSE, 1);
                        p.getInventory().setChestplate(elytra);
                        p.setGliding(true);
                    }
                    if (count >= 4) {
                        cancel();
                        callback.accept(true);
                    }
                }
                count++;
            }
        }.runTaskTimer(BGHR.getPlugin(), 40L, 4L);
    }

    private void doHungergamesSpawn(Consumer<Boolean> callback) {
        List<Location> spawns = getSpawnPoints(getActivePlayers().size());

        if (spawns.size() < getActivePlayers().size()) {
            Bukkit.getLogger().warning("not enough spawns");
            getSpawnPoints(getActivePlayers().size());
        }

        Collections.shuffle(spawns);
        int count = 0;

        PotionEffect noMove = new PotionEffect(PotionEffectType.SLOW, 50, 99999, false, false, false);
        PotionEffect noJump = new PotionEffect(PotionEffectType.JUMP, 50, -99999, false, false, false);
        for (Player p : getActivePlayers()) {
            p.teleport(spawns.get(count), PlayerTeleportEvent.TeleportCause.PLUGIN);
            p.addPotionEffect(noMove);
            p.addPotionEffect(noJump);
            count++;
        }
        callback.accept(true);
    }

    private void updateBossBar() {
        int[] length = { options.getPregameTime(), options.getInvincibilityTime(), options.getMainPhaseTime(), options.getBorderTime(), options.getPostGameTime() };
        BarColor[] color = { BarColor.WHITE, BarColor.PINK, BarColor.BLUE, BarColor.RED, BarColor.GREEN };
        String[] names = { "Pregame", "Invincibility", "Main Phase", "Border Shrinking", "Postgame" };

        bar.setColor(color[currentPhase.ordinal()]);
        bar.setTitle(names[currentPhase.ordinal()]);

        double phaseProgress;
        if (currentPhase == GamePhase.PREGAME)
            phaseProgress = options.getPregameTime() - pregameTime;
        else if (currentPhase == GamePhase.POSTGAME) {
            phaseProgress = options.getPostGameTime() - postgameTime;
        }
        else {
            phaseProgress = gameTime;
            if (currentPhase.ordinal() > 1)
                for (int i = 1; i < currentPhase.ordinal(); i++) {
                    phaseProgress -= length[i];
                }
        }
        phaseProgress = Math.max(0, Math.min(1, phaseProgress/length[currentPhase.ordinal()]));

        bar.setProgress(1 - phaseProgress);
    }

    private boolean checkForWinner() {
        Player winner = null;
        for (Map.Entry<UUID, Integer> e : participants.entrySet()) {
            if (e.getValue() > 0) {
                if (winner != null) {
                    return false;
                }
                winner = Bukkit.getPlayer(e.getKey());
            }
        }
        if (winner != null) {
            doPostGame();
            gameLog.addLogEntry(winner.getName() + " won!");
        }
        return true;
    }

    private void startMainPhase() {
        currentPhase = GamePhase.MAIN;
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
            if (options.getStartType() == GameOptions.StartType.ELYTRA && p.getInventory().getChestplate() != null &&
                    p.getInventory().getChestplate().getType().equals(Material.ELYTRA)) {
                p.getInventory().setChestplate(null);
                p.setGliding(false);
                p.setAllowFlight(false);
            }
        }
        for (UUID id : toRemove)
            participants.remove(id);
        gameLog.addPhaseEntry(currentPhase);
        GameManager.getInstance().updateScoreboard();
    }

    private void startBorderPhase() {
        currentPhase = GamePhase.BORDER;
        world.getWorldBorder().setSize(10, options.getBorderTime());
        GameManager.getInstance().updateScoreboard();
        gameLog.addPhaseEntry(currentPhase);
    }

    private void doPostGame() {
        map.addGamePlayed((int) gameTime);
        gameTick.cancel();
        respawner.cancel();
        postgameTimer = startPostGameTimer();
        currentPhase = GamePhase.POSTGAME;
        GameManager.getInstance().updateScoreboard();
        gameLog.addPhaseEntry(currentPhase);
        world.getWorldBorder().setSize(world.getWorldBorder().getSize() + 4);
    }

    public void endGame() {
        if (gameTick != null) {
            gameTick.cancel();
            gameTick = null;
        }
        if (pregameTimer != null) {
            pregameTimer.cancel();
            pregameTimer = null;
        }
        if (postgameTimer != null) {
            postgameTimer.cancel();
            postgameTimer = null;
        }
        if (respawner != null) {
            respawner.cancel();
            respawner = null;
        }

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
        gameTick = null;
        pregameTimer = null;
        postgameTimer = null;
        participants.clear();
        spawnPoints.clear();
        MapManager.getInstance().unloadWorld(world);
        GameManager.getInstance().gameOver(this);
        HandlerList.unregisterAll(this);
        world = null;
    }

    private void setupScoreboard() {
        Objective obj = scoreboard.registerNewObjective("main", "dummy", Tools.componentalize(""));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 15; i >= 0; i--) {
            String entry = ChatColor.values()[i] + "" + ChatColor.values()[i+1];
            Team lineText = scoreboard.registerNewTeam(String.format("line%s", i));
            lineText.addEntry(entry);
            obj.getScore(entry).setScore(i);
        }
    }

    private void updateScoreboard() {
        Objective obj = scoreboard.getObjective("main");
        obj.displayName(Tools.componentalize(String.format("&f&l%s", map.getMapName())));
        setScoreboardLine(15, "&6Phase: &e" + getPhase());
        setScoreboardLine(14, String.format("&6Border Size: &e%s", (int) getWorld().getWorldBorder().getSize()));
        if (gameTime > 0)
            setScoreboardLine(13, String.format("&6Time Elapsed: &e%s", Tools.secondsToAbbreviatedMinsSecs((int) gameTime)));
        else
            setScoreboardLine(13, String.format("&6Starts In: &e%s", (int) pregameTime));
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

    private void setScoreboardLine(int line, String text) {
        scoreboard.getTeam("line" + line).prefix(Tools.componentalize(text));
    }

    // game tasks
    private BukkitTask startPostGameTimer() {
        postgameTime = options.getPostGameTime();

        BukkitRunnable postGameTimer = new BukkitRunnable() {
            long last = System.currentTimeMillis();
            @Override
            public void run() {
                postgameTime -= (System.currentTimeMillis() - last) / 1000D;

                if (postgameTime <= 0) {
                    cancel();
                    endGame();
                    return;
                }
                updateBossBar();
                last = System.currentTimeMillis();
            }
        };

        return postGameTimer.runTaskTimer(BGHR.getPlugin(), 20L, 4L);
    }

    private BukkitTask startPregameTimer() {
        BukkitRunnable pregameTimer = new BukkitRunnable() {
            double last = System.currentTimeMillis();
            @Override
            public void run() {
                if (currentPhase != GamePhase.PREGAME)
                    cancel();

                if (pregameTime <= 0) {
                    // check if there's enough players to start
                    if (getActivePlayers().size() >= options.getPlayersNeededToStart()) {
                        cancel();
                        startGame();
                    }
                    else {
                        pregameTime = options.getPregameTime();
                    }

                } else
                    pregameTime -= (System.currentTimeMillis() - last) / 1000D;

                updateBossBar();
                updateScoreboard();
                last = System.currentTimeMillis();
            }
        };
        return pregameTimer.runTaskTimer(BGHR.getPlugin(), 20L, 4L);
    }

    private BukkitTask startGameTick() {
        respawner = respawner();

        BukkitRunnable task = new BukkitRunnable() {
            long last = System.currentTimeMillis();
            @Override
            public void run() {
                gameTime += (System.currentTimeMillis() - last) / 1000D;

                if (checkForWinner())
                    cancel();

                if (currentPhase == GamePhase.INVINCIBILITY && gameTime >= options.getInvincibilityTime()) {
                    startMainPhase();
                    GameManager.getInstance().updateScoreboard();
                }
                if (currentPhase == GamePhase.MAIN && gameTime >= options.getInvincibilityTime() + options.getMainPhaseTime()) {
                    startBorderPhase();
                    GameManager.getInstance().updateScoreboard();
                }

                if ((System.currentTimeMillis() - lastChestRespawn)/1000/60 >= options.getChestRespawnTime()) {
                    lootManager.placeLootChests((int) (getActivePlayers().size() * 5 * Math.sqrt(getMap().getBorderRadius())));
                    lastChestRespawn = System.currentTimeMillis();
                }

                updateBossBar();
                updateScoreboard();
                last = System.currentTimeMillis();
            }
        };
        return task.runTaskTimer(BGHR.getPlugin(), 20L, 4L);
    }

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
                        }
                        else {
                            theDeceased.put(p.getUniqueId(), System.currentTimeMillis());
                        }
                    }
                    else theDeceased.remove(p.getUniqueId());
                }
            }
        };
        return task.runTaskTimer(BGHR.getPlugin(), getOptions().getInvincibilityTime(), 10L);
    }

    // static fields and methods
    public static void createNewGameWithCallback(MapData map, GameOptions options, Consumer<Game> callback) {
        new Game(map, options, callback);
    }

    public static void createNewGame(MapData map, GameOptions options) {
        new Game(map, options, null);
    }

    // listeners
    @EventHandler
    public void handleQuits(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        p.setAllowFlight(false);
        if (isSpectating(p) || isPlaying(p))
            quit(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (isPlaying(event.getPlayer())) {
            event.setRespawnLocation(world.getSpawnLocation());
            event.getPlayer().setGameMode(GameMode.ADVENTURE);
        }
        if (isSpectating(event.getPlayer())) {
            event.setRespawnLocation(world.getSpawnLocation());
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

        gameLog.addDamageEntry(event);
    }

    @EventHandler
    public void handleDeaths(GameDeathEvent event) {
        Player dead = event.getRecentlyDeceased();
        Game game = GameManager.getInstance().getPlayersCurrentGame(dead);

        if (game  != null && game.equals(this)) {
            gameLog.addDeathEntry(dead);

            if (participants.containsKey(dead.getUniqueId())) {
                int livesLeft = Math.max(0, participants.get(dead.getUniqueId()) - 1);

                participants.replace(dead.getUniqueId(), livesLeft);
                if (livesLeft > 0)
                    dead.sendMessage(Tools.componentalize("&cYou died! &fYou may respawn &e" + livesLeft + " &fmore time. &eYou will be &aautomatically respawned &ein &a5 &eseconds." + ((livesLeft > 1) ? "s" : "")));
                else
                    dead.sendMessage(Tools.componentalize("&cYou died! &fYou have no respawns left."));
            }
        }
    }

}
