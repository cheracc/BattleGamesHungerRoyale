package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.events.PlayerLootedChestEvent;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.AbilityTrigger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AddToChestLootTable extends TriggeredAbility {
    private final double chance;
    private final ItemStack item;

    public AddToChestLootTable() {
        this.chance = 0.5;
        this.item = new ItemStack(Material.NETHERITE_HOE);
        setDescription("Add an item to the loot table used when the player opens a loot chest");
    }

    public AbilityTrigger getTrigger() {
        return AbilityTrigger.OPEN_LOOT_CHEST;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        if (event instanceof PlayerLootedChestEvent) {
            LootGenerateEvent e = ((PlayerLootedChestEvent) event).getEvent();
            Random r = ThreadLocalRandom.current();

            if (r.nextFloat() < chance) {
                e.getLoot().add(Tools.makeItemPlaceable(item));
            }
        }
    }
}
