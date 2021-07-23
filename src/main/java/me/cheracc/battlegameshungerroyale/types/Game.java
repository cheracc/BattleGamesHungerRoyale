package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.events.*;
import me.cheracc.battlegameshungerroyale.managers.*;
import me.cheracc.battlegameshungerroyale.tools.Logr;
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
import org.bukkit.metadata.FixedMetadataValue;
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
    private final BGHR plugin;
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

    private Game(MapData map, GameOptions options, BGHR plugin, Consumer<Game> callback) {
        currentPhase = GamePhase.PREGAME;
        this.plugin = plugin;
        this.map = map;
        this.options = options;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        setupScoreboard();
        bar = Bukkit.createBossBar("Pregame", BarColor.WHITE, BarStyle.SOLID);
        bar.setVisible(true);
        openToPlayers = false;
        pregameTime = -1;
        gameTime = -1;
        postgameTime = -1;

        MapManager.getInstance().createNewWorldAsync(map, w -> {
            setGameWorld(w);
            setupGame();
            gameLog = new GameLog(this);
            gameLog.addPhaseEntry(currentPhase);
            this.lootManager = new LootManager(this);
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
        new GameLoadedEvent(this).callEvent();
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
                world.setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, false);
                new GameChangedPhaseEvent(this, "invincibility").callEvent();
            });
        else if (options.getStartType() == GameOptions.StartType.HUNGERGAMES) {
            doHungergamesSpawn(success -> {
                gameTick = startGameTick();
                gameLog.addPhaseEntry(currentPhase);
                currentPhase = GamePhase.INVINCIBILITY;
                new GameChangedPhaseEvent(this, "invincibility").callEvent();
            });
        }
        for (Player p : getActivePlayers()) {
            p.setInvulnerable(true);
            PlayerData data = PlayerManager.getInstance().getPlayerData(p);
            if (data.getKit() == null || (!data.getKit().isEnabled() && !p.hasPermission("bghr.admin.kits.disabled"))) {
                data.registerKit(KitManager.getInstance().getRandomKit(false), false);
            }
        }
        new GameStartEvent(this).callEvent();
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

    public int getLivesLeft(UUID uuid) {
        if (participants.containsKey(uuid))
            return participants.get(uuid);
        return 0;
    }

    public boolean isActive() {
        return gameTime > 0 && currentPhase != GamePhase.POSTGAME && !gameTick.isCancelled();
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
        new PlayerJoinedGameEvent(player, this, true).callEvent();
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
            player.sendMessage(Tools.componentalize("&fYou haven't selected a kit! Type &e/kit &for let the gods of randomness have their say!"));
        } else {
            PlayerManager.getInstance().getPlayerData(player).getKit().outfitPlayer(player);
        }
        player.setScoreboard(scoreboard);
        player.setMetadata("pregame-health", new FixedMetadataValue(plugin, player.getHealth()));
        player.setHealth(20);
        player.setMetadata("pregame-foodlevel", new FixedMetadataValue(plugin, player.getFoodLevel()));
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
            gameLog.addLogEntry(String.format("%s left (%s/%s)", player.getName(), getActivePlayers().size(), getStartingPlayersSize()));
        }
        new PlayerQuitGameEvent(player, this, livesRemaining).callEvent();
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

        if (player.hasMetadata("pregame-health")) {
            player.setHealth((double) player.getMetadata("pregame-health").get(0).value());
        }
        if (player.hasMetadata("pregame-foodlevel")) {
            player.setFoodLevel((int) player.getMetadata("pregame-foodlevel").get(0).value());
        }
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
                Logr.info("Found " + spawnPoints.size() + " spawn points on " + map.getSpawnBlockType().name());
        }


        int spawnPointsNeeded = Math.max(getActivePlayers().size(), number);
        int extra = 2;
        if (spawnPoints.size() < spawnPointsNeeded) {
            for (int i = 0; i < spawnPointsNeeded + extra; i++) {
                double angle = (Math.PI * 2 * i) / spawnPointsNeeded + Math.PI/(ThreadLocalRandom.current().nextInt(16,32));
                int radius = map.getSpawnRadius();
                Location spawnPoint = center.clone().add(radius * Math.cos(angle), 2, radius * Math.sin(angle)).toHighestLocation().add(0,1,0);
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

    private void doElytraSpawn(Consumer<Boolean> callback) {
        Vector boost = new Vector(0, 1, 0);

        if (spawnPoints.size() < getActivePlayers().size()) {
            Bukkit.getLogger().warning("not enough spawns");
            getSpawnPoints(getActivePlayers().size());
        }

        world.setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);
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
                        p.setVelocity(boost);
                    } else {
                        p.setVelocity(p.getVelocity().add(boost));
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1F, 1F + (0.1F * 2 * count));

                    if (count == 2) {
                        ItemStack elytra = new ItemStack(Material.ELYTRA);
                        elytra.addEnchantment(Enchantment.BINDING_CURSE, 1);
                        ItemStack current = p.getInventory().getChestplate();
                        if (current != null && !current.getType().isAir())
                            p.setMetadata("pre-elytra", new FixedMetadataValue(BGHR.getPlugin(), current));
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

    public boolean checkForWinner() {
        if (winner != null)
            return true;

        int count = (int) participants.values().stream().filter(lives -> lives > 0).count();

        if (count <= 1 && gameTime > 0) {
            participants.forEach((key, value) -> {
                if (value > 0)
                    winner = Bukkit.getPlayer(key);
            });
            doPostGame();
            gameLog.addLogEntry(winner.getName() + " won!");
        }

        return winner != null;
    }

    private void startMainPhase() {
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
            if (options.getStartType() == GameOptions.StartType.ELYTRA && p.getInventory().getChestplate() != null &&
                    p.getInventory().getChestplate().getType().equals(Material.ELYTRA)) {
                if (p.hasMetadata("pre-elytra") && p.getMetadata("pre-elytra").get(0).value() instanceof ItemStack) {
                    p.getInventory().setChestplate((ItemStack) p.getMetadata("pre-elytra").get(0).value());
                    p.removeMetadata("pre-elytra", BGHR.getPlugin());
                } else
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
        new GameChangedPhaseEvent(this, "border").callEvent();
        world.getWorldBorder().setSize(10, options.getBorderTime());
        GameManager.getInstance().updateScoreboard();
        gameLog.addPhaseEntry(currentPhase);
    }

    private void doPostGame() {
        map.addGamePlayed((int) gameTime);
        new GameFinishedEvent(this, getWinner()).callEvent();
        gameTick.cancel();
        respawner.cancel();
        postgameTimer = startPostGameTimer();
        currentPhase = GamePhase.POSTGAME;
        new GameChangedPhaseEvent(this, "postgame").callEvent();
        GameManager.getInstance().gameIsEnding(this);
        gameLog.addPhaseEntry(currentPhase);
        world.getWorldBorder().setSize(world.getWorldBorder().getSize() + 4);
    }

    public void endGame() {
        endGame(null);
    }

    public void endGame(Consumer<Game> callback) {
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
        GameManager.getInstance().gameOver(this, callback);
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
            @Override
            public void run() {
                gameTime = (System.currentTimeMillis() - startTime) / 1000D;

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
    public static void createNewGameWithCallback(GameOptions options, BGHR plugin, Consumer<Game> callback) {
        new Game(options.getMap(), options, plugin, callback);
    }

    public static void createNewGame(GameOptions options, BGHR plugin) {
        new Game(options.getMap(), options, plugin,null);
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
                    dead.sendMessage(Tools.componentalize("&cYou died! &fYou may respawn &e" + livesLeft + " &fmore time" + ((livesLeft > 1) ? "s" : "") + ". &eYou will be &aautomatically respawned &ein &a5 &eseconds."));
                else
                    dead.sendMessage(Tools.componentalize("&cYou died! &fYou have no respawns left."));
            }
        }
    }

}
