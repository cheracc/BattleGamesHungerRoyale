package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.*;
import me.cheracc.battlegameshungerroyale.tools.InventorySerializer;
import me.cheracc.battlegameshungerroyale.tools.Logr;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerData {
    private final UUID uuid;
    private String[] lastInventory;
    private Location lastLocation;
    private final PlayerStats stats;
    private final PlayerSettings settings;
    private Kit kit;
    private long joinTime;
    private boolean modified = false;
    private boolean loaded = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        settings = new PlayerSettings();
        stats = new PlayerStats(uuid);
        // load the stats and if new, mark as modified so they can be saved on the next update
        loadAsynchronously(foundRecords -> {
            if (foundRecords)
                Logr.info("Finished loading playerdata for " + uuid);
            else
                Logr.info("Created new playerdata for " + uuid);
            modified = !foundRecords;
            PlayerManager.getInstance().thisPlayerIsLoaded(this);
            loaded = true;

            if (getPlayer() != null)
                restorePlayer();
        });
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isLoaded() {
        return loaded;
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

    public void setJoinTime(long value) {
        joinTime = value;
    }

    public Kit getKit() {
        return kit;
    }

    public void registerKit(Kit kit, boolean clearInventory) {
        Player p = getPlayer();
        if (p == null) return;

        if (!kit.isEnabled() && !p.hasPermission("bghr.admin.kits.disabled")) {
            p.sendMessage(Tools.componentalize("That kit is disabled"));
            return;
        }


        if (this.kit != null)
            removeKit(this.kit);

        if (clearInventory)
            p.getInventory().clear();

        this.kit = kit;


        if (MapManager.getInstance().isThisAGameWorld(p.getWorld()) || BGHR.getPlugin().getConfig().getBoolean("main world.kits useable in main world", false)) {
            kit.outfitPlayer(p);
        } else {
            p.sendMessage(Component.text("Kit&e " + kit.getName() + " &fwill be equipped when you join a game."));
        }
    }

    public void removeKit(Kit kit) {
        kit.disrobePlayer(getPlayer());
        this.kit = null;
    }

    public boolean hasKit(Kit kit) {
        return this.kit != null && this.kit.equals(kit);
    }

    public void restorePlayer() {
        Player p = getPlayer();
        if (settings.isShowMainScoreboard())
            p.setScoreboard(GameManager.getInstance().getMainScoreboard());

        writeSavedInventoryToPlayer();

        if (getSettings().getDefaultKit() != null) {
            Kit kit = KitManager.getInstance().getKit(getSettings().getDefaultKit());
            if (kit != null)
                registerKit(kit, false);
        }
    }

    public Location getLastLocation() {
        return lastLocation.clone();
    }

    public void setLastLocation(Location loc) {
        if (!loc.equals(lastLocation)) {
            lastLocation = loc;
            modified = true;
        }
    }

    public void saveInventory(boolean clear) {
        String[] currentInventory = InventorySerializer.playerInventoryToBase64(getPlayer());

        if (!Arrays.equals(currentInventory, lastInventory)) {
            lastInventory = currentInventory;
            modified = true;
        }

        if (clear) {
            Player p = getPlayer();
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            p.getEnderChest().clear();
        }
    }

    public String[] getSavedInventory() {
        return lastInventory;
    }

    private void writeSavedInventoryToPlayer() {
        try {
            if (lastInventory != null) {
                InventorySerializer.resetPlayerInventoryFromBase64(getPlayer(), lastInventory);
            }
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

    private void loadAsynchronously(Consumer<Boolean> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean success = load();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(success);
                    }
                }.runTask(BGHR.getPlugin());
            }
        }.runTaskAsynchronously(BGHR.getPlugin());
    }


    public boolean load() {
        boolean found = false;

        try (Connection con = DatabaseManager.get().getConnection();
        PreparedStatement loadStatsQuery = con.prepareStatement("SELECT * FROM player_stats WHERE uuid=?");
        PreparedStatement loadSettingsQuery = con.prepareStatement("SELECT * FROM player_settings WHERE uuid=?");
        PreparedStatement loadDataQuery = con.prepareStatement("SELECT * FROM player_data WHERE uuid=?")) {

            loadStatsQuery.setString(1, uuid.toString());
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
                stats.setChestsOpened(result.getInt("chests"));
                result.close();
            }

            loadSettingsQuery.setString(1, uuid.toString());
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

            loadDataQuery.setString(1, uuid.toString());
            result = loadDataQuery.executeQuery();
            if (result.isBeforeFirst()) {
                result.next();

                if (!result.isLast()) {
                    Bukkit.getLogger().warning("attempting to load player data resulted in duplicate uuids (this shouldn't happen)");
                    return false;
                }

                found = true;
                World w = Bukkit.getWorld(UUID.fromString(result.getString("lastworld")));
                if (w == null)
                    w = Bukkit.getWorlds().get(0);
                int x = result.getInt("lastx");
                int y = result.getInt("lasty");
                int z = result.getInt("lastz");
                lastLocation = new Location(w, x, y, z);

                String main = result.getString("inventory");
                String armor = result.getString("armor");
                String enderChest = result.getString("enderchest");
                lastInventory = new String[] {main, armor, enderChest};
                result.close();
            }

            return found;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save() {
        try (Connection con = DatabaseManager.get().getConnection();
             PreparedStatement updateSettings = con.prepareStatement(
                     "INSERT INTO player_settings (uuid,showmain,showhelp,defaultkit) VALUES (?,?,?,?) " +
                             "ON DUPLICATE KEY UPDATE " +
                             "showmain=VALUES(showmain)," +
                             "showhelp=VALUES(showhelp)," +
                             "defaultkit=VALUES(defaultkit);");
             PreparedStatement updateStats = con.prepareStatement(
                     "INSERT INTO player_stats (uuid,played,kills,killstreak,deaths,wins,secondplaces,totaltime,quits,damagedealt,damagetaken,activeabilities,chests) VALUES " +
                             "(?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
                             "played=VALUES(played)," +
                             "kills=VALUES(kills)," +
                             "killstreak=VALUES(killstreak)," +
                             "deaths=VALUES(deaths)," +
                             "wins=VALUES(wins)," +
                             "secondplaces=VALUES(secondplaces)," +
                             "totaltime=VALUES(totaltime)," +
                             "quits=VALUES(quits)," +
                             "damagedealt=VALUES(damagedealt)," +
                             "damagetaken=VALUES(damagetaken)," +
                             "activeabilities=VALUES(activeabilities)," +
                             "chests=VALUES(chests)");
             PreparedStatement updateData = con.prepareStatement(
                     "INSERT INTO player_data (uuid,lastworld,lastx,lasty,lastz,inventory,armor,enderchest) VALUES (?,?,?,?,?,?,?,?) " +
                             "ON DUPLICATE KEY UPDATE " +
                             "lastworld=VALUES(lastworld)," +
                             "lastx=VALUES(lastx)," +
                             "lasty=VALUES(lasty)," +
                             "lastz=VALUES(lastz)," +
                             "inventory=VALUES(inventory)," +
                             "armor=VALUES(armor)," +
                             "enderchest=VALUES(enderchest)")) {

            updateSettings.setString(1, getUuid().toString());
            updateSettings.setBoolean(2, settings.isShowMainScoreboard());
            updateSettings.setBoolean(3, settings.isShowHelp());
            updateSettings.setString(4, settings.getDefaultKit());
            updateSettings.executeUpdate();

            updateStats.setString(1, getUuid().toString());
            updateStats.setInt(2, stats.getPlayed());
            updateStats.setInt(3, stats.getKills());
            updateStats.setInt(4, stats.getKillStreak());
            updateStats.setInt(5, stats.getDeaths());
            updateStats.setInt(6, stats.getWins());
            updateStats.setInt(7, stats.getSecondPlaceFinishes());
            updateStats.setLong(8, stats.getTotalTimePlayed());
            updateStats.setInt(9, stats.getGamesQuit());
            updateStats.setInt(10, stats.getDamageDealt());
            updateStats.setInt(11, stats.getDamageTaken());
            updateStats.setInt(12, stats.getActiveAbilitiesUsed());
            updateStats.setInt(13, stats.getChestsOpened());
            updateStats.execute();

            updateData.setString(1, getUuid().toString());
            updateData.setString(2, getLastLocation().getWorld().getUID().toString());
            updateData.setInt(3, (int) getLastLocation().getX());
            updateData.setInt(4, (int) getLastLocation().getY());
            updateData.setInt(5, (int) getLastLocation().getZ());
            updateData.setString(6, getSavedInventory()[0]);
            updateData.setString(7, getSavedInventory()[1]);
            updateData.setString(8, getSavedInventory()[2]);
            updateData.execute();

            modified = false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
