package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.events.GameChangedPhaseEvent;
import me.cheracc.battlegameshungerroyale.events.GameTickEvent;
import org.bukkit.event.EventHandler;

@SuppressWarnings("BukkitListenerImplemented")
public interface BorderPhase {
    default void startBorderPhase() {
        if (this instanceof Game) {
            Game game = (Game) this;
            game.setPhase(Game.GamePhase.BORDER);
            new GameChangedPhaseEvent(game, "border").callEvent();
            game.getWorld().getWorldBorder().setSize(10, game.getOptions().getBorderTime());
        }
    }
    @EventHandler
    default void watchForEndOfMainPhase(GameTickEvent event) {
        if (this instanceof Game && event.getGame() == this) {
            Game game = (Game) this;

            int length = game.getOptions().getMainPhaseTime();
            if (this instanceof InvincibilityPhase)
                length += game.getOptions().getInvincibilityTime();

            if (game.getCurrentGameTime() > length)
                startBorderPhase();
        }
    }
}
