package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.DamageSource;
import me.cheracc.battlegameshungerroyale.types.abilities.Totem;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.TotemType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class EffectTotem extends Totem implements Listener {
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
    private TotemType totemType;

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
        this.totemType = TotemType.FIRE_TOTEM;
        setDescription("This will place a totem on the ground that provides a potion effect to nearby players. The range, cooldown, effects are all configurable.");
        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    @Override
    public ItemStack createAbilityItem() {
        return makeItem(Material.valueOf(totemItemType.toUpperCase()), totemName, itemDescription, cooldown);
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
        return totemIsDestroyable;
    }

    @Override
    public int getDuration() {
        return duration;
    }

    @Override
    public int getAttackSpeed() {
        return 1;
    }

    @Override
    public ItemStack getTotemHead() {
        return new ItemStack(Material.valueOf(totemItemType.toUpperCase()));
    }

    @Override
    public UnaryOperator<LivingEntity> additionalOptions() {
        BGHR plugin = BGHR.getPlugin();
        return totem -> {
            Player owner = Bukkit.getPlayer((UUID) totem.getMetadata("owner").get(0).value());
            if (owner == null)
                return totem;
            totem.setMetadata("effect", new FixedMetadataValue(plugin, effect));
            if (affectsOnlyPlacingPlayer)
                totem.setMetadata("affects", new FixedMetadataValue(plugin, owner.getUniqueId()));
            else if (placingPlayerIsImmune)
                totem.setMetadata("immune", new FixedMetadataValue(plugin, owner.getUniqueId()));
            return totem;
        };
    }

    @Override
    public void doTotemAbility(LivingEntity totem, Player owner) {
        visualizeRadius(totem);
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
