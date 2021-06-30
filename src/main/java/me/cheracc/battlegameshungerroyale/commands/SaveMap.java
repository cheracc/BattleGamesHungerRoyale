package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.datatypes.MapData;
import me.cheracc.battlegameshungerroyale.tools.Tools;
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
            Player p = (Player) commandSender;
            World w = p.getWorld();
            MapData mapDataToSave = MapManager.getInstance().getMapFromWorld(w);

            if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
                if (checkConfirmation(p) && mapDataToSave != null) {
                    double startTime = System.currentTimeMillis();
                    w.save();
                    w.setAutoSave(false);

                    String mapName = mapDataToSave.getMapName();
                    MapManager.getInstance().saveMap(mapDataToSave, w);
                    double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;

                    p.sendMessage(Tools.componentalize(String.format("&fWorld and Config for &e%s &fhas been saved. That took %.3f seconds.", mapName, elapsedSeconds)));
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
