package me.cheracc.battlegameshungerroyale.listeners;

import me.cheracc.battlegameshungerroyale.events.*;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.entity.Animals;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.PlayerInventory;

public class StatsListeners implements Listener {
    private final PlayerManager pm;
    private final GameManager gm;

    public StatsListeners(GameManager gameManager, PlayerManager playerManager) {
        this.pm = playerManager;
        this.gm = gameManager;
    }

    @EventHandler
    public void recordEliminations(PlayerEliminatedEvent event) {
        PlayerData dead = pm.getPlayerData(event.getPlayer());
        dead.getStats().addToTimePlayed(event.getGame().getCurrentGameTime());
        if (event.getGame().getActivePlayers().size() == 2)
            dead.getStats().addSecondPlaceFinish();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void recordKillsAndDeaths(GameDeathEvent event) {
        if (event.getKiller() instanceof Player) {
            PlayerData killer = pm.getPlayerData((Player) event.getKiller());
            killer.getStats().addKill();
            killer.setModified(true);
        }
        PlayerData dead = pm.getPlayerData(event.getRecentlyDeceased());
        dead.getStats().addDeath();
        dead.setModified(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void recordDamage(GameDamageEvent event) {
        if (event.getAggressor() instanceof Player && event.getVictim() instanceof Player) {
            PlayerData aggressor = pm.getPlayerData((Player) event.getAggressor());
            aggressor.getStats().addDamageDealt(event.getDamage());
            aggressor.setModified(true);
            if (!event.getVictim().isDead()) {
                PlayerData victim = pm.getPlayerData((Player) event.getVictim());
                victim.getStats().addDamageReceived(Math.min(event.getDamage(), 20));
                victim.setModified(true);
            }
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
    public void countLootChests(PlayerLootedChestEvent event) {
        PlayerData data = pm.getPlayerData(event.getPlayer());
        data.getStats().addChestOpened();
        data.setModified(true);
    }

    @EventHandler
    public void monsterAndAnimalKills(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && (event.getEntity() instanceof Monster || event.getEntity() instanceof Animals)) {
            Player p = (Player) event.getDamager();
            LivingEntity e = (LivingEntity) event.getEntity();

            if (event.getFinalDamage() < e.getHealth())
                return;

            if (gm.isActivelyPlayingAGame(p)) {
                if (event.getEntity() instanceof Monster) {
                    PlayerData data = pm.getPlayerData(p);
                    data.getStats().addMonstersKilled();
                    data.setModified(true);
                } else if (event.getEntity() instanceof Animals) {
                    PlayerData data = pm.getPlayerData(p);
                    data.getStats().addAnimalsKilled();
                    data.setModified(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void countFood(PlayerItemConsumeEvent event) {
        if (gm.isActivelyPlayingAGame(event.getPlayer())) {
            PlayerData data = pm.getPlayerData(event.getPlayer());
            data.getStats().addFoodEaten();
            data.setModified(true);
        }
    }

    @EventHandler
    public void countArrowsShot(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player p = (Player) event.getEntity().getShooter();
            if (gm.isActivelyPlayingAGame(p)) {
                PlayerData data = pm.getPlayerData(p);
                data.getStats().addArrowsShot();
                data.setModified(true);
            }
        }
    }

    @EventHandler
    public void countLootedItems(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            if (gm.isActivelyPlayingAGame(p)) {
                if (!(event.getClickedInventory() instanceof PlayerInventory) &&
                        (event.getAction().name().contains("PICKUP") || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)) {
                    PlayerData data = pm.getPlayerData(p);
                    data.getStats().addItemsLooted();
                    data.setModified(true);
                }
            }
        }
    }
}
