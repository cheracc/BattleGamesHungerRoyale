package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.guis.SelectKitGui;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KitMenu implements CommandExecutor {
    private final BghrApi api;

    public KitMenu(BghrApi api) {
        this.api = api;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            PlayerData data = api.getPlayerManager().getPlayerData(p);

            if (api.getGameManager().isActivelyPlayingAGame(p) && !p.isOp()) {
                p.sendMessage(Trans.lateToComponent("You cannot change your kit while playing a game"));
                return true;
            }

            new SelectKitGui(p, null, api.getKitManager(), kit -> data.registerKit(kit, false));
        }
        return true;
    }
}
