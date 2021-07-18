package me.cheracc.battlegameshungerroyale.events;

import me.cheracc.battlegameshungerroyale.types.Game;
import org.bukkit.entity.Player;

import java.util.Set;

public interface GameEvent {
    Game getGame();

    default Set<Player> getPlayers() {
        return getGame().getActivePlayers();
    }
}
