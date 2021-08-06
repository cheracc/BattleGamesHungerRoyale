package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.abilities.Totem;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.RemoteAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.TotemType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

public class RemoteActivation extends Totem implements Listener {
    private final TotemType totemType;
    private final RemoteAbility remoteAbility;
    private final boolean isDestroyable;
    private final String totemItemType;
    private final String itemDescription;
    private final int cooldown;
    private final int explosionPower;

    public RemoteActivation() {
        totemType = TotemType.BASIC;
        remoteAbility = RemoteAbility.EXPLODE;
        isDestroyable = true;
        totemItemType = "tnt";
        itemDescription = "Use this to do what it does";
        cooldown = 30;
        explosionPower = 3;
        setDescription("Places a device on the ground when used. When used again, some action is executed at the device's location");
    }

    private void placeDevice(Player player) {
        Location loc = player.getLocation();

        LivingEntity device = getTotemType().createBaseTotem(loc, getTotemHead(), getCustomName());
        if (!isDestroyable()) {
            device.setMetadata("invulnerable", new FixedMetadataValue(plugin, true));
        }
        device.setMetadata("expires", new FixedMetadataValue(plugin, System.currentTimeMillis() + (getDuration() * 1000L)));
        device.setMetadata("owner", new FixedMetadataValue(plugin, player.getUniqueId()));

        startTotemWatcher(device);

        player.setMetadata("remote_device", new FixedMetadataValue(plugin, device));
        player.setCooldown(Material.valueOf(totemItemType.toUpperCase()), cooldown * 2);
    }

    private LivingEntity getDevice(Player player) {
        if (player.hasMetadata("remote_device")) {
            LivingEntity device = (LivingEntity) player.getMetadata("remote_device").get(0).value();

            if (device == null || device.isDead() || !device.isValid()) {
                player.removeMetadata("remote_device", plugin);
                return null;
            }
            return device;
        }
        return null;
    }

    private boolean isDeviceActive(Player player) {
        if (player.hasMetadata("remote_device")) {
            LivingEntity device = (LivingEntity) player.getMetadata("remote_device").get(0).value();

            if (device == null || device.isDead() || !device.isValid()) {
                player.removeMetadata("remote_device", plugin);
                return false;
            }
            return true;
        }
        return false;
    }

    private void activateDevice(Player player) {
        LivingEntity device = getDevice(player);
        if (device == null || player.hasCooldown(Material.valueOf(totemItemType.toUpperCase())))
            return;

        switch (remoteAbility) {
            case EXPLODE:
                device.getLocation().createExplosion(player, explosionPower, false, false);
                if (getSound() != null)
                    getSound().play(player.getLocation());
                break;
            case TELEPORT_SELF:
                Tools.uncheckedTeleport(player, device.getLocation().add(0, 1, 0));
                if (getSound() != null) {
                    getSound().play(player.getLocation());
                    getSound().play(device.getLocation());
                }
        }
        device.remove();
        player.removeMetadata("remote_device", plugin);
    }

    @Override
    public ItemStack createAbilityItem() {
        return makeItem(Material.valueOf(totemItemType.toUpperCase()), getCustomName(), itemDescription, cooldown);
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public TotemType getTotemType() {
        return totemType;
    }

    @Override
    public boolean isDestroyable() {
        return isDestroyable;
    }

    @Override
    public int getDuration() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getAttackSpeed() {
        return Integer.MAX_VALUE;
    }

    @Override
    public ItemStack getTotemHead() {
        return new ItemStack(Material.valueOf(totemItemType.toUpperCase()));
    }

    @Override
    public void doTotemAbility(LivingEntity totem, Player owner) {
    }

    @Override
    public boolean doAbility(Player source) {
        if (source.hasCooldown(Material.valueOf(totemItemType.toUpperCase())))
            return false;
        if (isDeviceActive(source)) {
            activateDevice(source);
            return true;
        } else {
            placeDevice(source);
            return false;
        }
    }
}
