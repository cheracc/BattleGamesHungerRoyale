package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class AddToKillLootTable extends TriggeredAbility {
    boolean playersOnly;
    boolean monstersOnly;
    boolean animalsOnly;
    ItemStack item;
    double chance;

    public AddToKillLootTable() {
        playersOnly = false;
        monstersOnly = false;
        animalsOnly = false;
        item = new ItemStack(Material.SPECTRAL_ARROW);
        chance = 0.2;
        setDescription("Adds an item to the drops when a player makes a kill.");
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
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), item);
            }
        }

    }
}
