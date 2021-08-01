package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.events.GameChangedPhaseEvent;

public interface BorderPhase {
    default void startBorderPhase(Game game) {
        game.currentPhase = Game.GamePhase.BORDER;
        new GameChangedPhaseEvent(game, "border").callEvent();
        game.getWorld().getWorldBorder().setSize(10, game.getOptions().getBorderTime());
    }
}
