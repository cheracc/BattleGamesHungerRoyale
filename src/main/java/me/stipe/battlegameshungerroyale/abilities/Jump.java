package me.stipe.battlegameshungerroyale.abilities;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Jump extends Ability implements ActiveAbility {
    private final Material itemType;
    private final String itemName;

    public Jump(int cooldown, Material itemType, String itemName) {
        super("Jump", "Makes the player jump high", cooldown);
        this.itemType = itemType;
        this.itemName = itemName;
    }

    public Jump() {
        super("Jump", "Makes the player jump high", 30);
        this.itemType = Material.FIREWORK_ROCKET;
        this.itemName = "Jump Rocket";
    }

    @Override
    public void doAbility(Player source, @Nullable Player target, double amplifier, int durationModifier) {
        Vector newTrajectory = source.getVelocity();
        double y = source.getLocation().getY();

        if (Math.floor(y) == y) {
            newTrajectory.add(new Vector(0,1 + amplifier,0));
            source.setVelocity(newTrajectory);
            new BukkitRunnable() {
                @Override
                public void run() {
                    source.setFallDistance(-500);
                }
            }.runTaskLater(BGHR.getPlugin(), 20L);
        }

    }

    @Override
    public ItemStack createAbilityItem() {
        ItemStack abilityItem = new ItemStack(itemType);
        ItemMeta meta = abilityItem.getItemMeta();
        List<Component> lore = new ArrayList<>();

        if (meta == null)
            return abilityItem;

        meta.displayName(Component.text(itemName));
        attachNewUuid(meta, UUID.randomUUID().toString());

        lore.add(Component.text(""));
        lore.add(Component.text("  Use this item while on the ground"));
        lore.add(Component.text("  to jump high into the air!"));
        lore.add(Component.text(""));
        lore.add(Component.text("Cooldown: " + Tools.secondsToMinutesAndSeconds(getCooldown())));

        meta.lore(lore);
        abilityItem.setItemMeta(meta);

        return abilityItem;
    }

    @Override
    public void load(ConfigurationSection section) {

    }
}
