package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.guis.AdminGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BghrCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player)
            new AdminGui((Player) sender);
        return true;
    }
}
