package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.datatypes.Game;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QuitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            p.setCooldown(Material.AIR, 2);

            Game game = GameManager.getInstance().getPlayersCurrentGame(p);

            if (game != null) {
                game.quit(p);
            }

            if (game == null && !p.getWorld().equals(MapManager.getInstance().getLobbyWorld())) {
                World world = p.getWorld();
                p.teleport(MapManager.getInstance().getLobbyWorld().getSpawnLocation());
                MapManager.getInstance().unloadWorld(world);
                p.sendMessage(Tools.componentalize("Finished editing, world unloaded."));
            }
        }
        return true;
    }
}
