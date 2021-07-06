package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.datatypes.Kit;
import me.cheracc.battlegameshungerroyale.datatypes.PlayerData;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
                String kitName = Tools.rebuildString(args, 0);
                Kit kit = KitManager.getInstance().getKit(kitName);

                if (kit != null) {
                    PlayerData data = PlayerManager.getInstance().getPlayerData(p);

                    if (data.getKit() != null && data.getKit().equals(kit)) {
                        p.sendMessage(Component.text("You are already using kit " + kit.getName()));
                        return true;
                    }

                    data.registerKit(kit, false);
                    p.closeInventory();
                    return true;
                }
            }
            Bukkit.dispatchCommand(p, "kitmenu");
        }
        return true;
    }
}
