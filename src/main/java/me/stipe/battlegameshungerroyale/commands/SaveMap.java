package me.stipe.battlegameshungerroyale.commands;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.datatypes.MapData;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class SaveMap implements CommandExecutor {
    java.util.Map<UUID, Long> confirmations = new HashMap<>();
    BukkitTask task = null;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            MapManager maps = BGHR.getPlugin().getMapManager();
            Player p = (Player) commandSender;
            World w = p.getWorld();
            Long currentTime = System.currentTimeMillis();
            MapData mapDataToSave = null;

            // TODO: make sure this map hasn't been used in a game...

            for (MapData mapData : maps.getMaps()) {
                if (mapData.isLoaded() && mapData.getWorld().equals(w)) {
                    mapDataToSave = mapData;
                }
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
                if (checkConfirmation(p) && mapDataToSave != null) {
                    String mapName = mapDataToSave.getMapName();
                    maps.saveMap(mapDataToSave);
                    p.sendMessage(Component.text("MapData " + mapName + " has been saved"));
                    return true;
                }
                else
                    p.sendMessage(Component.text("Your confirmation timer has expired. You will need to re-issue the /savemap command"));
                return true;
            } else {
                if (mapDataToSave != null) {
                    sendConfirmation(p);
                    if (task == null)
                        startCleanupTask();
                    return true;
                }
            }
        }
        return false;
    }

    private void sendConfirmation(Player p) {
        p.sendMessage(Component.text(ChatColor.YELLOW + "====================================================="));
        p.sendMessage(Component.text(ChatColor.RED + "You are about to save this map. This CANNOT BE UNDONE.  "));
        p.sendMessage(Component.text(ChatColor.RED + "Saving this map will OVERWRITE the current map."));
        p.sendMessage(Component.text(ChatColor.RED + "If you are certain you wish to save, type" + ChatColor.GOLD + " '/savemap confirm'"));
        p.sendMessage(Component.text(ChatColor.YELLOW + "====================================================="));

        confirmations.put(p.getUniqueId(), System.currentTimeMillis());
    }

    private boolean checkConfirmation(Player p) {
        UUID uuid = p.getUniqueId();
        Long currentTime = System.currentTimeMillis();

        if (confirmations.containsKey(uuid))
            return currentTime - confirmations.get(uuid) <= 15000;

        return false;
    }

    private void startCleanupTask() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                Long currentTime = System.currentTimeMillis();
                confirmations.entrySet().removeIf(e -> currentTime - e.getValue() >= 15000);
            }
        }.runTaskTimer(BGHR.getPlugin(), 20*60L, 20*60L);
    }
}
