package me.stipe.battlegameshungerroyale.abilities;

import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

public class Teleport extends Ability implements ActiveAbility {
    int cooldown;
    int maxDistance;
    boolean unsafeTeleporting;
    boolean leaveTracer;
    Material itemType;
    String itemName;
    String itemDescription;

    public Teleport() {
        super("Teleport", "Teleports the player in the direction they are looking");
        this.maxDistance = 30;
        this.cooldown = 30;
        this.unsafeTeleporting = true;
        this.leaveTracer = true;
        this.itemType = Material.SOUL_TORCH;
        this.itemName = "Teleport Rod";
        this.itemDescription = String.format("Use this item to teleport up to %s blocks away!", maxDistance);
    }

    @Override
    public void load(ConfigurationSection section) {
        this.cooldown = section.getInt("cooldown", 30);
        this.maxDistance = section.getInt("max distance", 30);
        this.unsafeTeleporting = section.getBoolean("allow unsafe teleporting", true);
        this.leaveTracer = section.getBoolean("leave tracer", true);
        this.itemType = Material.valueOf(section.getString("item type", "soul_torch").toUpperCase());
        this.itemName = section.getString("item name", "Teleport Rod");
        this.itemDescription = String.format("Use this item to teleport up to %s blocks away!", maxDistance);
    }

    @Override
    public boolean doAbility(Player source) {
        RayTraceResult result = source.rayTraceBlocks(maxDistance);
        Location teleportLocation = null;

        if (result != null && result.getHitBlock() != null) {
            teleportLocation = result.getHitBlock().getLocation();
        } else if (unsafeTeleporting) {
            teleportLocation = source.getLocation().add(source.getLocation().getDirection().normalize().multiply(maxDistance));
        }

        if (teleportLocation == null)
            return false;
        else
            source.teleport(teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);

        return true;
    }

    @Override
    public ItemStack createAbilityItem() {
        return makeItem(itemType, itemName, itemDescription, cooldown);
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }
}
