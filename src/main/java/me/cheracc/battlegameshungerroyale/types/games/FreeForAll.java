package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.events.GameDamageEvent;
import me.cheracc.battlegameshungerroyale.events.GameDeathEvent;
import me.cheracc.battlegameshungerroyale.events.GameStartEvent;
import me.cheracc.battlegameshungerroyale.events.PlayerLootedChestEvent;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
    }

    public FreeForAll() {
        super();
        ffaStats = new HashMap<>();
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
        Entity a = event.getAggressor();
        Entity v = event.getVictim();

        if (a instanceof Player) {
            getStats(a.getUniqueId()).damage += event.getDamage();
        }
        if (v instanceof Player) {
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
    public void join(Player player) {
        super.join(player);
        ffaStats.put(player.getUniqueId(), new FfaStats());
        if (api.getPlayerManager().getPlayerData(player).getSettings().isShowGameScoreboard()) {
            setupScoreboard(player);
        }
        api.getPlayerManager().clearInventoryAndRestoreKit(player);
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
    protected void onTick() {
        setGameTime((System.currentTimeMillis() - getStartTime()) / 1000D);
    }

    @Override
    public void handleDeaths(GameDeathEvent event) {
        if (event.getGame().equals(this)) {
            Player p = event.getRecentlyDeceased();
            if (event.getKiller() instanceof Player) {
                getStats((Player) event.getKiller()).kills++;
                getStats((Player) event.getKiller()).streak++;
            }
            FfaStats stats = getStats(p);
            stats.deaths++;
            stats.streak = 0;
        }
    }

    @EventHandler
    public void onStart(GameStartEvent event) {
        startMainPhase();
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
