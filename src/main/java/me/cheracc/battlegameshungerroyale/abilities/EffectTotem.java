package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.DamageSource;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EffectTotem extends Ability implements ActiveAbility, Listener {
    private int cooldown;
    private int duration;
    private int radius;
    private boolean totemIsDestroyable;
    private boolean placingPlayerIsImmune;
    private boolean affectsOnlyPlacingPlayer;
    private String totemItemType;
    private String totemName;
    private String itemDescription;
    private PotionEffect effect;

    public EffectTotem() {
        this.cooldown = 15;
        this.totemItemType = "magma_block";
        this.totemName = "Totem";
        this.itemDescription = "This places your totem at your feet. It pulses its effect once per second.";
        this.duration = 15;
        this.radius = 8;
        this.effect = new PotionEffect(PotionEffectType.LEVITATION, 25, 0, false, false, true);
        this.totemIsDestroyable = true;
        this.placingPlayerIsImmune = false;
        this.affectsOnlyPlacingPlayer = false;
        setDescription("This will place a totem on the ground that provides a potion effect to nearby players. The range, cooldown, effects are all configurable.");
        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    @Override
    public boolean doAbility(Player source) {
        setTotem(source);
        return true;
    }

    @Override
    public ItemStack createAbilityItem() {
        return makeItem(Material.valueOf(totemItemType.toUpperCase()), totemName, itemDescription, cooldown);
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    private void setTotem(Player player) {
        BGHR plugin = BGHR.getPlugin();
        Location loc = player.getLocation();

        LivingEntity totem = createBaseTotem(loc);
        if (!totemIsDestroyable)
            totem.setInvulnerable(true);
        totem.setMetadata("effect", new FixedMetadataValue(plugin, effect));
        totem.setMetadata("expires", new FixedMetadataValue(plugin, System.currentTimeMillis() + (duration * 1000L)));
        if (affectsOnlyPlacingPlayer)
            totem.setMetadata("affects", new FixedMetadataValue(plugin, player.getUniqueId()));
        else if (placingPlayerIsImmune)
            totem.setMetadata("immune", new FixedMetadataValue(plugin, player.getUniqueId()));
        if (!totemIsDestroyable)
            totem.setMetadata("invulnerable", new FixedMetadataValue(plugin, true));
        totem.setMetadata("owner", new FixedMetadataValue(plugin, player.getUniqueId()));

        startTotemWatcher(totem);
    }

    private void destroyTotem(LivingEntity totem) {
        new BukkitRunnable() {
            @Override
            public void run() {
                totem.getWorld().playEffect(totem.getLocation(), Effect.LAVA_CONVERTS_BLOCK, null);
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
                        if (p != null && p.getLocation().distance(totem.getLocation()) <= radius) {
                            p.addPotionEffect(effect);
                        }
                    }
                    else {
                        List<Entity> targets = new ArrayList<>(totem.getNearbyEntities(radius, radius, radius));

                        if (totem.hasMetadata("immune"))
                            targets.removeIf(e -> e.getUniqueId().equals(totem.getMetadata("immune").get(0).value()));

                        targets.forEach(e -> {
                            if (e instanceof LivingEntity) {
                                ((LivingEntity) e).addPotionEffect(effect);
                                if (e instanceof Player) {
                                    Player source = Bukkit.getPlayer((UUID) totem.getMetadata("owner").get(0).value());
                                    if (source != null)
                                        DamageSource.fromPotionEffect(effect, source).apply((Player) e);
                                }
                            }
                        });
                    }
                }
                visualizeRadius(totem);
                long expiry = totem.getMetadata("expires").get(0).asLong();
                if (expiry < System.currentTimeMillis())
                    destroyTotem(totem);
            }
        }.runTaskTimer(BGHR.getPlugin(), 5L, 10L);
    }

    private LivingEntity createBaseTotem(Location loc) {
        while (!loc.getBlock().isSolid()) {
            loc.add(0, -1, 0);
        }
        loc.add(0,1,0);
        ArmorStand totem = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        ItemStack head = new ItemStack(Material.valueOf(totemItemType.toUpperCase()));

        totem.setSmall(true);
        totem.setInvisible(true);
        totem.setInvulnerable(true);
        totem.setFireTicks(duration*21);
        totem.setCustomName(totemName);
        totem.setCustomNameVisible(true);
        totem.setAI(false);
        totem.setHealth(4);
        totem.setSilent(true);
        totem.getEquipment().setHelmet(head, false);

        return totem;
    }

    @EventHandler (ignoreCancelled = true)
    public void noDropsFromTotems(EntityDeathEvent event) {
        if (event.getEntity() instanceof ArmorStand)
            event.getDrops().clear();
    }

    @EventHandler
    public void damageTotems(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        Entity e = p.getTargetEntity(2);

        if (e instanceof ArmorStand && !e.hasMetadata("invulnerable") && ((ArmorStand) e).getNoDamageTicks() <= 0) {
            if (e.hasMetadata("expires")) {
                ArmorStand totem = (ArmorStand) e;
                double damage = 1 + ThreadLocalRandom.current().nextDouble();
                totem.getWorld().playSound(totem.getLocation(), Sound.ENTITY_SHULKER_HURT_CLOSED, 1F, 1F);
                totem.getWorld().spawnParticle(Particle.CRIT, totem.getLocation(), 4);
                if (totem.getHealth() - damage <= 0)
                    destroyTotem(totem);
                else {
                    totem.setHealth(totem.getHealth() - damage);
                    totem.setNoDamageTicks(8);
                }
            }
        }
    }

    private void visualizeRadius(LivingEntity totem) {
        Location center = totem.getLocation();
        double angle = 2 * Math.PI / 100;

        for (int i = 0; i < 100; i++) {
            Location l = center.clone().add(radius * Math.cos(angle * i), 0, radius * Math.sin(angle * i));
            l = l.getWorld().getHighestBlockAt(l).getLocation().add(0,1,0);
            totem.getWorld().spawnParticle(Particle.BLOCK_DUST, l, 1, 0.2, 0.2, 0.2, Material.valueOf(totemItemType.toUpperCase()).createBlockData());
        }
    }

}
