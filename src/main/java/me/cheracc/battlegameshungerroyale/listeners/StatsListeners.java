package me.cheracc.battlegameshungerroyale.listeners;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.events.*;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class StatsListeners implements Listener {
    private final BGHR plugin;
    private final PlayerManager pm;

    public StatsListeners(BGHR plugin) {
        this.plugin = plugin;
        this.pm = PlayerManager.getInstance();
    }

    @EventHandler
    public void recordEliminations(PlayerEliminatedEvent event) {
        PlayerData dead = pm.getPlayerData(event.getPlayer());
        dead.getStats().addToTimePlayed(event.getGame().getCurrentGameTime());
        if (event.getGame().getActivePlayers().size() == 2)
            dead.getStats().addSecondPlaceFinish();

    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void recordKillsAndDeaths(GameDeathEvent event) {
        if (event.getKiller() != null) {
            PlayerData killer = pm.getPlayerData(event.getKiller());
            killer.getStats().addKill();
            killer.setModified(true);
        }
        PlayerData dead = pm.getPlayerData(event.getRecentlyDeceased());
        dead.getStats().addDeath();
        dead.setModified(true);
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void recordDamage(GameDamageEvent event) {
        if (event.getAggressor() != null) {
            PlayerData aggressor = pm.getPlayerData(event.getAggressor());
            aggressor.getStats().addDamageDealt(event.getDamage());
            aggressor.setModified(true);
        }
        if (event.getVictim() != null && !event.getVictim().isDead()) {
            PlayerData victim = pm.getPlayerData(event.getVictim());
            victim.getStats().addDamageReceived(Math.min(event.getDamage(), 20));
            victim.setModified(true);
        }
    }

    @EventHandler
    public void recordGameStart(GameStartEvent event) {
        for (Player p : event.getGame().getActivePlayers()) {
            PlayerData data = pm.getPlayerData(p);
            data.getStats().addGamePlayed();
            data.setJoinTime(System.currentTimeMillis());
            data.setModified(true);
        }
    }

    @EventHandler
    public void recordGameFinish(GameFinishedEvent event) {
        if (event.getWinner() != null) {
            PlayerData winner = pm.getPlayerData(event.getWinner());
            winner.getStats().addWin();
            winner.getStats().addToTimePlayed(event.getGame().getCurrentGameTime());
            winner.setModified(true);
        }
    }

    @EventHandler
    public void recordQuits(PlayerQuitGameEvent event) {
        if (event.getLivesRemaining() > 0 && event.getGame().isActive()) {
            PlayerData data = pm.getPlayerData(event.getPlayer());
            data.getStats().addGameQuit();
            data.setModified(true);
            new PlayerEliminatedEvent(event.getPlayer(), event.getGame()).callEvent();
        }
    }

    @EventHandler
    public void countLootChests(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player) || !GameManager.getInstance().isActivelyPlayingAGame((Player) event.getPlayer()))
            return;
        PlayerData data = pm.getPlayerData((Player) event.getPlayer());
        data.getStats().addChestOpened();
        data.setModified(true);
    }

}
