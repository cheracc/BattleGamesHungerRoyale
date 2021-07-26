package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.guis.SelectKitGui;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KitMenu implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            PlayerData data = PlayerManager.getInstance().getPlayerData(p);

            if (GameManager.getInstance().isActivelyPlayingAGame(p) && !p.isOp()) {
                p.sendMessage(Trans.lateToComponent("You cannot change your kit while playing a game"));
                return true;
            }

            new SelectKitGui(p, null, kit -> data.registerKit(kit, false));
        }
        return true;
    }
}
