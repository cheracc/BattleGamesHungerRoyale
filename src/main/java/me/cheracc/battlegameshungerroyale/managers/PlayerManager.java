package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.Metadata;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.PassiveAbility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerManager {
    private final BGHR plugin;
    private final Logr logr;
    private final KitManager kitManager;
    private final GameManager gameManager;
    private final DatabaseManager databaseManager;
    private final ConcurrentHashMap<UUID, CompletableFuture<PlayerData>> waitingForDatabase;
    private final List<PlayerData> loadedPlayers;
    private final BukkitRunnable databaseUpdater;
    private final BukkitRunnable playerDataUpdater;

    public PlayerManager(BGHR plugin, KitManager kitManager, GameManager gameManager, DatabaseManager databaseManager, Logr logr) {
        this.plugin = plugin;
        this.logr = logr;
        this.kitManager = kitManager;
        this.gameManager = gameManager;
        this.databaseManager = databaseManager;
        this.loadedPlayers = new CopyOnWriteArrayList<>();
        this.waitingForDatabase = new ConcurrentHashMap<>();
        databaseUpdater = databaseUpdater();
        databaseUpdater.runTaskTimer(plugin, 600L, 200L);
        playerDataUpdater = playerDataUpdater(gameManager);
        playerDataUpdater.runTaskTimer(plugin, 0L, 1L);
    }

    public boolean isPlayerDataLoaded(UUID uuid) {
        for (PlayerData data : loadedPlayers) {
            if (data.getUuid().equals(uuid) && data.isLoaded())
                return true;
        }
        return (waitingForDatabase.containsKey(uuid));
    }

    public CompletableFuture<PlayerData> getPlayerDataAsync(UUID uuid) {
        if (isPlayerDataLoaded(uuid)) {
            CompletableFuture<PlayerData> future = new CompletableFuture<>();
            future.complete(getPlayerData(uuid));
            return future;
        }
        return new PlayerData(uuid).loadAsynchronously(databaseManager, logr, plugin);
    }

    public void loadPlayerDataAsync(UUID uuid) {
        if (!isPlayerDataLoaded(uuid)) {
            PlayerData unloaded = new PlayerData(uuid);
            waitingForDatabase.put(uuid, unloaded.loadAsynchronously(databaseManager, logr, plugin));
            if (playerDataUpdater.isCancelled())
                playerDataUpdater.runTaskTimer(plugin, 0L, 1L);
        }
    }

    public @NotNull PlayerData getPlayerData(UUID uuid) {
        for (PlayerData d : loadedPlayers) {
            if (d.getUuid().equals(uuid))
                return d;
        }
        return new PlayerData(uuid);
    }

    public @NotNull PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public void thisPlayerIsLoaded(PlayerData data) {
        if (!loadedPlayers.contains(data)) {
            loadedPlayers.add(data);
        } else
            logr.warn("data for %s is already loaded", data.getName());
    }

    public void restorePlayerFromSavedData(Player player, PlayerData data) {
        if (player.hasMetadata(Metadata.PREGAME_SCOREBOARD.key())) {
            Scoreboard sb = (Scoreboard) player.getMetadata(Metadata.PREGAME_SCOREBOARD.key()).get(0).value();
            player.setScoreboard(sb);
        } else if (data.getSettings().isShowMainScoreboard() && plugin.getConfig().getBoolean("show main scoreboard", true))
            player.setScoreboard(plugin.getApi().getDisplayManager().getMainScoreboard());
        else
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        data.writeSavedInventoryToPlayer();

        if (data.getSettings().getDefaultKit() != null) {
            Kit kit = kitManager.getKit(data.getSettings().getDefaultKit());
            if (kit != null) {
                data.registerKit(kit, false);
                if (gameManager.isThisAGameWorld(player.getWorld()) || plugin.getConfig().getBoolean("main world.kits useable in main world", false)) {
                    outfitPlayer(player, kit);
                } else {
                    player.sendMessage(Trans.lateToComponent("Kit&e %s &fwill be equipped when you join a game.", kit.getName()));
                }
            }
        }
    }

    public boolean hasAbility(Player player, Ability ability) {
        PlayerData data = getPlayerData(player);
        return data.getKit() != null && data.getKit().equals(ability.getAssignedKit());
    }

    public void disrobePlayer(Player player, Kit kit) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getItemMeta() == null)
                continue;
            if (kitManager.isAbilityItem(item) || Tools.isPluginItem(item))
                player.getInventory().remove(item);
        }
        ItemStack item = player.getInventory().getItemInOffHand();
        if (item != null && item.getItemMeta() != null) {
            if (kitManager.isAbilityItem(item) || Tools.isPluginItem(item))
                player.getInventory().setItemInOffHand(null);
        }

        kit.getEquipment().unequip(player);
        for (Ability ability : kit.getAbilities()) {
            if (ability instanceof PassiveAbility) {
                ((PassiveAbility) ability).deactivate(player);
            }
        }
    }

    public void outfitPlayer(Player p, Kit kit) {
        if (!kit.isEnabled() && !p.hasPermission("bghr.admin.kits.disabled")) {
            p.sendMessage(Trans.lateToComponent("That kit is disabled"));
            return;
        }

        if (!gameManager.isInAGame(p) && !plugin.getConfig().getBoolean("main world.kits useable in main world", false)) {
            p.sendMessage(Trans.lateToComponent("&7You will be equipped with kit &e%s &7at the start of your next game.", kit.getName()));
            return;
        }

        if (kit != null)
            disrobePlayer(p, kit);

        for (Ability a : kit.getAbilities()) {
            if (a instanceof ActiveAbility) {
                ((ActiveAbility) a).givePlayerAbilityItem(p);
            }
            if (a instanceof PassiveAbility) {
                if (((PassiveAbility) a).hasToggleItem()) {
                    ((PassiveAbility) a).givePlayerAbilityItem(p);
                } else {
                    ((PassiveAbility) a).activate(p);
                }
            }
        }
        if (kit.getEquipment().isNotEmpty()) {
            kit.getEquipment().equip(p);
        }
        p.sendMessage(Trans.lateToComponent("You have been equipped with kit &e" + kit.getName()));
    }

    public void disable() {
        databaseUpdater.cancel();
        playerDataUpdater.cancel();
    }

    private BukkitRunnable databaseUpdater() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                for (PlayerData data : loadedPlayers) {
                    if (data.isModified()) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                data.save(databaseManager);
                            }
                        }.runTaskAsynchronously(plugin);
                    }
                }
            }
        };
    }

    private BukkitRunnable playerDataUpdater(GameManager gm) {
        final int[] tickCounter = {0};
        return new BukkitRunnable() {
            @Override
            public void run() {
                List<UUID> toRemove = new ArrayList<>();
                waitingForDatabase.values().forEach(f -> {
                    if (f.isDone()) {
                        PlayerData data = f.getNow(null);
                        if (data != null && data.getPlayer() != null) {
                            restorePlayerFromSavedData(data.getPlayer(), data);
                            thisPlayerIsLoaded(data);
                            toRemove.add(data.getUuid());
                        }
                    }
                });
                toRemove.forEach(waitingForDatabase::remove);

                // save locations and inventories if not in a game
                if (tickCounter[0] >= 20) {
                    loadedPlayers.stream().filter(data -> !gm.isInAGame(data.getPlayer()))
                                 .forEach(data -> {
                                     data.saveInventory(false);
                                     data.setLastLocation();
                                 });
                    tickCounter[0] = 0;
                }
            }
        };
    }

    public List<PlayerData> getLoadedPlayers() {
        return new ArrayList<>(loadedPlayers);
    }
}
