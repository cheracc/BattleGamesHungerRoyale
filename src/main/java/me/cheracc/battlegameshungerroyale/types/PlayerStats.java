package me.cheracc.battlegameshungerroyale.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PlayerStats {
    UUID uuid;
    long totalTimePlayed;
    int played;
    int kills;
    int killStreak;
    int deaths;
    int wins;
    int secondPlaceFinishes;
    int gamesQuit;
    int damageDealt;
    int damageTaken;
    int activeAbilitiesUsed;

    List<String> usedKits = new ArrayList<>();
    List<String> playedMaps = new ArrayList<>();

    public PlayerStats(UUID id) {
        this.uuid = id;
        this.played = 0;
        this.kills = 0;
        this.killStreak = 0;
        this.deaths = 0;
        this.wins = 0;
        this.secondPlaceFinishes = 0;
        this.totalTimePlayed = 0;
        this.gamesQuit = 0;
        this.damageDealt = 0;
        this.damageTaken = 0;
        this.activeAbilitiesUsed = 0;
    }

    public String[] getUsedKits() {
        return usedKits.toArray(new String[0]);
    }

    public String[] getPlayedMaps() {
        return usedKits.toArray(new String[0]);
    }

    public int getPlayed() {
        return played;
    }

    public int getKills() {
        return kills;
    }

    public int getKillStreak() {
        return killStreak;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getWins() {
        return wins;
    }

    public int getSecondPlaceFinishes() {
        return secondPlaceFinishes;
    }

    public long getTotalTimePlayed() {
        return totalTimePlayed;
    }

    public int getGamesQuit() {
        return gamesQuit;
    }

    public int getDamageDealt() {
        return damageDealt;
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public int getActiveAbilitiesUsed() {
        return activeAbilitiesUsed;
    }

    public void setPlayed(int value) {
        played = value;
    }

    public void setKills(int value) {
        kills = value;
    }

    public void setKillStreak(int value) {
        killStreak = value;
    }

    public void setDeaths(int value) {
        deaths = value;
    }

    public void setWins(int value) {
        wins = value;
    }

    public void setSecondPlaceFinishes(int value) {
        secondPlaceFinishes = value;
    }

    public void setTotalTimePlayed(long value) {
        totalTimePlayed = value;
    }

    public void setQuits(int value) {
        gamesQuit = value;
    }

    public void setDamageDealt(int value) {
        damageDealt = value;
    }

    public void setDamageReceived(int value) {
        damageTaken = value;
    }

    public void setActiveAbilitiesUsed(int value) {
        activeAbilitiesUsed = value;
    }

    public void setUsedKits(Object[] usedKits) {
        for (Object o : usedKits) {
            this.usedKits.add((String) o);
        }
    }

    public void setPlayedMaps(Object[] playedMaps) {
        for (Object o : playedMaps)
            this.playedMaps.add((String) o);
    }
}
