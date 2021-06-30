package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Jump extends Ability implements ActiveAbility {
    private String abilityItemType;
    private String itemName;
    private String itemDescription;
    private double amplifier;
    private int cooldown;

    public Jump() {
        this.abilityItemType = "firework_rocket";
        this.itemName = "Jump Rocket";
        this.amplifier = 0;
        this.cooldown = 0;
        this.itemDescription = "Use this to give you a little boost!";
        setDescription("This will make a player jump about 4 blocks into the air. The amplifier affects only the height of the jump");
    }

    @Override
    public boolean doAbility(Player source) {
        Vector newTrajectory = source.getVelocity();
        double y = source.getLocation().getY();

        newTrajectory.add(new Vector(0,1 + amplifier,0));
        source.setVelocity(newTrajectory);
        if (getSound() != null)
            getSound().play(source.getLocation());
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 120 || source.getLocation().getY() == Math.floor(source.getLocation().getY())) {
                    cancel();
                }
                source.setFallDistance(-100);
                count++;
            }
        }.runTaskTimer(BGHR.getPlugin(), 5L, 5L);
        return true;
    }

    @Override
    public ItemStack createAbilityItem() {
        Material type = Material.valueOf(abilityItemType.toUpperCase());

        return makeItem(type, itemName, itemDescription, cooldown);
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

}
