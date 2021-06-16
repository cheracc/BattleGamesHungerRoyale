package me.stipe.battlegameshungerroyale.listeners;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.managers.PlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class GeneralPlayerEventListener implements Listener {
    MapManager mapManager = BGHR.getPlugin().getMapManager();
    PlayerManager playerManager = BGHR.getPlugin().getPlayerManager();

    @EventHandler
    public void noBreakingBlocks(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (p.getWorld().equals(mapManager.getLobbyWorld()) && !mapManager.canBuildInLobby()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void noPlacingBlocks(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        if (p.getWorld().equals(mapManager.getLobbyWorld()) && !mapManager.canBuildInLobby()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void noPlacingBlocks(BlockCanBuildEvent event) {
        Player p = event.getPlayer();
        if (p == null) return;
        if (p.getWorld().equals(mapManager.getLobbyWorld()) && !mapManager.canBuildInLobby()) {
            event.setBuildable(false);
        }
    }


}
