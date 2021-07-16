package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeWrapper;

import java.util.concurrent.ThreadLocalRandom;

public class AddEffectOnHit extends TriggeredAbility {
    boolean meleeOnly;
    boolean projectileOnly;
    PotionEffect effect;
    double chance;

    public AddEffectOnHit() {
        meleeOnly = false;
        projectileOnly = false;
        effect = PotionEffectTypeWrapper.GLOWING.createEffect(40, 0);
        chance = 0.2;
        setDescription("Adds an effect to a player's target.");
    }

    @Override
    public Trigger getTrigger() {
        if (meleeOnly)
            return Trigger.DEAL_MELEE_HIT;
        if (projectileOnly)
            return Trigger.DEAL_PROJECTILE_HIT;
        return Trigger.DEAL_ANY_DAMAGE;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;

            if (ThreadLocalRandom.current().nextDouble() <= chance) {
                if (e.getEntity() instanceof LivingEntity)
                    ((LivingEntity) e.getEntity()).addPotionEffect(effect);
            }
        }
    }
}
