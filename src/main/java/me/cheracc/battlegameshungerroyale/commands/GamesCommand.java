package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.guis.SelectGameGui;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GamesCommand implements CommandExecutor {
    private final BghrApi api;

    public GamesCommand(BghrApi api) {
        this.api = api;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (api.getGameManager().isThisAGameWorld(p.getWorld())) {
                p.sendMessage(Trans.lateToComponent("You are already in a game. You must /quit before joining another"));
                return true;
            }

            new SelectGameGui(p, api);
        }
        return true;
    }
}
