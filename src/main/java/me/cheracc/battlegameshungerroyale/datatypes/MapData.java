package me.cheracc.battlegameshungerroyale.datatypes;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;

public class MapData {
    private final FileConfiguration config;
    private final File mapDirectory;
    private Material icon;
    private String mapName;
    private String mapCreator;
    private String mapDescription;
    private double averageLength;
    private int timesPlayed;

    private boolean useBorder;
    private int borderRadius;
    private int borderCenterX;
    private int borderCenterY;
    private int borderCenterZ;

    private Material spawnBlockType = null;
    private int spawnRadius;
    private int spawnCenterX;
    private int spawnCenterY;
    private int spawnCenterZ;

    private MapData(FileConfiguration config, File mapDirectory) {
        this.config = config;
        this.mapDirectory = mapDirectory;
        mapName = config.getString("name",  mapDirectory.getName());
        mapCreator = config.getString("creator", "unknown");
        mapDescription = config.getString("description", "there is no description for this map");
        averageLength = config.getInt("average length", 0);
        timesPlayed = config.getInt("times played", 0);
        icon = Material.getMaterial(config.getString("icon", "map").toUpperCase());
        if (config.contains("spawn block type"))
            spawnBlockType = Material.getMaterial(config.getString("spawn block type").toUpperCase());
        else
            spawnBlockType = null;
        borderCenterX = config.getInt("border center.x", 0);
        borderCenterY = config.getInt("border center.y", 100);
        borderCenterZ = config.getInt("border center.z", 0);
        spawnRadius = config.getInt("spawn radius", 10);
        useBorder = config.getBoolean("use border", false);
        borderRadius = config.getInt("border radius", 50);
        spawnCenterX = config.getInt("spawn center.x", 0);
        spawnCenterY = config.getInt("spawn center.y", 100);
        spawnCenterZ = config.getInt("spawn center.z", 0);
    }

    public static MapData createFromConfig(FileConfiguration config, File mapDirectory) {
        return new MapData(config, mapDirectory);
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

    public void setSpawnBlockType(Material material) {
        this.spawnBlockType = material;
        config.set("spawn block type", spawnBlockType.name().toLowerCase());
        saveConfig();
    }

    public void setSpawnRadius(int radius) {
        this.spawnRadius = radius;
        config.set("spawn radius", spawnRadius);
        saveConfig();
    }

    public void setSpawnCenter(Location location) {
        spawnCenterX = (int) Math.floor(location.getX());
        spawnCenterY = (int) Math.floor(location.getY());
        spawnCenterZ = (int) Math.floor(location.getZ());
        config.set("spawn center.x", spawnCenterX);
        config.set("spawn center.y", spawnCenterY);
        config.set("spawn center.z", spawnCenterZ);
        saveConfig();
    }

    public void setBorderCenter(Location location) {
        borderCenterX = (int) Math.floor(location.getX());
        borderCenterY = (int) Math.floor(location.getY());
        borderCenterZ = (int) Math.floor(location.getZ());
        config.set("border center.x", borderCenterX);
        config.set("border center.y", borderCenterY);
        config.set("border center.z", borderCenterZ);
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
        File configFile = new File(mapDirectory, "mapconfig.yml");

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

    public FileConfiguration getConfig() {
        return config;
    }

    public Location getBorderCenter(World world) {
        return new Location(world, borderCenterX + 0.5, borderCenterY, borderCenterZ + 0.5);
    }

    public Location getSpawnCenter(World world) {
        return new Location(world, spawnCenterX + 0.5, spawnCenterY, spawnCenterZ + 0.5);
    }

    public boolean isUseBorder() {
        return useBorder;
    }

    public int getBorderRadius() {
        return borderRadius;
    }

    public Material getSpawnBlockType() {
        return spawnBlockType;
    }

    public int getSpawnRadius() {
        return spawnRadius;
    }
}
