package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.PassiveAbility;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlayerData {
    private final UUID uuid;
    PlayerStats stats;

    Game currentGame;
    Kit kit;
    Map<Ability, ItemStack> abilityItems = new HashMap<>();

    public PlayerData(Player p) {
        uuid = p.getUniqueId();
    }

    public List<ItemStack> getAbilityItems() {
        return new ArrayList<>(abilityItems.values());
    }

    public Ability getAbilityFromItem(ItemStack item) {
        UUID id = Tools.getUuidFromItem(item);
        if (id == null) {
            return null;
        }
        for (Map.Entry<Ability, ItemStack> e : abilityItems.entrySet()) {
            if (Tools.getUuidFromItem(e.getValue()) != null)
                if (Objects.equals(Tools.getUuidFromItem(e.getValue()), id))
                    return e.getKey();
        }
        return null;
    }

    public Kit getKit() {
        return kit;
    }

    public void registerKit(Kit kit, boolean clearInventory) {
        if (this.kit != null) {
            removeKit(this.kit);
            getPlayer().sendMessage(Component.text("Removed Kit " + this.kit.getName()));
        }
        if (clearInventory)
            getPlayer().getInventory().clear();
        this.kit = kit;
        kit.outfitPlayer(getPlayer(), this);
        getPlayer().sendMessage(Component.text("You have been given Kit " + kit.getName()));
    }

    public void registerAbilityItem(Ability ability, ItemStack item) {
        abilityItems.put(ability, item);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void removeKit(Kit kit) {
        for (Ability a : kit.getAbilities()) {
            ItemStack item = abilityItems.get(a);
            int slot = findHotbarSlot(kit, item);
            if (slot < 0) {
                Bukkit.getLogger().warning("cannot find a kit item"); //TODO
                return;
            }
            getPlayer().getInventory().setItem(slot, null);

            if (a instanceof PassiveAbility) {
                ((PassiveAbility) a).deactivate(getPlayer());
            }
        }
        if (kit.getEquipment() != null) {
            kit.getEquipment().unequip(getPlayer());
        }
    }

    private int findHotbarSlot(Kit kit, ItemStack item) {
        for (int i = 0; i < 9; i++) {
            ItemStack ithItem = getPlayer().getInventory().getItem(i);
            if (item != null && ithItem != null)
                if (Objects.equals(Tools.getUuidFromItem(item), Tools.getUuidFromItem(ithItem)))
                    return i;
        }
        return -1;
    }

    public boolean hasKit(Kit kit) {
        return this.kit != null && this.kit.equals(kit);
    }
}
