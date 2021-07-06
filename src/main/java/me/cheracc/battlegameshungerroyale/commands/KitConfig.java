package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.datatypes.Kit;
import me.cheracc.battlegameshungerroyale.guis.ConfigureKitGui;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class KitConfig implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;

            // TODO add checks etc
            if (args.length > 0) {
                Kit kit = KitManager.getInstance().getKit(Tools.rebuildString(args, 0));

                if (kit != null) {
                    new ConfigureKitGui(kit, null, p);
                    return true;
                }

                if (Tools.rebuildString(args, 0).toLowerCase().contains("new")) {
                    new ConfigureKitGui(new Kit(p.getName() + "_" + ThreadLocalRandom.current().nextInt(999999)), null, p);
                    return true;
                }
            }
            p.sendMessage(Tools.componentalize("That's not a valid kit. You can also type '/kitconfig new' to create a new kit."));
        }

        return true;
    }


}
