package me.cheracc.battlegameshungerroyale.types;

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
    int chestsOpened;
    int currentKillStreak;
    int itemsLooted;
    int arrowsShot;

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

    public int getChestsOpened() {
        return chestsOpened;
    }

    public void setChestsOpened(int value) {
        chestsOpened = value;
    }

    public void addChestOpened() {
        chestsOpened++;
    }

    public void setPlayed(int value) {
        played = value;
    }

    public void addGamePlayed() {
        played++;
    }

    public void setKills(int value) {
        kills = value;
    }

    public void addKill() {
        kills++;
        currentKillStreak++;
        if (currentKillStreak > killStreak)
            killStreak = currentKillStreak;
    }

    public void setKillStreak(int value) {
        killStreak = value;
    }

    public void setDeaths(int value) {
        deaths = value;
    }

    public void addDeath() {
        deaths++;
        currentKillStreak = 0;
    }

    public void setWins(int value) {
        wins = value;
    }

    public void addWin() {
        wins++;
    }

    public void setSecondPlaceFinishes(int value) {
        secondPlaceFinishes = value;
    }

    public void addSecondPlaceFinish() {
        secondPlaceFinishes++;
    }

    public void setTotalTimePlayed(long value) {
        totalTimePlayed = value;
    }

    public void addToTimePlayed(long value) {
        totalTimePlayed += value;
    }

    public void setQuits(int value) {
        gamesQuit = value;
    }

    public void addGameQuit() {
        gamesQuit++;
    }

    public void setDamageDealt(int value) {
        damageDealt = value;
    }

    public void addDamageDealt(double value) {
        damageDealt += value;
    }

    public void setDamageReceived(int value) {
        damageTaken = value;
    }

    public void addDamageReceived(double value) {
        damageTaken += value;
    }

    public void setActiveAbilitiesUsed(int value) {
        activeAbilitiesUsed = value;
    }

    public void addActiveAbilityUsed() {
        activeAbilitiesUsed++;
    }

}
