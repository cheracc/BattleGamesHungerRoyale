package me.cheracc.battlegameshungerroyale.datatypes;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.events.CustomEventsListener;
import me.cheracc.battlegameshungerroyale.events.GameDamageEvent;
import me.cheracc.battlegameshungerroyale.events.GameDeathEvent;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class Game implements Listener {
    private final MapData map;
    private final GameLog gameLog;
    private final GameOptions options;
    private final BossBar bar;
    private final World world;

    private boolean openToPlayers;
    private double postgameTime;
    private double pregameTime;
    private double gameTime;
    private Player winner = null;
    private BukkitTask gameTick = null;
    private final Map<UUID, Integer> participants = new HashMap<>();
    private final List<Location> hardSpawnPoints = new ArrayList<>();
    private GamePhase currentPhase;
    enum GamePhase { PREGAME, INVINCIBILITY, MAIN, BORDER, POSTGAME }

    public Game(MapData map, GameOptions options) {
        this.map = map;
        this.options = options;
        this.world = MapManager.getInstance().createNewWorld(map);
        bar = Bukkit.createBossBar("Pregame", BarColor.WHITE, BarStyle.SOLID);
        bar.setVisible(true);
        gameLog = new GameLog(this);
        openToPlayers = false;
        pregameTime = -1;
        gameTime = -1;
        postgameTime = -1;
        currentPhase = GamePhase.PREGAME;

        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    public void setupGame() {
        openToPlayers = true;
        hardSpawnPoints.addAll(getSpawnPoints());
        if (map.isUseBorder() && map.getBorderRadius() > 0) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(map.getBorderCenter(world));
            border.setSize(map.getBorderRadius() * 2);
        }
        // always
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);

        // for pregame only
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

        gameLog.addPhaseEntry(currentPhase);

        startPregameTimer();
    }

    public void startGame() {
        CustomEventsListener.getInstance();
        openToPlayers = false;
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, true);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);


        if (options.getStartType() == GameOptions.StartType.ELYTRA)
            doElytraSpawn(() -> {
                gameTick = startGameTick();
                gameLog.addPhaseEntry(currentPhase);
                currentPhase = GamePhase.INVINCIBILITY;
            });
        else if (options.getStartType() == GameOptions.StartType.HUNGERGAMES) {
            doHungergamesSpawn(() -> {
                gameTick = startGameTick();
                gameLog.addPhaseEntry(currentPhase);
                currentPhase = GamePhase.INVINCIBILITY;
            });
        }
        for (Player p : getActivePlayers()) {
            p.setInvulnerable(true);
            p.setBedSpawnLocation(p.getLocation(), true);
        }

    }

    private interface ImFinished {
        void finished();
    }

    private void startMainPhase() {
        currentPhase = GamePhase.MAIN;
        for (UUID uuid : participants.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (isPlaying(p)) {
                p.setInvulnerable(false);
                p.setWalkSpeed(0.2F);
            }
            if (options.getStartType() == GameOptions.StartType.ELYTRA && p.getInventory().getChestplate() != null &&
                    p.getInventory().getChestplate().getType().equals(Material.ELYTRA)) {
                p.getInventory().setChestplate(null);
                p.setGliding(false);
                p.setAllowFlight(false);
            }
        }
        gameLog.addPhaseEntry(currentPhase);
    }

    private void startBorderPhase() {
        currentPhase = GamePhase.BORDER;
        world.getWorldBorder().setSize(10, options.getBorderTime());
        gameLog.addPhaseEntry(currentPhase);
    }

    private void doPostGame() {
        map.addGamePlayed((int) gameTime);
        gameTick.cancel();
        startPostGameTimer();
        currentPhase = GamePhase.POSTGAME;
        gameLog.addPhaseEntry(currentPhase);
    }

    public void endGame() {
        gameTick.cancel();
        gameLog.finalizeLog();
        for (Player p : getCurrentPlayersAndSpectators())
            quit(p);

        world.getWorldBorder().setSize(world.getWorldBorder().getSize() + 4);
        bar.setVisible(false);
        bar.removeAll();

        MapManager.getInstance().unloadWorld(world);
        GameManager.getInstance().gameOver(this);
        HandlerList.unregisterAll(this);
    }

    public int getCurrentGameTime() {
        return (int) gameTime;
    }

    public String getPhase() {
        return currentPhase.name().toLowerCase();
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
        player.teleport(map.getSpawnCenter(world));
        bar.addPlayer(player);
    }

    public void join(Player player) {
        participants.put(player.getUniqueId(), options.getLivesPerPlayer());
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(map.getSpawnCenter(world));
        bar.addPlayer(player);
        if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.INVINCIBILITY)
            player.setInvulnerable(true);
        gameLog.addLogEntry(String.format("%s joined (%s/%s)", player.getName(), getActivePlayers().size(), getStartingPlayersSize()));
    }

    public void quit(Player player) {
        if (currentPhase == GamePhase.PREGAME) {
            participants.remove(player.getUniqueId());
        }
        if (participants.containsKey(player.getUniqueId()) && participants.get(player.getUniqueId()) > 0) {
            participants.replace(player.getUniqueId(), 0);
            gameLog.addLogEntry(String.format("%s left (%s/%s)", player.getName(), getActivePlayers().size(), getStartingPlayersSize()));
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.setWalkSpeed(0.2F);
        player.setAllowFlight(false);
        player.teleport(MapManager.getInstance().getLobbyWorld().getSpawnLocation());
        bar.removePlayer(player);
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
        doPostGame();
        gameLog.addLogEntry(winner.getName() + " won!");
        return true;
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

    private void startPostGameTimer() {
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

        postGameTimer.runTaskTimer(BGHR.getPlugin(), 20L, 4L);
    }

    private void startPregameTimer() {
        BukkitRunnable pregameTimer = new BukkitRunnable() {
            double last = System.currentTimeMillis();
            @Override
            public void run() {
                if (currentPhase != GamePhase.PREGAME)
                    cancel();

                if (pregameTime <= 0) {
                    // check if there's enough players to start
                    if (getActivePlayers().size() >= options.getPlayersNeededToStart()) {
                        startGame();
                        cancel();
                    }
                    else {
                        pregameTime = options.getPregameTime();
                    }

                } else
                    pregameTime -= (System.currentTimeMillis() - last) / 1000D;

                updateBossBar();
                last = System.currentTimeMillis();
            }
        };
        pregameTimer.runTaskTimer(BGHR.getPlugin(), 20L, 4L);
    }

    private BukkitTask startGameTick() {
        BukkitRunnable task = new BukkitRunnable() {
            long last = System.currentTimeMillis();
            @Override
            public void run() {
                gameTime += (System.currentTimeMillis() - last) / 1000D;

                if (checkForWinner())
                    cancel();

                if (currentPhase == GamePhase.INVINCIBILITY && gameTime >= options.getInvincibilityTime()) {
                    startMainPhase();
                }
                if (currentPhase == GamePhase.MAIN && gameTime >= options.getInvincibilityTime() + options.getMainPhaseTime()) {
                    startBorderPhase();
                }

                updateBossBar();
                last = System.currentTimeMillis();
            }
        };
        return task.runTaskTimer(BGHR.getPlugin(), 20L, 4L);
    }

    public int getStartingPlayersSize() {
        return participants.size();
    }

    public List<String> getFullPlayerList() {
        List<String> list = new ArrayList<>();
        for (UUID u : participants.keySet()) {
            list.add(Bukkit.getPlayer(u).getName());
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
                if (p.isOnline() && isPlaying(p))
                    players.add(p);
                else { // where'd they go?
                    Bukkit.getLogger().info(String.format("%s has %s lives but is not here", p.getName(), participants.get(p.getUniqueId())));
                    quit(p);
                }
            }
        }
        return players;
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

    private void doHungergamesSpawn(ImFinished callback) {
        List<Location> spawns = getSpawnPoints();

        if (spawns.size() < getActivePlayers().size()) {
            Bukkit.getLogger().warning("not enough spawns");
            return;
        }

        Collections.shuffle(spawns);
        int count = 0;
        for (Player p : getActivePlayers()) {
            p.teleport(spawns.get(count));
            p.setWalkSpeed(0F);
            count++;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : getActivePlayers())
                    p.setWalkSpeed(0.2F);
                callback.finished();
            }
        }.runTaskLater(BGHR.getPlugin(), 40L);
    }

    // gets the spawn points using map configuration - intensive so only done once
    private List<Location> getHardSpawnPoints() {
        return new ArrayList<>(hardSpawnPoints);
    }

    // for starting game - adds some if needed
    private List<Location> getSpawnPoints() {
        Location center = map.getSpawnCenter(world);
        List<Location> spawnPoints = new ArrayList<>(hardSpawnPoints);
        int scanRadius = map.getSpawnRadius() + 1;

        if (spawnPoints.isEmpty() && map.getSpawnBlockType() != null) {
            for (int x = -scanRadius; x <= scanRadius; x++) {
                for (int y = -scanRadius; y <= scanRadius; y++)
                    for (int z = -scanRadius; z <= scanRadius; z++) {
                        Location l = center.clone().add(x, y, z);
                        Block b = l.getBlock();

                        if (b != null && b.getType() == map.getSpawnBlockType()) {
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

        if (spawnPoints.size() < getActivePlayers().size()) {
            Bukkit.getLogger().info("no spawn points found - adding our own");
            int spawnPointsNeeded = getActivePlayers().size() - spawnPoints.size();

            for (int i = 0; i < spawnPointsNeeded; i++) {
                double angle = (Math.PI * 2 * i) / spawnPointsNeeded;
                int radius = map.getSpawnRadius();
                Location spawnPoint = center.clone().add(radius * Math.cos(angle), 2, radius * Math.sin(angle)).toHighestLocation().add(0,2,0);
                spawnPoint.setDirection(spawnPoint.clone().subtract(center).toVector().multiply(-1));
                spawnPoints.add(spawnPoint);
            }
        }
        return spawnPoints;
    }

    private void doElytraSpawn(ImFinished callback) {
        Vector boost = new Vector(0, 1, 0);
        List<Location> spawns = getSpawnPoints();

        if (spawns.size() < getActivePlayers().size()) {
            Bukkit.getLogger().warning("not enough spawns");
            return;
        }

        Collections.shuffle(spawns);
        int count = 0;
        for (Player p : getActivePlayers()) {
            p.teleport(spawns.get(count));
            p.setWalkSpeed(0F);
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
                        p.setWalkSpeed(0.2F);
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
                        p.setAllowFlight(true);
                    }
                    if (count >= 4) {
                        cancel();
                        callback.finished();
                    }
                }
                count++;
            }
        }.runTaskTimer(BGHR.getPlugin(), 40L, 4L);
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

        if (GameManager.getInstance().getPlayersCurrentGame(dead).equals(this)) {
            gameLog.addDeathEntry(dead);

            if (participants.containsKey(dead.getUniqueId())) {
                int livesLeft = Math.max(0, participants.get(dead.getUniqueId()) - 1);

                participants.replace(dead.getUniqueId(), livesLeft);
                if (livesLeft > 0)
                    dead.sendMessage(Tools.componentalize("&cYou died! &fYou may respawn &e" + livesLeft + " &fmore time" + ((livesLeft > 1) ? "s" : "")));
                else
                    dead.sendMessage(Tools.componentalize("&cYou died! &fYou have no respawns left."));
            }
        }
    }

    @EventHandler
    public void handleQuits(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        p.setWalkSpeed(0.2F);
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

}
