package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.InventorySerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String[] lastInventory;
    private Location lastLocation;
    private PlayerStats stats;
    private Kit kit;

    public PlayerData(Player p) {
        uuid = p.getUniqueId();
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

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void removeKit(Kit kit) {
        kit.disrobePlayer(getPlayer());
        this.kit = null;
    }

    public boolean hasKit(Kit kit) {
        return this.kit != null && this.kit.equals(kit);
    }
}
