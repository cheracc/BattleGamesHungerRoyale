package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.guis.StatsGui;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (args.length > 0) {
                // see if its a playername we know
                OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[0]);

                if (op == null) {
                    p.sendMessage(Tools.componentalize("Couldn't find player: " + args[0]));
                    return true;
                }
                PlayerManager.getInstance().getPlayerDataCallbackIfAsync(op.getUniqueId(), data -> new StatsGui(p, data));
                return true;
            }

            new StatsGui(p, PlayerManager.getInstance().getPlayerData(p));
        }
        return true;
    }
}
