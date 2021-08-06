package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.BGHR;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerLootedChestEvent extends Event implements Listener {
    private final static HandlerList handlerList = new HandlerList();
    private final Player player;
    private final LootGenerateEvent event;
    private final BGHR plugin;

    public PlayerLootedChestEvent(Player player, LootGenerateEvent event, BGHR plugin) {
        this.player = player;
        this.plugin = plugin;
        this.event = event;
    }

    // for initializing the listener
    public PlayerLootedChestEvent(BGHR plugin) {
        player = null;
        this.plugin = plugin;
        event = null;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public Player getPlayer() {
        return player;
    }

    public LootGenerateEvent getEvent() {
        return event;
    }

    @EventHandler
    public void watchForLootChests(LootGenerateEvent event) {
        if (player == null && this.event == null) {
            if (event.getEntity() instanceof Player && plugin.getApi().getGameManager().isActivelyPlayingAGame((Player) event.getEntity())) {
                new PlayerLootedChestEvent((Player) event.getEntity(), event, plugin).callEvent();
            }
        }
    }
}
