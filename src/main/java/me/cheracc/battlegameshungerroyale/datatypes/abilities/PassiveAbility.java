package me.cheracc.battlegameshungerroyale.datatypes.abilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PassiveAbility {
    void activate(Player p);
    void deactivate(Player p);
    boolean hasToggleItem();
    ItemStack makeToggleItem();
    boolean isActive(Player p);
}
