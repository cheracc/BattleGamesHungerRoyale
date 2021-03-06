package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.guis.ConfigureMapGui;
import me.cheracc.battlegameshungerroyale.guis.SelectMapGui;
import me.cheracc.battlegameshungerroyale.listeners.TextInputListener;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.MapData;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MapConfig implements CommandExecutor {
    private final MapManager mapManager;
    private final TextInputListener textInputListener;

    public MapConfig(MapManager mapManager, TextInputListener textInputListener) {
        this.mapManager = mapManager;
        this.textInputListener = textInputListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            MapData map = null;

            if (args.length == 0) {
                map = mapManager.getMapFromWorld(p.getWorld());
            }
            else {
                if (args[0].equalsIgnoreCase("bordercenter")) {
                    map = mapManager.getMapFromWorld(p.getWorld());
                    map.setBorderCenter(p.getLocation());
                    p.sendMessage(Trans.lateToComponent("Border center set to your location"));
                }
                if (args[0].equalsIgnoreCase("spawncenter")) {
                    map = mapManager.getMapFromWorld(p.getWorld());
                    map.setSpawnCenter(p.getLocation());
                    p.sendMessage(Trans.lateToComponent("Spawn center set to your location"));
                }
                if (args[0].equalsIgnoreCase("spawn")) {
                    map = mapManager.getMapFromWorld(p.getWorld());
                    Block b = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
                    b.getType();
                    map.setSpawnBlockType(b.getType());
                    p.sendMessage(Trans.lateToComponent("Set spawn block to %s", b.getType().name().toLowerCase()));
                }
                if (args[0].equalsIgnoreCase("list")) {
                    new SelectMapGui(p, null, mapManager, mapData -> new ConfigureMapGui(p, null, mapData, mapManager, textInputListener));
                }
                else {
                    for (MapData m : mapManager.getMaps()) {
                        String combinedArgs = Tools.rebuildString(args, 0);
                        if (m.getMapName().equalsIgnoreCase(combinedArgs)) {
                            map = m;
                        }
                    }
                }
            }
            if (map != null)
                new ConfigureMapGui(p, null, map, mapManager, textInputListener);
        }
        return true;
    }
}
