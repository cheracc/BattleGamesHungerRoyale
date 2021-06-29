package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.events.GameDamageEvent;
import me.stipe.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.*;

public class GameLog {
    private final long startTime;
    private final String mapName;
    private final Map<Long, Object> entries = new HashMap<>();
    private final Game game;

    public GameLog(Game game) {
        this.startTime = System.currentTimeMillis();
        this.game = game;
        mapName = game.getMap().getMapName();
    }

    public void addDamageEntry(GameDamageEvent event) {
        GameLogDamageEntry entry = new GameLogDamageEntry(event.getAggressor(), event.getVictim(), event.getType(), event.getDamage(), event.getBestGuess());
        entries.put(System.currentTimeMillis(), entry);
    }

    public void addDeathEntry(Player player) {
        entries.put(System.currentTimeMillis(), player.getName());
        try {
            saveLogFile();
        } catch (IOException e) {
            Bukkit.getLogger().warning("couldn't save gamelog file");
        }
    }

    public void addPhaseEntry(Game.GamePhase phase) {
        entries.put(System.currentTimeMillis(), phase);
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
    }

    private void saveLogFile() throws IOException {
        String timestamp = Instant.now().toString().split("\\.")[0].replace('T', '_').replace(':', '-');
        File gameLogFile = new File(BGHR.getPlugin().getDataFolder().getAbsolutePath() + "/gamelogs",  timestamp + ".log");

        if (!gameLogFile.exists())
            if (gameLogFile.getParentFile().mkdirs())
                Bukkit.getLogger().info("creating game log directory: " + gameLogFile.getAbsolutePath());

        FileWriter writer = new FileWriter(gameLogFile);
        PrintWriter print = new PrintWriter(writer);

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

        print.printf("Game Started: %s", Instant.ofEpochMilli(startTime));
        print.println();
        print.printf("Game Ended: %s", Instant.now());
        print.println();
        print.printf("Time elapsed: %s", Tools.secondsToMinutesAndSeconds(game.getCurrentGameTime()));
        print.println();
        print.printf("Map: %s", mapName);
        print.println();
        print.printf("Participants (%s): %s", players.size(), playerString);
        print.println();
        print.printf("Winner: %s", game.getWinner() == null ? "" : game.getWinner().getName());
        print.println();

        for (Long l : entryTimes) {
            Object o = entries.get(l);
            Instant time = Instant.ofEpochMilli(l);

            if (o instanceof GameLogDamageEntry) {
                GameLogDamageEntry entry = (GameLogDamageEntry) o;
                if (entry.getDamager() != null && entry.getDamager().length() > 1) {
                    print.printf("[%s]: %s(%s) [%s-> %s-%s(%s)", time.toString(), entry.getDamager(), entry.getDamagerHealth(),
                            entry.getType().name().toLowerCase(), entry.getVictim(), entry.getDamage(), entry.getVictimHealth());
                } else
                    print.printf("[%s]: %s [%s-> %s-%s(%s)", time.toString(), entry.getBestGuess(), entry.getType().name().toLowerCase(), entry.getVictim(), entry.getDamage(), entry.getVictimHealth());
            }
            else if (o instanceof String) {
                print.printf("[%s]: %s died.", time.toString(), o);
            }
            else if (o instanceof Game.GamePhase) {
                Game.GamePhase phase = (Game.GamePhase) o;
                print.printf("[%s]: Started %s phase", time.toString(), phase.name().toLowerCase());
            }
            print.println();
        }
        print.close();
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
}
