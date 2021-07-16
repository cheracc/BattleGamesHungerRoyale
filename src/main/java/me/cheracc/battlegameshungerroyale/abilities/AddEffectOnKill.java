package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
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
    public Trigger getTrigger() {
        if (playersOnly)
            return Trigger.KILL_PLAYER;
        if (monstersOnly)
            return Trigger.KILL_MONSTER;
        if (animalsOnly)
            return Trigger.KILL_ANIMAL;
        return Trigger.KILL_ANYTHING;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        if (ThreadLocalRandom.current().nextDouble() <= chance)
            player.addPotionEffect(effect);
    }
}
