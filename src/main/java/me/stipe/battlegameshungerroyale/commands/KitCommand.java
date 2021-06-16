package me.stipe.battlegameshungerroyale.commands;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.datatypes.PlayerData;
import me.stipe.battlegameshungerroyale.managers.PlayerManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;

            if (args.length >= 1) {
                String kitName = args[0];
                Kit kit = BGHR.getPlugin().getKitManager().getKit(kitName);

                if (kit != null) {
                    PlayerManager playerManager = BGHR.getPlugin().getPlayerManager();
                    PlayerData data = playerManager.getPlayerData(p);

                    data.registerKit(kit, false);
                }
            }
        }
        return false;
    }
}
