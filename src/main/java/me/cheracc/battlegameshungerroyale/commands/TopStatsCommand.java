package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.BghrApi;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TopStatsCommand implements CommandExecutor {
    private final BghrApi api;

    public TopStatsCommand(BghrApi api) {
        this.api = api;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            api.getTopStatsGui().send(p);
        }
        return true;
    }
}
