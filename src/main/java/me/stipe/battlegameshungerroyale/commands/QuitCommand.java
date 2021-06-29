package me.stipe.battlegameshungerroyale.commands;

import me.stipe.battlegameshungerroyale.datatypes.Game;
import me.stipe.battlegameshungerroyale.managers.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QuitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            Game game = GameManager.getInstance().getPlayersCurrentGame(p);

            if (game != null) {
                game.quit(p);
            }
        }
        return true;
    }
}
