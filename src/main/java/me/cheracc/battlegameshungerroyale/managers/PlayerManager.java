package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PlayerManager {

    private static PlayerManager singletonInstance = null;
    private BGHR plugin;
    private final List<PlayerData> loadedPlayers = new CopyOnWriteArrayList<>();
    private BukkitTask databaseUpdater;
    private BukkitTask playerDataUpdater;

    private PlayerManager() {
    }

    public boolean isPlayerDataLoaded(UUID uuid) {
        for (PlayerData data : loadedPlayers) {
            if (data.getUuid().equals(uuid) && data.isLoaded())
                return true;
        }
        return false;
    }

    public PlayerData getPlayerDataCallbackIfAsync(UUID uuid, Consumer<PlayerData> callback) {
        if (isPlayerDataLoaded(uuid)) {
            PlayerData d = getPlayerData(uuid);
            callback.accept(d);
            return d;
        }
        return new PlayerData(uuid, callback);
    }

    public @NotNull PlayerData getPlayerData(UUID uuid) {
        for (PlayerData d : loadedPlayers) {
            if (d.getUuid().equals(uuid))
                return d;
        }
        return new PlayerData(uuid, null);
    }

    public @NotNull PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public void thisPlayerIsLoaded(PlayerData data) {
        if (!isPlayerDataLoaded(data.getUuid()))
            loadedPlayers.add(data);
        else
            Bukkit.getLogger().warning("data for " + data.getPlayer().getName() + " is already loaded");
    }

    public static PlayerManager getInstance() {
        if (singletonInstance == null)
            singletonInstance = new PlayerManager();
        return singletonInstance;
    }

    public void initialize(BGHR plugin) {
        this.plugin = plugin;
        databaseUpdater = databaseUpdater();
        playerDataUpdater = playerDataUpdater();
    }

    public void disable() {
        databaseUpdater.cancel();
        playerDataUpdater.cancel();
    }


    private BukkitTask databaseUpdater() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerData data : loadedPlayers) {
                    if (data.isModified()) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                data.save();
                            }
                        }.runTaskAsynchronously(plugin);
                    }
                }
            }
        };
        return task.runTaskTimer(plugin, 600L, 200L);
    }

    private BukkitTask playerDataUpdater() {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                List<PlayerData> toRemove = new ArrayList<>();

                for (PlayerData data : loadedPlayers) {
                    Player p = data.getPlayer();
                    if (!data.isModified() && data.isLoaded() && (p == null || !p.isOnline())) {
                        toRemove.add(data);
                        continue;
                    }

                    if (p != null && !MapManager.getInstance().isThisAGameWorld(p.getWorld())) {
                        data.saveInventory(false);
                        data.setLastLocation(p.getLocation());
                    }
                }

                toRemove.forEach(loadedPlayers::remove);
            }
        };
        return task.runTaskTimer(plugin, 600L, 80L);
    }

    public List<PlayerData> getLoadedPlayers() {
        return new ArrayList<>(loadedPlayers);
    }
}
