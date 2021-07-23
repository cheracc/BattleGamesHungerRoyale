package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.TotemAttackType;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AttackTotem extends Ability implements ActiveAbility {
    private int cooldown;
    private int duration;
    private int attackRadius;
    private int attackDamage;
    private int secondsBetweenAttacks;
    private boolean totemIsDestroyable;
    private String totemItemType;
    private String totemName;
    private String itemDescription;
    private TotemAttackType attackType;

    public AttackTotem() {
        this.cooldown = 30;
        this.duration = 10;
        this.attackRadius = 8;
        this.attackDamage = 1;
        this.secondsBetweenAttacks = 2;
        this.totemIsDestroyable = true;
        this.totemItemType = "target";
        this.totemName = "Attack Totem";
        this.itemDescription = "Use this to place your attack totem";
        this.attackType = TotemAttackType.ARROW;
        setDescription("Places a totem that attacks nearby enemies until it is destroyed or expires");
    }

    private LivingEntity createBaseTotem(Location loc) {
        while (!loc.getBlock().isSolid()) {
            loc.add(0, -1, 0);
        }
        loc.add(0,1,0);
        Zombie totem = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        ItemStack head = new ItemStack(Material.valueOf(totemItemType.toUpperCase()));

        totem.setBaby();
        totem.setInvisible(true);
        totem.setInvulnerable(true);
        totem.setCustomName(totemName);
        totem.setCustomNameVisible(true);
        totem.setAI(false);
        totem.setHealth(4);
        totem.setSilent(true);
        totem.getEquipment().setHelmet(head, false);

        return totem;
    }

    private void startTotemWatcher(LivingEntity totem) {
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (totem == null || totem.isDead() || !totem.isValid()) {
                    cancel();
                }
                Player owner = Bukkit.getPlayer((UUID) totem.getMetadata("owner").get(0).value());
                if (owner == null || !owner.isOnline()) {
                    destroyTotem(totem);
                    cancel();
                }

                List<Entity> targetsWithinRange = new ArrayList<>(totem.getNearbyEntities(attackRadius, attackRadius, attackRadius));
                targetsWithinRange.removeIf(entity -> !(entity instanceof LivingEntity));
                targetsWithinRange.remove(owner);
                boolean players = targetsWithinRange.stream().anyMatch(e -> (e instanceof Player));
                if (players)
                    targetsWithinRange.removeIf(entity -> !(entity instanceof Player));


                if (!targetsWithinRange.isEmpty() && (count % (2 * secondsBetweenAttacks)) == 0) {
                    Collections.shuffle(targetsWithinRange);
                    LivingEntity target = (LivingEntity) targetsWithinRange.get(0);
                    attack(totem, target, owner);
                    totem.getLocation().setDirection(target.getLocation().subtract(totem.getLocation()).toVector());
                }

                long expiry = totem.getMetadata("expires").get(0).asLong();
                if (expiry < System.currentTimeMillis()) {
                    destroyTotem(totem);
                    cancel();
                }
                count++;
            }
        }.runTaskTimer(BGHR.getPlugin(), 5L, 10L);
    }

    private void attack(LivingEntity totem, LivingEntity target, Player owner) {
        Projectile proj = attackType.getProjectile(totem, target.getEyeLocation());
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

    @Override
    public boolean doAbility(Player source) {
        BGHR plugin = BGHR.getPlugin();
        Location loc = source.getLocation();

        LivingEntity totem = createBaseTotem(loc);
        if (!totemIsDestroyable)
            totem.setInvulnerable(true);
        totem.setMetadata("attack", new FixedMetadataValue(plugin, null)); // TODO
        totem.setMetadata("expires", new FixedMetadataValue(plugin, System.currentTimeMillis() + (duration * 1000L)));
        if (!totemIsDestroyable)
            totem.setMetadata("invulnerable", new FixedMetadataValue(plugin, true));
        totem.setMetadata("owner", new FixedMetadataValue(plugin, source.getUniqueId()));

        startTotemWatcher(totem);
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
}
