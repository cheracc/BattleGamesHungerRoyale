package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KitCommand implements CommandExecutor {
    private final BghrApi api;

    public KitCommand(BghrApi api) {
        this.api = api;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;

            if (args.length >= 1) {
                String kitName = Tools.rebuildString(args, 0);
                Kit kit = api.getKitManager().getKit(kitName);

                if (kit != null && (kit.isEnabled() || p.hasPermission("bghr.admin.kits.disabled"))) {
                    PlayerData data = api.getPlayerManager().getPlayerData(p);
                    api.logr().debug("%s requested kit %s (has %s)", p.getName(), kit.getName(), data.getKit() == null ? "none" : data.getKit().getName());

                    if (data.getKit() != null && data.getKit().equals(kit)) {
                        p.sendMessage(Trans.lateToComponent("You are already using kit %s", kit.getName()));
                        return true;
                    }

                    if (api.getGameManager().isActivelyPlayingAGame(p) && !p.isOp()) {
                        p.sendMessage(Trans.lateToComponent("You cannot change your kit while playing a game"));
                        return true;
                    }

                    data.registerKit(kit, false);
                    api.getPlayerManager().outfitPlayer(p, kit);
                    p.closeInventory();
                    return true;
                }
            }
            Bukkit.dispatchCommand(p, "kitmenu");
        }
        return true;
    }
}
