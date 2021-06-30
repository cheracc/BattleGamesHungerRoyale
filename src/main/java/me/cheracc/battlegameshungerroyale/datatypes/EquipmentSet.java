package me.cheracc.battlegameshungerroyale.datatypes;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class EquipmentSet implements ConfigurationSerializable {
    private final UUID uuid = UUID.randomUUID();
    private final Map<EquipmentSlot, ItemStack> armor = new HashMap<>();
    private final List<ItemStack> otherItems = new ArrayList<>();
    private final static NamespacedKey key = new NamespacedKey(BGHR.getPlugin(), "equipment");

    public EquipmentSet() {

    }

    public EquipmentSet(Map<String, Object> inputMap) {

    }

    public HashMap<EquipmentSlot, ItemStack> getArmor() {
        return new HashMap<>(armor);
    }

    public List<ItemStack> getOtherItems() {
        return new ArrayList<>(otherItems);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> output = new HashMap<>();
        for (Map.Entry<EquipmentSlot, ItemStack> e : armor.entrySet()) {
            output.put("armor." + e.getKey().name().toLowerCase(), e.getValue());
        }

        int count = 1;
        for (ItemStack other : getOtherItems()) {
            output.put("other." + count, other);
            count++;
        }
        return output;
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
            p.getInventory().setItem(e.getKey(), tagItem(e.getValue()));
        }
        for (ItemStack i : getOtherItems()) {
            p.getInventory().setItem(Tools.getLastEmptyHotbarSlot(p), tagItem(i));
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
        String id = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        return (id != null && id.equalsIgnoreCase(uuid.toString()));
    }

    private ItemStack tagItem(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, uuid.toString());
        item.setItemMeta(meta);
        return item;
    }
}
