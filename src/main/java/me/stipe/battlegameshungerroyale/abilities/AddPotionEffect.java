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
    PotionEffectType effectType = PotionEffectType.REGENERATION;
    int amplifier = 0;
    boolean toggleable = false;
    boolean showParticles = false;
    boolean showEffectIcon = false;
    Material itemType = Material.POTION;
    String itemName = effectType.getName().toLowerCase() + " switch";
    String itemDescription = "Toggles the " + effectType.getName().toLowerCase() + " effect on or off";

    public AddPotionEffect() {
        setDescription("Adds a configurable potion effect to the player. The effect can either be toggled by the player (with the provided item) or on all the time (no item needed)");
    }

    @Override
    public void load(ConfigurationSection section) {
        if (section.contains("effect")) {
            effectType = PotionEffectType.getByName(section.getString("effect type"));
            amplifier = section.getInt("amplifier");
            showParticles = section.getBoolean("show particles");
            showEffectIcon = section.getBoolean("show effect icon");
            toggleable = section.getBoolean("toggleable");
            itemType = Material.valueOf(section.getString("item type", "potion").toUpperCase());
            itemName = "Effect Inducer";
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
