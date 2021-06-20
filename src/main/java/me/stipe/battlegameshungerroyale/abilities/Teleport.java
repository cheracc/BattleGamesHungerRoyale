package me.stipe.battlegameshungerroyale.abilities;

import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

public class Teleport extends Ability implements ActiveAbility {
    int cooldown;
    int maxDistance;
    boolean allowTeleportingIntoMidair;
    boolean leaveTracer;
    String abilityItemType;
    String itemName;
    String itemDescription;

    public Teleport() {
        this.maxDistance = 30;
        this.cooldown = 30;
        this.allowTeleportingIntoMidair = true;
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
            teleportLocation = result.getHitBlock().getLocation();
            if (!isSafeLocation(teleportLocation))
                teleportLocation = findNearbySafeLocation(teleportLocation, 3).add(0.5,0,0.5);
            if (teleportLocation == null)
                return false;
        } else if (allowTeleportingIntoMidair) {
            teleportLocation = source.getLocation().add(source.getLocation().getDirection().normalize().multiply(maxDistance));
        }

        if (teleportLocation == null)
            return false;
        else {
            if (leaveTracer) {
                int tracerDensity = 200;
                double distance = teleportLocation.distance(source.getLocation());
                for (int i = 0; i <= tracerDensity; i++) {
                    Location particleLocation = source.getLocation().add(source.getLocation().getDirection().normalize().multiply(distance/tracerDensity*i));
                    source.getWorld().spawnParticle(Particle.WHITE_ASH, particleLocation, 5, 0.1,0.1,0.1);
                }
            }
            source.teleport(teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
            source.setFallDistance(0);
        }

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

    private Location findNearbySafeLocation(Location location, int distanceToSearch) {

        for (int radius = 0; radius <= distanceToSearch; radius++)
            for (int x = -radius; x <= radius; x++)
                for (int y = -radius; y <= radius; y++)
                    for (int z = -radius; z <= radius; z++)
                        if (isSafeLocation(location.clone().add(x,y,z))) {
                            return location.add(x,y,z);
                        }
        return null;
    }

    /**
     * Checks if a location is safe (solid ground with 2 breathable blocks)
     *
     * @param location Location to check
     * @return True if location is safe
     */
    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        if (!feet.getType().isAir())
            return false;
        Block head = feet.getRelative(BlockFace.UP);
        if (!head.getType().isAir())
            return false;
        Block ground = feet.getRelative(BlockFace.DOWN);
        return ground.getType().isSolid();
    }
}
