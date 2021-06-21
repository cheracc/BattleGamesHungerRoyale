package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.tools.Tools;
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
        for (ItemStack other : otherItems) {
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
        if (otherItems.size() < 3)
            otherItems.add(item);
    }

    public void removeItem(@NotNull ItemStack item) {
        otherItems.remove(item);
        armor.values().remove(item);
    }

    public void equip(Player p) {
        for (Map.Entry<EquipmentSlot, ItemStack> e : armor.entrySet()) {
            p.getInventory().setItem(e.getKey(), tagItem(e.getValue()));
        }
        for (ItemStack i : otherItems) {
            p.getInventory().setItem(Tools.getLastEmptyHotbarSlot(p), tagItem(i));
        }
    }

    public void unequip(Player p) {
        List<ItemStack> items = new ArrayList<>();
        items.addAll(Arrays.asList(p.getInventory().getArmorContents()));
        items.addAll(Arrays.asList(p.getInventory().getContents()));
        items.addAll(Arrays.asList(p.getInventory().getStorageContents()));
        items.addAll(Arrays.asList(p.getInventory().getExtraContents()));
        items.add(p.getItemOnCursor());

        items.removeIf(Objects::isNull);

        for (ItemStack item : items) {
            if (hasTag(item))
                p.getInventory().remove(item);
        }
    }

    private boolean hasTag(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String id = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        return (id != null && id.equalsIgnoreCase(uuid.toString()));
    }

    private ItemStack tagItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, uuid.toString());
        item.setItemMeta(meta);
        return item;
    }
}
