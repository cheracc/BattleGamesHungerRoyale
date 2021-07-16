package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AddToChestLootTable extends TriggeredAbility {
    private double chance;
    private ItemStack item;

    public AddToChestLootTable() {
        this.chance = 0.5;
        this.item = new ItemStack(Material.NETHERITE_HOE);
        setDescription("Add an item to the loot table used when the player opens a loot chest");
    }

    public Trigger getTrigger() {
        return Trigger.OPEN_LOOT_CHEST;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        if (event instanceof LootGenerateEvent) {
            LootGenerateEvent e = (LootGenerateEvent) event;
            Random r = ThreadLocalRandom.current();

            if (r.nextFloat() < chance) {
                e.getLoot().add(Tools.makeItemPlaceable(item));
            }
        }
    }
}
