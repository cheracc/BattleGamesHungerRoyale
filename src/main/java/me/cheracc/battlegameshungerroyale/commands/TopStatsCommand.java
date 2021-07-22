package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.guis.TopStatsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TopStatsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            TopStatsGui.getInstance().send(p);
        }
        return true;
    }
}
