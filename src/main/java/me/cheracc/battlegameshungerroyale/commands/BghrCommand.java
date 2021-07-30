package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.guis.AdminGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BghrCommand implements CommandExecutor {
    private final BghrApi api;
    public BghrCommand(BghrApi api) {
        this.api = api;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player)
            new AdminGui((Player) sender, api);
        return true;
    }
}
