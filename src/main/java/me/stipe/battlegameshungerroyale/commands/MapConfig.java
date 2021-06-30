package me.stipe.battlegameshungerroyale.commands;

import me.stipe.battlegameshungerroyale.datatypes.MapData;
import me.stipe.battlegameshungerroyale.guis.ConfigureMapGui;
import me.stipe.battlegameshungerroyale.guis.SelectMapGui;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
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
                if (args[0].equalsIgnoreCase("center")) {
                    map = MapManager.getInstance().getMapFromWorld(p.getWorld());
                    map.setCenter(p.getLocation());
                    p.sendMessage(Tools.componentalize("Border center set to your location"));
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
