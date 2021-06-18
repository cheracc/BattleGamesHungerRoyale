package me.stipe.battlegameshungerroyale.abilities;

import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.PassiveAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AddPotionEffect extends Ability implements PassiveAbility {
    PotionEffect effect;
    boolean toggleable = false;
    String abilityItemType = "potion";
    String itemName;
    String itemDescription;

    public AddPotionEffect() {
        setDescription("Adds a configurable potion effect to the player. The effect can either be toggled by the player (with the provided item) or on all the time (no item needed)");
        effect = new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, true, true, false);
        itemName = effect.getType().getName().toLowerCase() + " toggle switch";
        itemDescription = "Toggles the " + effect.getType().getName().toLowerCase() + " effect on or off";
    }

    @Override
    public void activate(Player p) {
        p.addPotionEffect(effect);
    }

    @Override
    public void deactivate(Player p) {
        p.removePotionEffect(effect.getType());
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
        return player.hasPotionEffect(effect.getType());
    }
}
