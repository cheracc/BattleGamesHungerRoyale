package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.DatabaseManager;
import me.cheracc.battlegameshungerroyale.managers.Logr;
import me.cheracc.battlegameshungerroyale.tools.InventorySerializer;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.PassiveAbility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerData {
    private final UUID uuid;
    private final PlayerStats stats;
    private final PlayerSettings settings;
    private final boolean loaded = false;
    private String[] lastInventory;
    private Location lastLocation;
    private Kit kit;
    private long joinTime;
    private boolean modified = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        settings = new PlayerSettings();
        stats = new PlayerStats(uuid);
        // load the stats and if new, mark as modified so they can be saved on the next update
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

    public void assignKit(Kit kit, boolean clearInventory) {
        Player p = getPlayer();
        if (p == null) return;

        if (!kit.isEnabled() && !p.hasPermission("bghr.admin.kits.disabled")) {
            p.sendMessage(Trans.lateToComponent("You are trying to equip a disabled kit and you do not have permission. Please report this."));
            return;
        }
        if (this.kit != null)
            removeKit();

        if (clearInventory)
            p.getInventory().clear();

        this.kit = kit;
    }

    private void removeAllKitItems() {
        Player player = getPlayer();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getItemMeta() == null)
                continue;
            if (Tools.isPluginItem(item))
                player.getInventory().remove(item);
        }
        ItemStack item = player.getInventory().getItemInOffHand();
        if (item != null && item.getItemMeta() != null) {
            if (Tools.isPluginItem(item))
                player.getInventory().setItemInOffHand(null);
        }

        kit.getEquipment().unequip(player);
        for (Ability ability : kit.getAbilities()) {
            if (ability instanceof PassiveAbility) {
                ((PassiveAbility) ability).deactivate(player);
            }
        }

        player.updateInventory();
    }

    public void removeKit() {
        removeAllKitItems();
        this.kit = null;
    }

    public boolean hasKit(Kit kit) {
        return this.kit != null && this.kit.equals(kit);
    }

    public Location getLastLocation() {
        if (lastLocation != null)
            return lastLocation.clone();
        setLastLocation(getPlayer().getLocation());
        return lastLocation.clone();
    }

    public void setLastLocation(Location loc) {
        if (!loc.equals(lastLocation)) {
            lastLocation = loc;
            modified = true;
        }
    }

    public void saveLocationAndInventory(boolean clear) {
        setLastLocation();
        saveInventory(clear);
        setModified(true);
    }

    public void setLastLocation() {
        if (getPlayer() != null) {
            setLastLocation(getPlayer().getLocation());
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
        if (lastInventory == null || lastInventory[0] == null)
            saveInventory(false);
        return lastInventory;
    }

    public void writeSavedInventoryToPlayer() {
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

    public CompletableFuture<PlayerData> loadAsynchronously(DatabaseManager db, Logr logr, BGHR plugin) {
        CompletableFuture<PlayerData> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                load(db, logr);
                future.complete(PlayerData.this);
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    private boolean load(DatabaseManager db, Logr logr) {
        boolean found = false;

        try (Connection con = db.getConnection();
             PreparedStatement loadStatsQuery = con.prepareStatement("SELECT * FROM player_stats WHERE uuid=?");
             PreparedStatement loadSettingsQuery = con.prepareStatement("SELECT * FROM player_settings WHERE uuid=?");
             PreparedStatement loadDataQuery = con.prepareStatement("SELECT * FROM player_data WHERE uuid=?")) {

            loadStatsQuery.setString(1, uuid.toString());
            ResultSet result = loadStatsQuery.executeQuery();

            if (result.isBeforeFirst()) {
                result.next();

                if (!result.isLast()) {
                    logr.warn("attempting to load player data resulted in duplicate uuids (this shouldn't happen)");
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
                stats.setItemsLooted(result.getInt("itemslooted"));
                stats.setArrowsShot(result.getInt("arrowsshot"));
                stats.setMonstersKilled(result.getInt("monsterskilled"));
                stats.setAnimalsKilled(result.getInt("animalskilled"));
                stats.setFoodEaten(result.getInt("foodeaten"));
                result.close();
            }

            loadSettingsQuery.setString(1, uuid.toString());
            result = loadSettingsQuery.executeQuery();
            if (result.isBeforeFirst()) {
                result.next();

                if (!result.isLast()) {
                    logr.warn("attempting to load player data resulted in duplicate uuids (this shouldn't happen)");
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
                    logr.warn("attempting to load player data resulted in duplicate uuids (this shouldn't happen)");
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
                lastInventory = new String[]{main, armor, enderChest};
                result.close();
            }

            return found;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save(DatabaseManager db) {
        try (Connection con = db.getConnection();
             PreparedStatement updateSettings = con.prepareStatement(
                     "INSERT INTO player_settings (uuid,showmain,showhelp,defaultkit) VALUES (?,?,?,?) " +
                             "ON DUPLICATE KEY UPDATE " +
                             "showmain=VALUES(showmain)," +
                             "showhelp=VALUES(showhelp)," +
                             "defaultkit=VALUES(defaultkit);");
             PreparedStatement updateStats = con.prepareStatement(
                     "INSERT INTO player_stats (uuid,played,kills,killstreak,deaths,wins,secondplaces,totaltime,quits,damagedealt,damagetaken,activeabilities,chests,itemslooted,arrowsshot,monsterskilled,animalskilled,foodeaten) VALUES " +
                             "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " +
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
                             "chests=VALUES(chests)," +
                             "itemslooted=VALUES(itemslooted)," +
                             "arrowsshot=VALUES(arrowsshot)," +
                             "monsterskilled=VALUES(monsterskilled)," +
                             "animalskilled=VALUES(animalskilled)," +
                             "foodeaten=VALUES(foodeaten)");
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
            updateStats.setInt(14, stats.getItemsLooted());
            updateStats.setInt(15, stats.getArrowsShot());
            updateStats.setInt(16, stats.getMonstersKilled());
            updateStats.setInt(17, stats.getAnimalsKilled());
            updateStats.setInt(18, stats.getFoodEaten());
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

    public String getName() {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }
}
