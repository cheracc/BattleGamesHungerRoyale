package me.stipe.battlegameshungerroyale.abilities;

import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

public class Teleport extends Ability implements ActiveAbility {
    int cooldown;
    int maxDistance;
    boolean allowUnsafeTeleporting;
    boolean leaveTracer;
    String abilityItemType;
    String itemName;
    String itemDescription;

    public Teleport() {
        this.maxDistance = 30;
        this.cooldown = 30;
        this.allowUnsafeTeleporting = true;
        this.leaveTracer = true;
        this.abilityItemType = "soul_torch";
        this.itemName = "Teleport Rod";
        this.itemDescription = String.format("Use this item to teleport up to %s blocks away!", maxDistance);
        setDescription("This will teleport a player instantly in the direction they are facing. Either to the block they are looking at (if it is in range). Or the max distance in that direction (if unsafe teleporting is enabled - this can end badly)");
    }

    @Override
    public boolean doAbility(Player source) {
        RayTraceResult result = source.rayTraceBlocks(maxDistance);
        Location teleportLocation = null;

        if (result != null && result.getHitBlock() != null) {
            teleportLocation = result.getHitBlock().getLocation().add(0.5,1,0.5);
        } else if (allowUnsafeTeleporting) {
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
        return makeItem(Material.valueOf(abilityItemType.toUpperCase()), itemName, itemDescription, cooldown);
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }
}
