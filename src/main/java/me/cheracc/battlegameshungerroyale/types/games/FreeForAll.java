package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.events.GameDamageEvent;
import me.cheracc.battlegameshungerroyale.events.GameDeathEvent;
import me.cheracc.battlegameshungerroyale.events.PlayerLootedChestEvent;
import me.cheracc.battlegameshungerroyale.events.PlayerQuitGameEvent;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Metadata;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FreeForAll extends Game {
    private final Map<UUID, FfaStats> ffaStats;

    public FreeForAll(GameOptions options) {
        super(options);
        ffaStats = new HashMap<>();
        options.setPlayersNeededToStart(0);
        doGameTick().runTaskTimer(JavaPlugin.getPlugin(BGHR.class), 200L, 10L);
    }

    public FreeForAll() {
        super();
        ffaStats = new HashMap<>();
    }

    private BukkitRunnable doGameTick() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (lootManager != null) {
                    if ((System.currentTimeMillis() - getLastChestRespawn()) / 1000 / 60 >= getOptions().getChestRespawnTime()) {
                        // TODO add the ability to modify this 'density' number for loot chests
                        lootManager.placeLootChests((int) (getActivePlayers().size() * 5 * Math.sqrt(getMap().getBorderRadius())));
                        setLastChestRespawn(System.currentTimeMillis());
                    }
                }
                updateScoreboard();
            }
        };
    }

    private FfaStats getStats(UUID id) {
        if (!ffaStats.containsKey(id) || ffaStats.get(id) == null)
            ffaStats.put(id, new FfaStats());
        return ffaStats.get(id);
    }

    private FfaStats getStats(Player p) {
        return getStats(p.getUniqueId());
    }

    @EventHandler
    public void updateStats(GameDamageEvent event) {
        Player a = event.getAggressor();
        Player v = event.getVictim();

        if (a != null) {
            getStats(a.getUniqueId()).damage += event.getDamage();
        }
        if (v != null) {
            getStats(v.getUniqueId()).taken += event.getDamage();
        }
    }

    @EventHandler
    public void countLootChests(PlayerLootedChestEvent event) {
        if (!event.getPlayer().getWorld().equals(getWorld()))
            return;
        getStats(event.getPlayer().getUniqueId()).chests++;
    }

    @Override
    public String getGameTypeName() {
        return "Free-For-All";
    }

    @Override
    public String getGameDescription() {
        return "Free-For-All games are simply open worlds with kits and unlimited lives. These games do not end unless ended by an administrator or when the server restarts.";
    }

    @Override
    public Material getGameIcon() {
        return Material.DIAMOND_SWORD;
    }

    @Override
    protected void runExtraMainPhaseProcedures() {
        setOpenToPlayer(true);
    }

    @Override
    public void join(Player player) {
        super.join(player);
        ffaStats.put(player.getUniqueId(), new FfaStats());
        if (api.getPlayerManager().getPlayerData(player).getSettings().isShowGameScoreboard()) {
            setupScoreboard(player);
        }
    }

    @Override
    public void quit(Player player) {
        new PlayerQuitGameEvent(player, this, 0).callEvent();
        PlayerData data = api.getPlayerManager().getPlayerData(player);
        player.setAllowFlight(false);
        player.setInvulnerable(false);
        if (api.getPlugin().getConfig().getBoolean("main world.place players at spawn on join", false))
            player.teleport(api.getMapManager().getLobbyWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        else
            player.teleport(data.getLastLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        getBossBar().removePlayer(player);
        if (player.hasMetadata(Metadata.PREGAME_SCOREBOARD.key())) {
            Scoreboard sb = (Scoreboard) player.getMetadata(Metadata.PREGAME_SCOREBOARD.key()).get(0).value();
            player.setScoreboard(sb);
        } else if (api.getPlugin().getConfig().getBoolean("show main scoreboard", false) && data.getSettings().isShowMainScoreboard()) {
            player.setScoreboard(api.getDisplayManager().getMainScoreboard());
        } else
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        if (player.hasMetadata(Metadata.PREGAME_HEALTH.key())) {
            player.setHealth((double) player.getMetadata(Metadata.PREGAME_HEALTH.key()).get(0).value());
            player.removeMetadata(Metadata.PREGAME_HEALTH.key(), api.getPlugin());
        }
        if (player.hasMetadata(Metadata.PREGAME_FOOD_LEVEL.key())) {
            player.setFoodLevel((int) player.getMetadata(Metadata.PREGAME_FOOD_LEVEL.key()).get(0).value());
        }
    }

    @Override
    public boolean checkForWinner() {
        return false;
    }

    @Override
    protected void updateScoreboard() {
        if (getWorld() == null)
            return;
        for (Player player : getWorld().getPlayers()) {
            if (api.getPlayerManager().getPlayerData(player).getSettings().isShowGameScoreboard()) {
                Scoreboard scoreboard = player.getScoreboard();
                Objective obj = scoreboard.getObjective("main");
                FfaStats stats = getStats(player.getUniqueId());
                obj.displayName(Tools.componentalize(String.format("&f&l%s", getGameTypeName())));
                setScoreboardLine(scoreboard, 15, "&6Map: &e" + getMap().getMapName());
                setScoreboardLine(scoreboard, 14, "&b==================");
                setScoreboardLine(scoreboard, 13, "  &dKills: &e" + stats.kills);
                setScoreboardLine(scoreboard, 12, "  &dStreak: &e" + stats.streak);
                setScoreboardLine(scoreboard, 11, "  &dDeaths: &e" + stats.deaths);
                setScoreboardLine(scoreboard, 10, "  &dDamage: &e" + stats.damage);
                setScoreboardLine(scoreboard, 9, "  &dTaken: &e" + stats.taken);
                setScoreboardLine(scoreboard, 8, "  &dChests: &e" + stats.chests);
            }
        }
    }

    @Override
    public void handleDeaths(GameDeathEvent event) {
        if (event.getGame().equals(this)) {
            Player p = event.getRecentlyDeceased();
            if (event.getKiller() != null) {
                getStats(event.getKiller()).kills++;
                getStats(event.getKiller()).streak++;
            }
            FfaStats stats = getStats(p);
            stats.deaths++;
            stats.streak = 0;
            p.sendMessage(Trans.lateToComponent("&cYou died! &fYou will be automatically respawned in 5 seconds."));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p != null && p.isOnline() && p.isDead())
                        p.spigot().respawn();
                }
            }.runTaskLater(api.getPlugin(), 20 * 5L);
        }
    }

    protected void setupScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("main", "dummy", Tools.componentalize(""));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (int i = 15; i >= 0; i--) {
            String entry = ChatColor.values()[i] + "" + ChatColor.values()[i + 1];
            Team lineText = scoreboard.registerNewTeam(String.format("line%s", i));
            lineText.addEntry(entry);
            obj.getScore(entry).setScore(i);
        }
        player.setScoreboard(scoreboard);
        updateScoreboard();
    }

    protected void setScoreboardLine(Scoreboard scoreboard, int line, String text) {
        scoreboard.getTeam("line" + line).prefix(Tools.componentalize(text));
    }

    private static class FfaStats {
        int kills;
        int streak;
        int deaths;
        int damage;
        int taken;
        int chests;
    }
}
