package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.events.CustomEventsListener;
import me.stipe.battlegameshungerroyale.events.GameDamageEvent;
import me.stipe.battlegameshungerroyale.events.GameDeathEvent;
import me.stipe.battlegameshungerroyale.managers.GameManager;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.WorldBorder;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Game implements Listener {
    private final MapData map;
    private final GameLog gameLog;
    private final GameOptions options;
    private final BossBar bar;

    private boolean openToPlayers;
    private double postgameTime;
    private double pregameTime;
    private double gameTime;
    private BukkitTask gameTick = null;
    private final Map<UUID, Integer> participants = new HashMap<>();
    private GamePhase currentPhase;
    enum GamePhase { PREGAME, INVINCIBILITY, MAIN, BORDER, POSTGAME }

    public Game(MapData map, GameOptions options) {
        this.map = map;
        this.options = options;
        bar = Bukkit.createBossBar("Pregame", BarColor.WHITE, BarStyle.SOLID);
        gameLog = new GameLog(this);
        openToPlayers = false;
        pregameTime = -1;
        gameTime = -1;
        postgameTime = -1;
        currentPhase = GamePhase.PREGAME;

        if (!map.isLoaded()) {
            MapManager.getInstance().loadMap(map);
        }
        bar.setVisible(true);

        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    public void setupGame() {
        openToPlayers = true;
        if (map.isUseBorder()) {
            WorldBorder border = map.getWorld().getWorldBorder();
            border.setCenter(map.getCenterX(), map.getCenterZ());
            border.setSize(map.getBorderRadius() * 2);
        }
        // always
        map.getWorld().setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        map.getWorld().setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        map.getWorld().setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);

        // for pregame only
        map.getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        map.getWorld().setGameRule(GameRule.DO_FIRE_TICK, false);
        map.getWorld().setGameRule(GameRule.DO_MOB_SPAWNING, false);
        map.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, false);

        gameLog.addPhaseEntry(currentPhase);

        startPregameTimer();
    }

    public void startGame() {
        CustomEventsListener.getInstance();
        gameTick = startGameTick();
        currentPhase = GamePhase.INVINCIBILITY;
        openToPlayers = false;
        map.getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        map.getWorld().setGameRule(GameRule.DO_FIRE_TICK, true);
        map.getWorld().setGameRule(GameRule.DO_MOB_SPAWNING, true);
        map.getWorld().setGameRule(GameRule.DO_WEATHER_CYCLE, true);

        for (Player p : map.getWorld().getPlayers()) {
            p.setInvulnerable(true);
        }
        gameLog.addPhaseEntry(currentPhase);
    }

    private void startMainPhase() {
        currentPhase = GamePhase.MAIN;
        for (UUID uuid : participants.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (isPlaying(p))
                p.setInvulnerable(false);
        }
        gameLog.addPhaseEntry(currentPhase);
    }

    private void startBorderPhase() {
        currentPhase = GamePhase.BORDER;
        map.getWorld().getWorldBorder().setSize(10, options.getBorderTime());
        gameLog.addPhaseEntry(currentPhase);
    }

    private void doPostGame() {
        map.addGamePlayed((int) gameTime);
        gameTick.cancel();
        startPostGameTimer();
        currentPhase = GamePhase.POSTGAME;
        gameLog.addPhaseEntry(currentPhase);
    }

    private void endGame() {
        gameLog.finalizeLog();
        for (Player p : getCurrentPlayersAndSpectators())
            quit(p);

        bar.setVisible(false);
        bar.removeAll();

        MapManager.getInstance().unloadMap(map);
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

    public boolean isOpenToPlayers() {
        return openToPlayers;
    }

    public boolean isPlaying(Player player) {
        if (!player.isOnline())
            return false;
        if (!participants.containsKey(player.getUniqueId()))
            return false;
        return participants.get(player.getUniqueId()) > 0 && map.getWorld().equals(player.getWorld());
    }

    public boolean isSpectating(Player player) {
        if (player.getWorld().equals(map.getWorld())) {
            if (participants.get(player.getUniqueId()) == null)
                return true;
            return participants.get(player.getUniqueId()) == 0;
        }
        return false;
    }

    public void join(Player player) {
        participants.put(player.getUniqueId(), options.getLivesPerPlayer());
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(map.getWorld().getSpawnLocation());
        bar.addPlayer(player);
        player.setBedSpawnLocation(map.getWorld().getSpawnLocation(), true);
        gameLog.addLogEntry(String.format("%s joined (%s/%s)", player.getName(), getActivePlayers().size(), getStartingPlayersSize()));
    }

    public void quit(Player player) {
        if (!getWinner().equals(player) && participants.containsKey(player.getUniqueId())) {
            participants.replace(player.getUniqueId(), 0);
            gameLog.addLogEntry(String.format("%s left (%s/%s)", player.getName(), getActivePlayers().size(), getStartingPlayersSize()));
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(MapManager.getInstance().getLobbyWorld().getSpawnLocation());
        bar.removePlayer(player);
    }

    private void checkForWinner() {
        Player winner = null;
        for (Map.Entry<UUID, Integer> e : participants.entrySet()) {
            if (e.getValue() > 0) {
                if (winner != null) {
                    return;
                }
                winner = Bukkit.getPlayer(e.getKey());
            }
        }
        doPostGame();
        gameLog.addLogEntry(winner.getName() + " won!");
    }

    public Player getWinner() {
        if (currentPhase != GamePhase.POSTGAME)
            return null;

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
                    endGame();
                    cancel();
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
                    else
                        pregameTime = options.getPregameTime();

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

                checkForWinner();

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

        players.addAll(map.getWorld().getPlayers());
        return players;
    }

    public Set<Player> getActivePlayers() {
        Set<Player> players = new HashSet<>();

        for (Map.Entry<UUID, Integer> e : participants.entrySet()) {
            if (e.getValue() > 0) {
                Player p = Bukkit.getPlayer(e.getKey());
                if (p.isOnline())
                    players.add(p);
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
            phaseProgress = 1 - postgameTime;
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

//    @EventHandler
//    public void handlePlacing(BlockPlaceEvent event) {
//        if (event.getPlayer().getWorld().equals(map.getWorld())) {
//            if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.POSTGAME) {
//                event.setCancelled(true);
//                return;
//            }
//
//            if (!options.isAllowRegularBuilding()) {
//                if (Tools.getUuidFromItem(event.getItemInHand()) == null) {
//                    event.setCancelled(true);
//                }
//            }
//
//        }
//    }
//
//    @EventHandler
//    public void handleBreaking(BlockBreakEvent event) {
//        if (event.getPlayer().getWorld().equals(map.getWorld())) {
//            if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.POSTGAME) {
//                event.setCancelled(true);
//                return;
//            }
//
//            if (!options.isAllowRegularBuilding()) {
//                if (Tools.getUuidFromBlock(event.getBlock()) == null) {
//                    event.setCancelled(true);
//                }
//            }
//
//        }
//    }
//
//    @EventHandler
//    public void stopDamage(BlockDamageEvent event) {
//        if (event.getPlayer().getWorld().equals(map.getWorld())) {
//            if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.POSTGAME) {
//                event.setCancelled(true);
//                return;
//            }
//
//            if (!options.isAllowRegularBuilding()) {
//                if (Tools.getUuidFromBlock(event.getBlock()) == null) {
//                    event.setCancelled(true);
//                }
//            }
//
//        }
//    }

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
        if (isSpectating(p) || isPlaying(p))
            quit(p);
    }

    @EventHandler
    public void onJoinWorld(PlayerChangedWorldEvent event) {
        // coming to this world!
        if (event.getPlayer().getWorld().equals(map.getWorld())) {
            Player p = event.getPlayer();
            if (participants.containsKey(p.getUniqueId()) && participants.get(p.getUniqueId()) > 0)
                return;
            p.setGameMode(GameMode.SPECTATOR);
            bar.addPlayer(event.getPlayer());
        }
        // leaving this world!
        if (event.getFrom().equals(map.getWorld())) {
            UUID uuid = event.getPlayer().getUniqueId();
            if (participants.get(uuid) != null)
                if (participants.get(uuid) != 0)
                    quit(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (isPlaying(event.getPlayer())) {
            event.setRespawnLocation(map.getWorld().getSpawnLocation());
            event.getPlayer().setGameMode(GameMode.ADVENTURE);
        }
        if (isSpectating(event.getPlayer())) {
            event.setRespawnLocation(map.getWorld().getSpawnLocation());
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }
}
