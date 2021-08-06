package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.events.GameStartEvent;
import me.cheracc.battlegameshungerroyale.events.GameTickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

@SuppressWarnings("BukkitListenerImplemented")
public interface InvincibilityPhase {
    default void onGameStart() {
    }
    default void atEndOfInvincibilityPhase() {
    }
    @EventHandler
    default void atGameStart(GameStartEvent event) {
        if (this instanceof Game && event.getGame().equals(this)) {
            Game game = (Game) this;
            game.setPhase(Game.GamePhase.INVINCIBILITY);
            for (Player p : game.getActivePlayers()) {
                p.setInvulnerable(true);
            }
            onGameStart();
        }
    }
    @EventHandler
    default void endInvincibilityPhase(GameTickEvent event) {
        if (this instanceof Game && event.getGame().equals(this)) {
            Game game = (Game) this;

            if (game.getCurrentGameTime() >= game.getOptions().getInvincibilityTime()) {
                game.forEachActivePlayer(p -> p.setInvulnerable(false));
                game.startMainPhase();
                atEndOfInvincibilityPhase();
            }
        }
    }
}
