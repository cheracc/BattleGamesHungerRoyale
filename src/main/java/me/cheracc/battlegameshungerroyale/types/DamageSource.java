package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

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
                if (v.value() instanceof DamageSource)
                    return (DamageSource) v.value();
            }
        }
        return null;
    }
}
