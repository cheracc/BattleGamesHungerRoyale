package me.cheracc.battlegameshungerroyale.datatypes;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.InventorySerializer;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class EquipmentSet {
    private final static NamespacedKey EQUIPMENT_KEY = new NamespacedKey(BGHR.getPlugin(), "equipment");
    private final static EquipmentSlot[] SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFF_HAND };

    private final UUID uuid = UUID.randomUUID();
    private final Map<EquipmentSlot, ItemStack> armor = new HashMap<>();
    private final List<ItemStack> otherItems = new ArrayList<>();

    private EquipmentSet() {

    }

    public static EquipmentSet newEquipmentSet() {
        EquipmentSet set = new EquipmentSet();

        for (EquipmentSlot slot : SLOTS)
            set.setArmor(slot, null);

        return set;
    }

    public String serializeAsBase64() {
        List<ItemStack> orderedList = new ArrayList<>();

        for (EquipmentSlot slot : SLOTS) {
            ItemStack item = armor.get(slot);
            if (item == null)
                item = new ItemStack(Material.AIR);
            orderedList.add(item);
        }
        orderedList.addAll(otherItems);

        return InventorySerializer.itemStackArrayToBase64(orderedList.toArray(new ItemStack[0]));
    }

    public void loadItemsFromBase64(String data) {
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFF_HAND };
        try {
            ItemStack[] items = InventorySerializer.itemStackArrayFromBase64(data);
            for (int i = 0; i < items.length - 1; i++) {
                if (i < slots.length - 1)
                    setArmor(slots[i], items[i]);
                else
                    addOtherItem(items[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<EquipmentSlot, ItemStack> getArmor() {
        return new HashMap<>(armor);
    }

    public List<ItemStack> getOtherItems() {
        return new ArrayList<>(otherItems);
    }

    public void setArmor(@NotNull EquipmentSlot slot, @NotNull ItemStack armor) {
        if (slot != EquipmentSlot.HAND) {
            this.armor.put(slot, armor);
        }
    }

    public void addOtherItem(@NotNull ItemStack item) {
        if (otherItems.size() < 3) {
            otherItems.add(item);
            Bukkit.getLogger().info("Added " + item.getI18NDisplayName());
        }
        Bukkit.getLogger().info("size: " + otherItems.size());
    }

    public void removeArmor(@NotNull EquipmentSlot slot) {
        armor.remove(slot);
    }

    public void removeItem(@NotNull ItemStack item) {
        otherItems.remove(item);
        armor.values().remove(item);
    }

    public void equip(Player p) {
        for (Map.Entry<EquipmentSlot, ItemStack> e : armor.entrySet()) {
            p.getInventory().setItem(e.getKey(), Tools.tagAsPluginItem(tagItem(e.getValue())));
        }
        for (ItemStack i : getOtherItems()) {
            p.getInventory().setItem(Tools.getLastEmptyHotbarSlot(p), Tools.tagAsPluginItem(tagItem(i)));
        }
    }

    public void unequip(Player p) {
        for (int i = 0; i <= 8; i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (hasTag(item))
                p.getInventory().setItem(i, null);
            if (hasTag(p.getInventory().getHelmet()))
                p.getInventory().setHelmet(null);
            if (hasTag(p.getInventory().getChestplate()))
                p.getInventory().setChestplate(null);
            if (hasTag(p.getInventory().getLeggings()))
                p.getInventory().setLeggings(null);
            if (hasTag(p.getInventory().getBoots()))
                p.getInventory().setBoots(null);
            if (hasTag(p.getInventory().getItemInOffHand()))
                p.getInventory().setItemInOffHand(null);
            if (hasTag(p.getInventory().getItemInMainHand()))
                p.getInventory().setItemInMainHand(null);
        }
    }

    private boolean hasTag(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String id = meta.getPersistentDataContainer().get(EQUIPMENT_KEY, PersistentDataType.STRING);

        return (id != null && id.equalsIgnoreCase(uuid.toString()));
    }

    private ItemStack tagItem(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(EQUIPMENT_KEY, PersistentDataType.STRING, uuid.toString());
        item.setItemMeta(meta);
        return item;
    }
}
