package me.stipe.battlegameshungerroyale.abilities;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class Jump extends Ability implements ActiveAbility {
    private Material itemType;
    private String itemName;
    private double amplifier;
    private int cooldown;

    public Jump() {
        this.itemType = Material.FIREWORK_ROCKET;
        this.itemName = "Jump Rocket";
        this.amplifier = 0;
        this.cooldown = 0;
        setDescription("This will make a player jump about 4 blocks into the air. The amplifier affects only the height of the jump");
    }

    @Override
    public boolean doAbility(Player source) {
        Vector newTrajectory = source.getVelocity();
        double y = source.getLocation().getY();

        newTrajectory.add(new Vector(0,1 + amplifier,0));
        source.setVelocity(newTrajectory);
        new BukkitRunnable() {
            @Override
            public void run() {
                source.setFallDistance(-50);
            }
        }.runTaskLater(BGHR.getPlugin(), 20L);
        return true;
    }

    @Override
    public ItemStack createAbilityItem() {
        ItemStack abilityItem = new ItemStack(itemType);
        ItemMeta meta = abilityItem.getItemMeta();
        List<Component> lore = new ArrayList<>();

        if (meta == null)
            return abilityItem;

        meta.displayName(Component.text(ChatColor.WHITE + itemName));
        attachNewUuid(meta, UUID.randomUUID().toString());

        lore.add(Component.text(""));
        lore.add(Component.text(ChatColor.RESET + "  Use this to give you a little boost!").color(Tools.GRAY));
        lore.add(Component.text(""));
        lore.add(Tools.componentalize("&7Cooldown: &f" + Tools.secondsToMinutesAndSeconds(getCooldown())));

        meta.lore(lore);
        abilityItem.setItemMeta(meta);

        return abilityItem;
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public void load(ConfigurationSection section) {
        this.itemType = Material.valueOf(section.getString("item type", "firework_rocket").toUpperCase());
        this.itemName = section.getString("item name", "Generic Rocket");
        this.cooldown = section.getInt("cooldown", 2);
        this.amplifier = section.getDouble("amplifier", 0);
    }
}
