package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.events.*;
import me.cheracc.battlegameshungerroyale.tools.Logr;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.*;

public class GameLog implements Listener {
    private final long startTime;
    private final String mapName;
    private final Map<Long, Object> entries = new HashMap<>();
    private final Game game;
    private final File logFile;

    public GameLog(Game game, BGHR plugin) {
        this.startTime = System.currentTimeMillis();
        this.game = game;
        mapName = game.getMap().getMapName();

        String timestamp = Instant.now().toString().split("\\.")[0].replace('T', '_').replace(':', '-');
        logFile = new File(plugin.getDataFolder().getAbsolutePath() + "/gamelogs",  timestamp + ".log");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void addJoinEntry(PlayerJoinedGameEvent event) {
        if (!isMyGame(event.getGame()))
            return;

        insertEntry(event.getPlayer() + " joined the game" + (event.isJoiningAsSpectator() ? " as a spectator" : ""));
    }

    @EventHandler
    public void addQuitEntry(PlayerQuitGameEvent event) {
        if (!isMyGame(event.getGame()))
            return;

        insertEntry(String.format("%s quit (had %s lives remaining). %s player%s left", event.getPlayer().getName(), event.getLivesRemaining(),
                event.getGame().getActivePlayers().size(), event.getGame().getActivePlayers().size() > 1 ? "s" : ""));
    }

    @EventHandler
    public void addEliminatedEntry(PlayerEliminatedEvent event) {
        if (!isMyGame(event.getGame()))
            return;

        StringBuilder sb = new StringBuilder();
        for (Player p : event.getGame().getActivePlayers()) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(p.getName()).append("(").append(p.getHealth()).append(")");
        }
        insertEntry(String.format("%s eliminated. Remaining: [ %s ]", event.getPlayer().getName(), sb));
    }

    @EventHandler
    public void addDamageEntry(GameDamageEvent event) {
        if (!isMyGame(event.getGame()))
            return;

        GameLogDamageEntry entry = new GameLogDamageEntry(event.getAggressor(), event.getVictim(), event.getType(), event.getDamage(), event.getBestGuess());
        entries.put(System.currentTimeMillis(), entry);
    }

    @EventHandler
    public void addDeathEntry(GameDeathEvent event) {
        if (!isMyGame(event.getGame()))
            return;

        entries.put(System.currentTimeMillis(), String.format("%s died. KB:%s [%s(%s)]",
                event.getRecentlyDeceased().getName(), event.getKiller() == null ? "?" : event.getKiller().getName(),
                ((int)(event.getKillingBlowDamage() * 10))/10D, event.getKillingBlowCause()));
        try {
            saveLogFile();
        } catch (IOException e) {
            Bukkit.getLogger().warning("couldn't save gamelog file");
        }
    }

    @EventHandler
    public void addPhaseEntry(GameChangedPhaseEvent event) {
        if (!isMyGame(event.getGame()))
            return;
        entries.put(System.currentTimeMillis(), event.getPhase());
        try {
            saveLogFile();
        } catch (IOException e) {
            Bukkit.getLogger().warning("couldn't save gamelog file");
        }
    }

    private void insertEntry(String string) {
        entries.put(System.currentTimeMillis(), string);
    }

    public void addLogEntry(String string) {
        entries.put(System.currentTimeMillis(), string);
        try {
            saveLogFile();
        } catch (IOException e) {
            Bukkit.getLogger().warning("couldn't save gamelog file");
        }
    }

    public void finalizeLog() {
        try {
            saveLogFile();
        } catch (IOException e) {
            Bukkit.getLogger().warning("couldn't save gamelog file");
        }
        HandlerList.unregisterAll(this);
    }

    private void saveLogFile() throws IOException {
        if (!logFile.exists())
            if (logFile.getParentFile().mkdirs())
                Logr.info("Creating directory for game log files: " + logFile.getParentFile().getAbsolutePath());

        FileWriter writer = new FileWriter(logFile);
        PrintWriter printer = new PrintWriter(writer);

        List<Long> entryTimes = new ArrayList<>(entries.keySet());
        List<String> players = new ArrayList<>(game.getFullPlayerList());
        Collections.sort(entryTimes);
        Collections.sort(players);

        StringBuilder playerString = new StringBuilder();
        for (String s : players) {
            if (playerString.length() > 0)
                playerString.append(", ");
            playerString.append(s);
        }

        printer.printf("Game Started: %s", Instant.ofEpochMilli(startTime));
        printer.println();
        printer.printf("Game Ended: %s", Instant.now());
        printer.println();
        printer.printf("Time elapsed: %s", Tools.secondsToMinutesAndSeconds(game.getCurrentGameTime()));
        printer.println();
        printer.printf("Map: %s", mapName);
        printer.println();
        printer.printf("Participants(%s): %s", players.size(), playerString);
        printer.println();
        printer.printf("Winner: %s", game.checkForWinner() ? game.getWinner().getName() : "");
        printer.println();

        for (Long l : entryTimes) {
            Object o = entries.get(l);
            Instant time = Instant.ofEpochMilli(l);

            if (o instanceof GameLogDamageEntry) {
                GameLogDamageEntry entry = (GameLogDamageEntry) o;
                if (entry.getDamager() != null && entry.getDamager().length() > 1) {
                    printer.printf("[%s]: %s(%.1f) [%s(%.1f)-> %s(%.1f)", time.toString(), entry.getDamager(), entry.getDamagerHealth(),
                            entry.getType().name().toLowerCase(), entry.getDamage(), entry.getVictim(), entry.getVictimHealth());
                } else
                    printer.printf("[%s]: %s [%s(%.1f)-> %s(%.1f)", time.toString(), entry.getBestGuess(), entry.getType().name().toLowerCase(), entry.getDamage(), entry.getVictim(), entry.getVictimHealth());
            }
            else if (o instanceof String) {
                printer.printf("[%s]: %s", time.toString(), o);
            }
            else if (o instanceof Game.GamePhase) {
                Game.GamePhase phase = (Game.GamePhase) o;
                printer.printf("[%s]: Started %s phase", time.toString(), phase.name().toLowerCase());
            }
            printer.println();
        }
        printer.close();
    }

    private static class GameLogDamageEntry {
        private final String damager;
        private final String victim;
        private final String bestGuess;
        private final EntityDamageEvent.DamageCause type;
        private final double damage;
        private final double damagerHealth;
        private final double victimHealth;

        public GameLogDamageEntry(Player damager, Player victim, EntityDamageEvent.DamageCause type, double damage, String bestGuess) {
            if (damager == null)
                this.damager = "";
            else
                this.damager = damager.getName();
            this.victim = victim.getName();
            this.type = type;
            this.damage = damage;
            if (damager != null)
                this.damagerHealth = damager.getHealth();
            else
                this.damagerHealth = 0;
            this.victimHealth = victim.getHealth();
            this.bestGuess = bestGuess == null ? "" : bestGuess;
        }

        public double getDamagerHealth() {
            return damagerHealth;
        }

        public double getVictimHealth() {
            return victimHealth;
        }

        public String getDamager() {
            return damager;
        }

        public String getVictim() {
            return victim;
        }

        public String getBestGuess() {
            return bestGuess;
        }

        public EntityDamageEvent.DamageCause getType() {
            return type;
        }

        public double getDamage() {
            return damage;
        }
    }

    private boolean isMyGame(Game game) {
        return this.game.equals(game);
    }
}
