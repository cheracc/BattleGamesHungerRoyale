package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.events.GameDamageEvent;
import me.stipe.battlegameshungerroyale.tools.Tools;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.*;

public class GameLog {
    private final long startTime;
    private final String mapName;
    private String winner = "";
    private final Map<Long, Object> entries = new HashMap<>();
    private final List<String> players = new ArrayList<>();

    public GameLog(Game game) {
        this.startTime = System.currentTimeMillis();
        mapName = game.getMap().getMapName();
        for (Player p : game.getActivePlayers()) {
            players.add(p.getName());
        }
    }

    public void addDamageEntry(GameDamageEvent event) {
        GameLogDamageEntry entry = new GameLogDamageEntry(event.getAggressor(), event.getVictim(), event.getType(), event.getDamage());
        entries.put(System.currentTimeMillis(), entry);
    }

    public void addDeathEntry(Player player) {
        entries.put(System.currentTimeMillis(), player.getName());
    }

    public void addPhaseEntry(Game.GamePhase phase) {
        entries.put(System.currentTimeMillis(), phase);
    }

    public void finalizeLog(@NotNull Game game) {
        if (game.getWinner() != null)
            winner = game.getWinner().getName();

        try {
            saveLogFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLogFile() throws IOException {
        Instant timestamp = Instant.now();
        File gameLogFile = new File(BGHR.getPlugin().getDataFolder(), "gamelogs/" + timestamp.toString());

        FileWriter writer = new FileWriter(gameLogFile);
        PrintWriter print = new PrintWriter(writer);

        List<Long> entryTimes = new ArrayList<>(entries.keySet());
        Collections.sort(entryTimes);
        Collections.sort(players);

        StringBuilder playerString = new StringBuilder();
        for (String s : players) {
            if (playerString.length() > 0)
                playerString.append(", ");
            playerString.append(s);
        }

        long elapsedTimeInSeconds = System.currentTimeMillis() - startTime / 1000;

        print.printf("Game Started: %s", Instant.ofEpochMilli(startTime));
        print.printf("Game Ended: %s", Instant.now());
        print.printf("Time elapsed: %s", Tools.secondsToMinutesAndSeconds((int) elapsedTimeInSeconds));
        print.printf("Map: %s", mapName);
        print.printf("Participants (%s): %s", players.size(), playerString);
        print.printf("Winner: %s", winner);

        for (Long l : entryTimes) {
            Object o = entries.get(l);
            Instant time = Instant.ofEpochMilli(l);

            if (o instanceof GameLogDamageEntry) {
                GameLogDamageEntry entry = (GameLogDamageEntry) o;
                if (entry.getDamager() != null) {
                    print.printf("[%s]: %s did %s damage to %s with %s", time.toString(), entry.getDamager(), entry.getDamage(), entry.getVictim(), entry.getType().name().toLowerCase());
                } else
                    print.printf("[%s]: %s took %s damage from %s", time.toString(), entry.getVictim(), entry.getDamage(), entry.getType().name().toLowerCase());
            }
            else if (o instanceof String) {
                print.printf("[%s]: %s died.", time.toString(), o);
            }
            else if (o instanceof Game.GamePhase) {
                Game.GamePhase phase = (Game.GamePhase) o;
                print.printf("[%s]: Started %s phase", time.toString(), phase.name().toLowerCase());
            }
        }
    }

    private static class GameLogDamageEntry {
        private final String damager;
        private final String victim;
        private final EntityDamageEvent.DamageCause type;
        private final double damage;

        public GameLogDamageEntry(Player damager, Player victim, EntityDamageEvent.DamageCause type, double damage) {
            if (damager == null)
                this.damager = "";
            else
                this.damager = damager.getName();
            this.victim = victim.getName();
            this.type = type;
            this.damage = damage;
        }

        public String getDamager() {
            return damager;
        }

        public String getVictim() {
            return victim;
        }

        public EntityDamageEvent.DamageCause getType() {
            return type;
        }

        public double getDamage() {
            return damage;
        }
    }
}
