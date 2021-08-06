package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

public class QuitCommand implements CommandExecutor {
    private final GameManager gameManager;
    private final MapManager mapManager;

    public QuitCommand(GameManager gameManager, MapManager mapManager) {
        this.gameManager = gameManager;
        this.mapManager = mapManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            Game game = gameManager.getPlayersCurrentGame(p);

            if (game != null) {
                game.quit(p);
            }

            if (game == null && !p.getWorld().equals(mapManager.getLobbyWorld())) {
                World world = p.getWorld();
                p.teleport(mapManager.getLobbyWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                mapManager.unloadWorld(world);
                p.sendMessage(Trans.lateToComponent("Finished editing, world unloaded."));
            }
        }
        return true;
    }
}
