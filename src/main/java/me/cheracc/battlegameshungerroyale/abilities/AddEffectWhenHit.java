package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.AbilityTrigger;
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
    public AbilityTrigger getTrigger() {
        if (meleeOnly)
            return AbilityTrigger.TAKE_MELEE_HIT;
        if (projectileOnly)
            return AbilityTrigger.TAKE_PROJECTILE_HIT;
        return AbilityTrigger.TAKE_ANY_DAMAGE;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            player.addPotionEffect(effect);
        }
    }
}
