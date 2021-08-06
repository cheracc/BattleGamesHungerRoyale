package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.Kit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerKitOutfitEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();
    Player player;
    Kit kit;

    public PlayerKitOutfitEvent(Player player, Kit kit) {
        this.player = player;
        this.kit = kit;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public Player getPlayer() {
        return player;
    }

    public Kit getKit() {
        return kit;
    }

    @Override
    public @NotNull
    HandlerList getHandlers() {
        return handlerList;
    }
}
