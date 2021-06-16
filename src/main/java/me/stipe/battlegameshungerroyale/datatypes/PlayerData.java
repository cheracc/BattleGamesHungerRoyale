package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerData {
    private final UUID uuid;
    PlayerStats stats;

    Game currentGame;
    Kit kit;
    Map<Kit, List<ItemStack>> abilityItems = new HashMap<>();

    public PlayerData(Player p) {
        uuid = p.getUniqueId();
    }

    public void registerKit(Kit kit, boolean clearInventory) {
        if (!(this.kit == null || this.kit.equals(kit))) {
            removeKitItems(this.kit);
        }
        if (clearInventory)
            getPlayer().getInventory().clear();
        this.kit = kit;
        kit.outfitPlayer(getPlayer(), this);
    }

    public void registerAbilityItem(Kit kit, ItemStack item) {
        List<ItemStack> items = new ArrayList<>();

        if (abilityItems.containsKey(kit))
            items.addAll(abilityItems.get(kit));

        items.add(item);
        abilityItems.put(kit, items);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void removeKitItems(Kit kit) {
        for (ItemStack item : abilityItems.get(kit)) {
            int slot = findHotbarSlot(kit, item);
            if (slot < 0) {
                Bukkit.getLogger().warning("cannot find a kit item"); //TODO
                return;
            }
            getPlayer().getInventory().setItem(slot, null);
        }
    }

    private int findHotbarSlot(Kit kit, ItemStack item) {
        for (int i = 0; i < 9; i++) {
            ItemStack ithItem = getPlayer().getInventory().getItem(i);
            if (Ability.getUuid(item).equals(Ability.getUuid(ithItem)))
                return i;
        }
        return -1;
    }
}
