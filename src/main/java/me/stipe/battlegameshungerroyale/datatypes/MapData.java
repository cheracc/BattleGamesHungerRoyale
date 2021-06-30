package me.stipe.battlegameshungerroyale.datatypes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapData {
    private final FileConfiguration config;
    private final File mapDirectory;
    private final List<World> createdWorlds;
    private String mapName;
    private String mapCreator;
    private String mapDescription;
    private int timesPlayed;
    private double averageLength;
    private Material icon;

    private final boolean isLobby;
    private int centerX;
    private int centerY;
    private int centerZ;
    private boolean useBorder;
    private int borderRadius;

    public MapData(FileConfiguration config, File mapDirectory, boolean isLobby) {
        this.config = config;
        this.mapDirectory = mapDirectory;
        this.createdWorlds = new ArrayList<>();
        mapName = config.getString("name",  mapDirectory.getName());
        mapCreator = config.getString("creator", "unknown");
        mapDescription = config.getString("description", "there is no description for this map");
        averageLength = config.getInt("average length", 0);
        timesPlayed = config.getInt("times played", 0);
        icon = Material.getMaterial(config.getString("icon", "map").toUpperCase());
        this.isLobby = isLobby;
        centerX = config.getInt("center.x", 0);
        centerY = config.getInt("center.y", 0);
        centerZ = config.getInt("center.z", 0);
        useBorder = config.getBoolean("use border", false);
        borderRadius = config.getInt("border radius", 50);
    }

    public void setIcon(Material material) {
        this.icon = material;
        config.set("icon", icon.name().toLowerCase());
        saveConfig();
    }

    public void setName(String name) {
        this.mapName = ChatColor.translateAlternateColorCodes('&', name);
        config.set("name", mapName);
        saveConfig();
    }

    public void setDescription(String name) {
        this.mapDescription = ChatColor.translateAlternateColorCodes('&', name);
        config.set("description", mapDescription);
        saveConfig();
    }

    public void setCreator(String name) {
        this.mapCreator = ChatColor.translateAlternateColorCodes('&', name);
        config.set("creator", mapCreator);
        saveConfig();
    }

    public void setCenter(Location location) {
        centerX = (int) Math.floor(location.getX());
        centerY = (int) Math.floor(location.getY());
        centerZ = (int) Math.floor(location.getZ());
        config.set("center.x", centerX);
        config.set("center.y", centerY);
        config.set("center.z", centerZ);
        saveConfig();
    }

    public void setBorderRadius(int size) {
        borderRadius = size;
        config.set("border radius", borderRadius);
        saveConfig();
    }

    public void toggleUseBorder() {
        useBorder = !useBorder;
        config.set("use border", useBorder);
        saveConfig();
    }

    public void saveConfig() {
        File configFile;

        if (isLobby())
            configFile = new File(mapDirectory, "lobby.yml");
        else
            configFile = new File(mapDirectory, "mapconfig.yml");

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addGamePlayed(int length) {
        averageLength = (averageLength * timesPlayed + length) / (timesPlayed + 1);
        timesPlayed++;
        config.set("average length", averageLength);
        config.set("times played", timesPlayed);
        saveConfig();
    }

    public File getMapDirectory() {
        return mapDirectory;
    }

    public String getMapName() {
        return mapName;
    }

    public String getMapDescription() {
        return mapDescription;
    }

    public int getTimesPlayed() {
        return timesPlayed;
    }

    public double getAverageLength() {
        return averageLength;
    }

    public Material getIcon() {
        return icon;
    }

    public String getMapCreator() {
        return mapCreator;
    }

    public boolean isLobby() {
        return isLobby;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public boolean isUseBorder() {
        return useBorder;
    }

    public int getBorderRadius() {
        return borderRadius;
    }
}
