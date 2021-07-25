package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.AbilityTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeWrapper;

import java.util.concurrent.ThreadLocalRandom;

public class AddEffectOnKill extends TriggeredAbility {
    boolean playersOnly;
    boolean monstersOnly;
    boolean animalsOnly;
    PotionEffect effect;
    double chance;

    public AddEffectOnKill() {
        playersOnly = false;
        monstersOnly = false;
        animalsOnly = false;
        effect = PotionEffectTypeWrapper.GLOWING.createEffect(40, 0);
        chance = 0.2;
        setDescription("adds an effect to the player when they kill something");
    }

    @Override
    public AbilityTrigger getTrigger() {
        if (playersOnly)
            return AbilityTrigger.KILL_PLAYER;
        if (monstersOnly)
            return AbilityTrigger.KILL_MONSTER;
        if (animalsOnly)
            return AbilityTrigger.KILL_ANIMAL;
        return AbilityTrigger.KILL_ANYTHING;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        if (ThreadLocalRandom.current().nextDouble() <= chance)
            player.addPotionEffect(effect);
    }
}
