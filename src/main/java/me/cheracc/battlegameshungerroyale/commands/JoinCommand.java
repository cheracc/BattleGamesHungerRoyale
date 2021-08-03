package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class JoinCommand implements CommandExecutor {
    private final GameManager gameManager;

    public JoinCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (args.length > 0) {
                try {
                    int gameNumber = Integer.parseInt(args[0]);
                    if (gameManager.getActiveGames().size() >= gameNumber)
                        gameManager.getActiveGames().get(gameNumber - 1).join(p);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }

            if (gameManager.getActiveGames().size() == 1) {
                Game game = gameManager.getActiveGames().get(0);
                if (game.isOpenToPlayers())
                    game.join(p);
                else
                    game.joinAsSpectator(p);
            } else
                Bukkit.dispatchCommand(p, "games");
        }
        return true;
    }
}
