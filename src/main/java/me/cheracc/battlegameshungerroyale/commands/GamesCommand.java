package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.guis.SelectGameGui;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GamesCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (GameManager.getInstance().getPlayersCurrentGame(p) != null) {
                p.sendMessage(Tools.componentalize("You are already in a game. Type /quit to return to the lobby."));
                return true;
            }

            new SelectGameGui(p);
        }
        return true;
    }
}
