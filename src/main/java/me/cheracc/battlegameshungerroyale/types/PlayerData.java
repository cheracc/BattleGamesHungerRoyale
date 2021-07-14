package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.InventorySerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.*;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerData {
    private final UUID uuid;
    private String[] lastInventory;
    private Location lastLocation;
    private final PlayerStats stats;
    private final PlayerSettings settings;
    private Kit kit;
    private final long joinTime;
    private boolean modified = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        settings = new PlayerSettings();
        joinTime = System.currentTimeMillis();
        stats = new PlayerStats(uuid);
        // load the stats and if new, mark as modified so they can be saved on the next update
        loadAsynchronously(foundRecords -> modified = !foundRecords);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public PlayerSettings getSettings() {
        return settings;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public Kit getKit() {
        return kit;
    }

    public void registerKit(Kit kit, boolean clearInventory) {
        if (this.kit != null)
            removeKit(this.kit);

        if (clearInventory)
            getPlayer().getInventory().clear();

        this.kit = kit;

        getPlayer().sendMessage(Component.text("Your chosen Kit has been set to " + kit.getName()));

        if (MapManager.getInstance().isThisAGameWorld(getPlayer().getWorld()) || BGHR.getPlugin().getConfig().getBoolean("main world.kits useable in main world", false)) {
            kit.outfitPlayer(getPlayer());
        }
    }

    public void removeKit(Kit kit) {
        kit.disrobePlayer(getPlayer());
        this.kit = null;
    }

    public boolean hasKit(Kit kit) {
        return this.kit != null && this.kit.equals(kit);
    }

    public Location getLastLocation() {
        return lastLocation.clone();
    }

    public void setLastLocation(Location loc) {
        lastLocation = loc;
    }

    public void saveInventory() {
        lastInventory = InventorySerializer.playerInventoryToBase64(getPlayer().getInventory());
    }

    public void resetInventory() {
        Player p = getPlayer();
        if (lastInventory == null)
            return;
        try {
            ItemStack[] mainInventory = InventorySerializer.itemStackArrayFromBase64(lastInventory[0]);
            ItemStack[] armorInventory = InventorySerializer.itemStackArrayFromBase64(lastInventory[1]);

            p.closeInventory();
            p.getInventory().clear();
            p.setItemOnCursor(null);

            for (int i = 0; i < mainInventory.length; i++) {
                p.getInventory().setItem(i, mainInventory[i]);
            }
            for (int i = 0; i < armorInventory.length; i++) {
                p.getInventory().setArmorContents(armorInventory);
            }
            lastInventory = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean value) {
        modified = value;
    }

    public void loadAsynchronously(Consumer<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                callback.accept(load());
            }
        }.runTaskAsynchronously(BGHR.getPlugin());
    }

    public boolean load() {
        final BGHR plugin = BGHR.getPlugin();
        boolean found = false;

        try (Connection con = DriverManager.getConnection("jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/plugin_data;mode=MySQL;DATABASE_TO_LOWER=TRUE" , "bghr", "bghr");
        PreparedStatement loadStatsQuery = con.prepareStatement("SELECT * FROM player_stats WHERE uuid=?");
        PreparedStatement loadSettingsQuery = con.prepareStatement("SELECT * FROM player_settings WHERE uuid=?")) {

            loadStatsQuery.setObject(1, uuid);
            ResultSet result = loadStatsQuery.executeQuery();

            if (result.isBeforeFirst()) {
                result.next();

                if (!result.isLast()) {
                    Bukkit.getLogger().warning("attempting to load player data resulted in duplicate uuids (this shouldn't happen)");
                    return false;
                }
                found = true;
                stats.setPlayed(result.getInt("played"));
                stats.setKills(result.getInt("kills"));
                stats.setKillStreak(result.getInt("killstreak"));
                stats.setDeaths(result.getInt("deaths"));
                stats.setWins(result.getInt("wins"));
                stats.setSecondPlaceFinishes(result.getInt("secondplaces"));
                stats.setTotalTimePlayed(result.getLong("totaltime"));
                stats.setQuits(result.getInt("quits"));
                stats.setDamageDealt(result.getInt("damagedealt"));
                stats.setDamageReceived(result.getInt("damagetaken"));
                stats.setActiveAbilitiesUsed(result.getInt("activeabilities"));
                stats.setUsedKits((Object[]) result.getArray("usedkits").getArray());
                stats.setPlayedMaps((Object[]) result.getArray("playedmaps").getArray());
                result.close();
            }

            loadSettingsQuery.setObject(1, uuid);
            result = loadSettingsQuery.executeQuery();
            if (result.isBeforeFirst()) {
                result.next();

                if (!result.isLast()) {
                    Bukkit.getLogger().warning("attempting to load player data resulted in duplicate uuids (this shouldn't happen)");
                    return false;
                }

                found = true;
                settings.setShowScoreboard(result.getBoolean("showmain"));
                settings.setShowHelp(result.getBoolean("showhelp"));
                settings.setDefaultKit(result.getString("defaultkit"));
                result.close();
            }
            return found;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
