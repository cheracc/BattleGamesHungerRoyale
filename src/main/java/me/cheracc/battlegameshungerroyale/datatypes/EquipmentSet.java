package me.cheracc.battlegameshungerroyale.datatypes;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.InventorySerializer;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
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
    private enum EquipmentSetSlot { HEAD, CHEST, LEGS, FEET, OFF_HAND, HOTBAR_1, HOTBAR_2, HOTBAR_3;
        boolean isArmor() {
            return ordinal() < 5;
        }}

    private final UUID uuid = UUID.randomUUID();
    private final Map<EquipmentSetSlot, ItemStack> items = new HashMap<>();

    private EquipmentSet() {

    }

    public static EquipmentSet newEquipmentSet() {
        EquipmentSet set = new EquipmentSet();

        for (EquipmentSetSlot slot : EquipmentSetSlot.values())
            set.setItem(slot, null);

        return set;
    }

    public String serializeAsBase64() {
        List<ItemStack> orderedList = new ArrayList<>();

        for (EquipmentSetSlot slot : EquipmentSetSlot.values()) {
            ItemStack item = items.get(slot);
            if (item == null)
                item = new ItemStack(Material.AIR);
            orderedList.add(item);
        }

        return InventorySerializer.itemStackArrayToBase64(orderedList.toArray(new ItemStack[0]));
    }

    public void loadItemsFromBase64(String data) {
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFF_HAND };
        try {
            ItemStack[] items = InventorySerializer.itemStackArrayFromBase64(data);
            for (int i = 0; i < items.length - 1; i++) {
                if (i < slots.length - 1)
                    setItem(EquipmentSetSlot.valueOf(slots[i].name()), items[i]);
                else
                    addOtherItem(items[i]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setItem(int slot, ItemStack item) {
        items.put(EquipmentSetSlot.values()[slot], item);
    }

    private void setItem(EquipmentSetSlot slot, ItemStack item) {
        items.put(slot, item);
    }

    public void setItem(EquipmentSlot slot, ItemStack item) {
        EquipmentSetSlot setSlot = EquipmentSetSlot.valueOf(slot.name());
        items.put(setSlot, item);
    }

    public Map<EquipmentSlot, ItemStack> getArmor() {
        Map<EquipmentSlot, ItemStack> armor = new HashMap<>();

        for (Map.Entry<EquipmentSetSlot, ItemStack> e : items.entrySet()) {
            if (e.getKey().isArmor() && e.getValue() != null)
                armor.put(EquipmentSlot.valueOf(e.getKey().name()), e.getValue());
        }
        return armor;
    }

    public List<ItemStack> getOtherItems() {
        List<ItemStack> otherItems = new ArrayList<>();
        for (EquipmentSetSlot slot : EquipmentSetSlot.values()) {
            if (!slot.isArmor() && items.get(slot) != null)
                otherItems.add(items.get(slot));
        }
        return otherItems;
    }

    public List<ItemStack> getAllItems() {
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack item : items.values()) {
            if (item != null)
                all.add(item);
        }
        return all;
    }

    public boolean addOtherItem(@NotNull ItemStack item) {
        boolean placed = false;
        for (EquipmentSetSlot slot : EquipmentSetSlot.values()) {
            if (!slot.isArmor() && items.get(slot) == null) {
                items.put(slot, item);
                Bukkit.getLogger().info("Added " + item.getI18NDisplayName());
                placed = true;
            }
        }
        return placed;
    }

    public void removeArmor(@NotNull EquipmentSlot slot) {
        items.remove(EquipmentSetSlot.valueOf(slot.name()));
    }

    public boolean removeItem(@NotNull ItemStack item) {
        boolean removed = false;
        return items.values().removeIf(i -> i.equals(item));
    }

    public boolean equip(Player p) {
        for (Map.Entry<EquipmentSetSlot, ItemStack> e : items.entrySet()) {
            if (e.getKey().isArmor()) {
                ItemStack current = p.getInventory().getItem(EquipmentSlot.valueOf(e.getKey().name()));
                if (current != null) {
                    if (!p.getInventory().addItem(current).isEmpty()) {
                        p.sendMessage(Tools.componentalize("This kit cannot be equipped because you do not have enough empty inventory slots to hold your currently equipped armor."));
                        unequip(p);
                        return false;
                    }
                }
                p.getInventory().setItem(EquipmentSlot.valueOf(e.getKey().name()), Tools.tagAsPluginItem(tagItem(e.getValue())));
            }
            else {
                if (p.getInventory().firstEmpty() > 8) {
                    p.sendMessage(Tools.componentalize("This kit cannot be equipped because you do not have enough empty hotbar slots."));
                    unequip(p);
                    return false;
                }
            }
        }
        for (ItemStack i : getOtherItems()) {
            p.getInventory().setItem(Tools.getLastEmptyHotbarSlot(p), Tools.tagAsPluginItem(tagItem(i)));
        }
        return true;
    }

    public void unequip(Player p) {
        for (int i = 0; i <= 8; i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (hasTag(item))
                p.getInventory().setItem(i, null);
        }
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

    public boolean isEmpty() {
        for (ItemStack item : items.values()) {
            if (item != null)
                return false;
        }
        return true;
    }
}
