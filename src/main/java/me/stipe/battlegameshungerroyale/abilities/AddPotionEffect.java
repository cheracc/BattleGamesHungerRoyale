package me.stipe.battlegameshungerroyale.abilities;

import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.PassiveAbility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AddPotionEffect extends Ability implements PassiveAbility {
    PotionEffectType effectType;
    int amplifier;
    boolean toggleable;
    boolean showParticles;
    boolean showEffectIcon;
    Material itemType;
    String itemName;
    String itemDescription;

    public AddPotionEffect() {
        super("AddPotionEffect", "Adds a permanent potion effect to the player, or optionally give the player an item to turn the effect on and off.");
    }

    @Override
    public void load(ConfigurationSection section) {
        if (section.contains("effect")) {
            effectType = PotionEffectType.getByName(section.getString("effect"));
            amplifier = section.getInt("amplifier", 0);
            showParticles = section.getBoolean("show particles", false);
            showEffectIcon = section.getBoolean("show effect icon", false);
            toggleable = section.getBoolean("toggleable", false);
            itemType = Material.valueOf(section.getString("item type", "potion").toUpperCase());
            itemName = "Effect Inducer";
            itemDescription = "Toggles the " + effectType.getName() + " effect on or off";
        } else {
            Bukkit.getLogger().warning("Tried to load AddPotionEffect ability with no effect specified");
        }
    }

    @Override
    public void activate(Player p) {
        PotionEffect effect = new PotionEffect(effectType, Integer.MAX_VALUE, amplifier, showParticles, showParticles, showEffectIcon);
        p.addPotionEffect(effect);
    }

    @Override
    public void deactivate(Player p) {
        p.removePotionEffect(effectType);
    }

    @Override
    public boolean hasToggleItem() {
        return toggleable;
    }

    @Override
    public ItemStack makeToggleItem() {
        if (hasToggleItem())
            return makeItem(itemType, itemName, itemDescription, -1);
        return null;
    }

    @Override
    public boolean isActive(Player player) {
        return player.hasPotionEffect(effectType);
    }
}
