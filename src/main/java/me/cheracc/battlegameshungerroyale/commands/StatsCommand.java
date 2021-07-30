package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.guis.StatsGui;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class StatsCommand implements CommandExecutor {
    private final BGHR plugin;
    private final PlayerManager playerManager;
    private final List<CompletableFuture<PlayerData>> futures;
    private final BukkitRunnable futuresProcessor;

    public StatsCommand(PlayerManager playerManager, BGHR plugin) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        futuresProcessor = futuresProcessor();
        futuresProcessor.runTaskTimer(plugin, 0L, 1L);
        futures = new ArrayList<>();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (args.length > 0) {
                // see if its a playername we know
                OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(args[0]);

                if (op == null) {
                    p.sendMessage(Trans.lateToComponent("Couldn't find player: %s", args[0]));
                    return true;
                }
                futures.add(playerManager.getPlayerDataAsync(op.getUniqueId()));
                if (futuresProcessor.isCancelled())
                    futuresProcessor.runTaskTimer(plugin, 0L, 1L);
                return true;
            }

            new StatsGui(p, playerManager.getPlayerData(p));
        }
        return true;
    }

    private BukkitRunnable futuresProcessor() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                List<CompletableFuture<PlayerData>> toRemove = new ArrayList<>();
                futures.forEach(future -> {
                    if (future.isDone()) {
                        try {
                            PlayerData data = future.get();
                            new StatsGui(data.getPlayer(), data);
                            toRemove.add(future);
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });
                futures.removeAll(toRemove);
                if (futures.isEmpty())
                    cancel();
            }
        };
    }
}
