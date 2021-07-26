package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.guis.SelectGameGui;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Trans;
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

            if (!p.getWorld().equals(MapManager.getInstance().getLobbyWorld())) {
                p.sendMessage(Trans.lateToComponent("You must be in the main world to join a game. Type /quit to return to the main world."));
                return true;
            }

            new SelectGameGui(p);
        }
        return true;
    }
}
