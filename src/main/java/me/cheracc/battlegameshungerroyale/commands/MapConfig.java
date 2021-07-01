package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.datatypes.MapData;
import me.cheracc.battlegameshungerroyale.guis.ConfigureMapGui;
import me.cheracc.battlegameshungerroyale.guis.SelectMapGui;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapConfig implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            MapData map = null;

            if (args.length == 0) {
                map = MapManager.getInstance().getMapFromWorld(p.getWorld());
            }
            else {
                if (args[0].equalsIgnoreCase("bordercenter")) {
                    map = MapManager.getInstance().getMapFromWorld(p.getWorld());
                    map.setBorderCenter(p.getLocation());
                    p.sendMessage(Tools.componentalize("Border center set to your location"));
                }
                if (args[0].equalsIgnoreCase("spawncenter")) {
                    map = MapManager.getInstance().getMapFromWorld(p.getWorld());
                    map.setSpawnCenter(p.getLocation());
                    p.sendMessage(Tools.componentalize("Spawn center set to your location"));
                }
                if (args[0].equalsIgnoreCase("spawn")) {
                    map = MapManager.getInstance().getMapFromWorld(p.getWorld());
                    Block b = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
                    if (b != null && b.getType() != null) {
                        map.setSpawnBlockType(b.getType());
                        p.sendMessage(Tools.componentalize("Set spawn block to " + b.getType().name().toLowerCase()));
                    }
                }
                if (args[0].equalsIgnoreCase("list")) {
                    new SelectMapGui(p, null, mapData -> new ConfigureMapGui(p, null, mapData));
                }
                else {
                    for (MapData m : MapManager.getInstance().getMaps()) {
                        String combinedArgs = Tools.rebuildString(args, 0);
                        if (m.getMapName().equalsIgnoreCase(combinedArgs)) {
                            map = m;
                        }
                    }
                }
            }
            if (map != null)
                new ConfigureMapGui(p, null, map);
        }
        return true;
    }
}
