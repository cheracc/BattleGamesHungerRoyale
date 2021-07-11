package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.PassiveAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AddPotionEffect extends Ability implements PassiveAbility {
    PotionEffect potionEffect = new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false, false);
    boolean toggleable = false;
    String abilityItemType = "potion";
    String itemName = "Toggle Item";
    String itemDescription = "Use this to do what it does";

    public AddPotionEffect() {
        setDescription("Adds a configurable potion effect to the player. The effect can either be toggled by the player (with the provided item) or on all the time (no item needed)");
    }

    @Override
    public void activate(Player p) {
        p.addPotionEffect(potionEffect);
    }

    @Override
    public void deactivate(Player p) {
        p.removePotionEffect(potionEffect.getType());
    }

    @Override
    public boolean hasToggleItem() {
        return toggleable;
    }

    @Override
    public ItemStack makeToggleItem() {
        if (hasToggleItem())
            return makeItem(Material.valueOf(abilityItemType.toUpperCase()), itemName, itemDescription, -1);
        return null;
    }

    @Override
    public boolean isActive(Player player) {
        return player.hasPotionEffect(potionEffect.getType());
    }
}
