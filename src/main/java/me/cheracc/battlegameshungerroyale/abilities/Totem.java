package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Totem extends Ability implements ActiveAbility {
    private int cooldown;
    private DyeColor color;
    private String itemType;
    private String totemName;
    private String itemName;
    private String itemDescription;
    private int duration;
    private PotionEffect effect;
    private boolean placingPlayerIsImmune;
    private boolean affectsOnlyPlacingPlayer;

    public Totem() {
        this.cooldown = 8;
        this.color = DyeColor.RED;
        this.itemType = "totem_of_undying";
        this.totemName = "Totem";
        this.itemName = "Totem";
        this.itemDescription = "This places your totem at your feet. It pulses its effect once per second.";
        this.duration = 8;
        this.effect = new PotionEffect(PotionEffectType.LEVITATION, 25, 0, false, false, true);
        this.placingPlayerIsImmune = false;
        this.affectsOnlyPlacingPlayer = false;
    }

    @Override
    public boolean doAbility(Player source) {
        setTotem(source);
        return true;
    }

    @Override
    public ItemStack createAbilityItem() {
        return makeItem(Material.valueOf(itemType.toUpperCase()), itemName, itemDescription, cooldown);
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    private void setTotem(Player player) {
        BGHR plugin = BGHR.getPlugin();
        Location loc = player.getLocation();
        while (!loc.getBlock().isSolid()) {
            loc.add(0, -1, 0);
        }
        ArmorStand totem = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        ItemStack banner = new ItemStack(Material.RED_BANNER);
        totem.setItem(EquipmentSlot.HEAD, banner);
        totem.setArms(false);
        totem.setCanMove(false);
        totem.setCustomName(totemName);
        totem.setCustomNameVisible(true);
        totem.setHealth(6);
        totem.setInvulnerable(false);
        totem.setMetadata("effect", new FixedMetadataValue(plugin, effect));
        totem.setMetadata("expires", new FixedMetadataValue(plugin, System.currentTimeMillis() + (duration * 1000L)));
        if (affectsOnlyPlacingPlayer)
            totem.setMetadata("affects", new FixedMetadataValue(plugin, player.getUniqueId()));
        else if (placingPlayerIsImmune)
            totem.setMetadata("immune", new FixedMetadataValue(plugin, player.getUniqueId()));

        totem.setDisabledSlots(EquipmentSlot.values());
        startTotemWatcher(totem);
    }

    private void destroyTotem(LivingEntity totem) {
        new BukkitRunnable() {
            @Override
            public void run() {
                totem.getWorld().playEffect(totem.getLocation(), Effect.LAVA_CONVERTS_BLOCK, null, 6);
                totem.remove();
            }
        }.runTask(BGHR.getPlugin());
    }

    private void startTotemWatcher(LivingEntity totem) {
        new BukkitRunnable() {
            boolean bit = false;
            @Override
            public void run() {
                if (totem == null || totem.isDead() || !totem.isValid()) {
                    cancel();
                }
                bit = !bit;
                if (bit) {
                    if (totem.hasMetadata("affects")) {
                        Player p = Bukkit.getPlayer((UUID) totem.getMetadata("affects").get(0).value());
                        if (p != null && p.getLocation().distance(totem.getLocation()) <= 6) {
                            p.addPotionEffect(effect);
                        }
                    }
                    else {
                        List<Entity> targets = new ArrayList<>(totem.getNearbyEntities(4, 4, 4));

                        if (totem.hasMetadata("immune"))
                            targets.removeIf(e -> e.getUniqueId().equals(totem.getMetadata("immune").get(0).value()));

                        targets.forEach(e -> {
                            if (e instanceof LivingEntity)
                                ((LivingEntity) e).addPotionEffect(effect);
                        });
                    }
                }
                long expiry = totem.getMetadata("expires").get(0).asLong();
                if (expiry < System.currentTimeMillis())
                    destroyTotem(totem);
            }
        }.runTaskTimer(BGHR.getPlugin(), 20L, 10L);
    }

    @EventHandler (ignoreCancelled = true)
    public void noDropsFromTotems(EntityDeathEvent event) {
        if (event.getEntity() instanceof ArmorStand)
            event.getDrops().clear();
    }
}
