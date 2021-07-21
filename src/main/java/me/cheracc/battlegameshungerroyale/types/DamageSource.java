package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class DamageSource {
    private long timeApplied;
    private int duration;
    private UUID source;
    private EntityDamageEvent.DamageCause type;

    public DamageSource(Player source, EntityDamageEvent.DamageCause type, int duration) {
        this.source = source.getUniqueId();
        this.type = type;
        this.duration = duration;
        timeApplied = System.currentTimeMillis();
    }

    public long getTimeApplied() {
        return timeApplied;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isApplicable(Player source, EntityDamageEvent.DamageCause type) {
        if ((System.currentTimeMillis() - timeApplied)/1000 > duration)
            return false;
        if (type != null && this.type != type)
            return false;
        if (source == null)
            return true;
        if (!source.getUniqueId().equals(this.source))
            return false;
        return true;
    }

    public Player getSource() {
        return Bukkit.getPlayer(source);
    }

    public EntityDamageEvent.DamageCause getType() {
        return type;
    }

    public void apply(Player target) {
        target.setMetadata("DamageSource", new FixedMetadataValue(BGHR.getPlugin(), this));
    }

    public void remove(Player target) {
        target.removeMetadata("DamageSource", BGHR.getPlugin());
    }

    public static DamageSource getFrom(Player target) {
        if (target.hasMetadata("DamageSource")) {
            for (MetadataValue v : target.getMetadata("DamageSource")) {
                if (v.value() instanceof DamageSource) {
                    DamageSource ds = (DamageSource) v.value();
                    if (ds.isApplicable(null, null))
                        return (DamageSource) v.value();
                    target.removeMetadata("DamageSource", v.getOwningPlugin());
                }
            }
        }
        return null;
    }

    public static DamageSource fromPotionEffect(PotionEffect effect, Player source) {
            EntityDamageEvent.DamageCause cause;

            PotionEffectType type = effect.getType();
            if (PotionEffectType.LEVITATION.equals(type)) {
                cause = EntityDamageEvent.DamageCause.FALL;
            } else if (PotionEffectType.HARM.equals(type)) {
                cause = EntityDamageEvent.DamageCause.MAGIC;
            } else if (PotionEffectType.POISON.equals(type)) {
                cause = EntityDamageEvent.DamageCause.POISON;
            } else if (PotionEffectType.WITHER.equals(type)) {
                cause = EntityDamageEvent.DamageCause.WITHER;
            } else {
                cause = EntityDamageEvent.DamageCause.CUSTOM;
            }

            return new DamageSource(source, cause, effect.getDuration() + 20);

    }
}
