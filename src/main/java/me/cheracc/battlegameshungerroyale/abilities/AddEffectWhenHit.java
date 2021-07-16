package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeWrapper;

import java.util.concurrent.ThreadLocalRandom;

public class AddEffectWhenHit extends TriggeredAbility {
    boolean meleeOnly;
    boolean projectileOnly;
    PotionEffect effect;
    double chance;

    public AddEffectWhenHit() {
        meleeOnly = false;
        projectileOnly = false;
        effect = PotionEffectTypeWrapper.ABSORPTION.createEffect(40, 2);
        chance = 0.2;
        setDescription("Adds a potion effect to the player when they are hit by something");
    }

    @Override
    public Trigger getTrigger() {
        if (meleeOnly)
            return Trigger.TAKE_MELEE_HIT;
        if (projectileOnly)
            return Trigger.TAKE_PROJECTILE_HIT;
        return Trigger.TAKE_ANY_DAMAGE;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            player.addPotionEffect(effect);
        }
    }
}
