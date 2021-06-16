package me.stipe.battlegameshungerroyale.datatypes.abilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ActiveAbility {
    void doAbility(Player source, @Nullable Player target, double amplifier1, int amplifier2);
    ItemStack createAbilityItem();
}
