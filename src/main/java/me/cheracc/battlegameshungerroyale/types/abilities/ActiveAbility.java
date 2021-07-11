package me.cheracc.battlegameshungerroyale.types.abilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ActiveAbility {
    boolean doAbility(Player source);
    ItemStack createAbilityItem();
    int getCooldown();
}
