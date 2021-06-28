package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Game implements Listener {
    private final MapData map;
    private final int livesPerPlayer;
    private final int invincibilityTime;
    private final int mainPhaseTime;
    private final int borderTime;
    private final boolean allowRegularBuilding;
    private boolean openToPlayers;
    private int gameTime;
    private BukkitTask gameTick = null;
    private Map<UUID, Integer> remainingPlayers = new HashMap<>();
    private GamePhase currentPhase;
    enum GamePhase { PREGAME, INVINCIBILITY, MAIN, BORDER, POSTGAME }

    public Game(MapData map, int livesPerPlayer, int invincibilityTime, int mainPhaseTime, int borderTime, boolean allowRegularBuilding) {
        this.map = map;
        this.livesPerPlayer = livesPerPlayer;
        this.invincibilityTime = invincibilityTime;
        this.mainPhaseTime = mainPhaseTime;
        this.borderTime = borderTime;
        this.allowRegularBuilding = allowRegularBuilding;
        openToPlayers = false;
        gameTime = -1;
        currentPhase = GamePhase.PREGAME;

        if (!map.isLoaded()) {
            MapManager.getInstance().loadMap(map);
        }

        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    public void setupGame() {
        openToPlayers = true;
        if (map.isUseBorder()) {
            WorldBorder border = map.getWorld().getWorldBorder();
            border.setCenter(map.getCenterX(), map.getCenterZ());
            border.setSize(map.getBorderRadius() * 2);
        }
    }

    public void startGame() {
        gameTick = startGameTick();
        currentPhase = GamePhase.INVINCIBILITY;
        openToPlayers = false;
        for (Player p : map.getWorld().getPlayers()) {
            p.setInvulnerable(true);
            remainingPlayers.put(p.getUniqueId(), livesPerPlayer);
        }
    }

    private void startMainPhase() {
        for (UUID uuid : remainingPlayers.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p.isOnline() && p.getGameMode() == GameMode.SURVIVAL)
                p.setInvulnerable(false);
        }
    }

    private void startBorderPhase() {
        map.getWorld().getWorldBorder().setSize(10, borderTime);
    }

    private void doPostGame() {
        map.addGamePlayed(gameTime);
        gameTick.cancel();
        currentPhase = GamePhase.POSTGAME;
    }

    private void endGame() {
        MapManager.getInstance().unloadMap(map);
        BlockPlaceEvent.getHandlerList().unregister(this);
        BlockBreakEvent.getHandlerList().unregister(this);
        BlockDamageEvent.getHandlerList().unregister(this);
    }

    public int getCurrentGameTime() {
        return gameTime;
    }

    public BukkitTask startGameTick() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                gameTime++;


                if (currentPhase == GamePhase.INVINCIBILITY && gameTime >= invincibilityTime) {
                    currentPhase = GamePhase.MAIN;
                    startMainPhase();
                }
                if (currentPhase == GamePhase.MAIN && gameTime >= invincibilityTime + mainPhaseTime) {
                    currentPhase = GamePhase.BORDER;
                    startBorderPhase();
                }
            }
        };
        return task.runTaskTimer(BGHR.getPlugin(), 20L, 20L);
    }

    public Set<Player> getActivePlayers() {
        Set<Player> players = new HashSet<>();

        for (Map.Entry<UUID, Integer> e : remainingPlayers.entrySet()) {
            if (e.getValue() > 0) {
                Player p = Bukkit.getPlayer(e.getKey());
                if (p.isOnline())
                    players.add(p);
            }
        }
        return players;
    }

    @EventHandler
    public void handlePlacing(BlockPlaceEvent event) {
        if (event.getPlayer().getWorld().equals(map.getWorld())) {
            if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.POSTGAME) {
                event.setCancelled(true);
                return;
            }

            if (!allowRegularBuilding) {
                if (Tools.getUuidFromItem(event.getItemInHand()) == null) {
                    event.setCancelled(true);
                    return;
                }
            }

        }
    }

    @EventHandler
    public void handleBreaking(BlockBreakEvent event) {
        if (event.getPlayer().getWorld().equals(map.getWorld())) {
            if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.POSTGAME) {
                event.setCancelled(true);
                return;
            }

            if (!allowRegularBuilding) {
                if (Tools.getUuidFromBlock(event.getBlock()) == null) {
                    event.setCancelled(true);
                    return;
                }
            }

        }
    }

    @EventHandler
    public void stopDamage(BlockDamageEvent event) {
        if (event.getPlayer().getWorld().equals(map.getWorld())) {
            if (currentPhase == GamePhase.PREGAME || currentPhase == GamePhase.POSTGAME) {
                event.setCancelled(true);
                return;
            }

            if (!allowRegularBuilding) {
                if (Tools.getUuidFromBlock(event.getBlock()) == null) {
                    event.setCancelled(true);
                    return;
                }
            }

        }

    }
}
