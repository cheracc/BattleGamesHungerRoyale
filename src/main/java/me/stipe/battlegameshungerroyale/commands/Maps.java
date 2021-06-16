package me.stipe.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.MapData;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Maps implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player))
            return true;

        Player p = (Player) commandSender;
        MapManager mapManager = BGHR.getPlugin().getMapManager();
        int rows = mapManager.getMaps().size() / 9 + 1;

        Gui gui = Gui.gui().title(Component.text("All Maps")).rows(rows).create();

        for (MapData mapData : mapManager.getMaps()) {
            GuiItem item = ItemBuilder.from(mapData.getIcon())
                    .name(getName(mapData))
                    .lore(getLore(p, mapData))
                    .asGuiItem( e -> {
                        if (e.getClick() == ClickType.LEFT) {
                            if (mapData.isLoaded())
                                if (!p.getWorld().equals(mapData.getWorld()))
                                    p.teleport(mapData.getWorld().getSpawnLocation());
                                else
                                    p.sendMessage(Component.text("You are already on that map!"));
                            else {
                                mapManager.loadMap(mapData);
                                p.sendMessage(Component.text("Map " + mapData.getMapName() + " has been loaded"));
                                gui.close(p);
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Bukkit.dispatchCommand(p, "maps");
                                    }
                                }.runTaskLater(BGHR.getPlugin(), 20L);
                            }
                        } else if (e.getClick() == ClickType.RIGHT) {
                            if (mapData.isLoaded()) {
                                if (!mapData.isLobby()) {
                                    mapManager.unloadMap(mapData);
                                    p.sendMessage(Component.text("Map " + mapData.getMapName() + " has been unloaded"));
                                    gui.close(p);
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            Bukkit.dispatchCommand(p, "maps");
                                        }
                                    }.runTaskLater(BGHR.getPlugin(), 5L);
                                }
                                else
                                    p.sendMessage(Component.text("You cannot unload the lobby"));
                            }
                        } else if (e.getClick() == ClickType.SHIFT_LEFT) {
                            gui.close(p);
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Bukkit.dispatchCommand(p,"mapconfig " + mapData.getMapDirectory().getName());
                                }
                            }.runTaskLater(BGHR.getPlugin(), 5L);
                        }
                    });
            gui.setDefaultClickAction(e -> e.setCancelled(true));
            gui.addItem(item);
        }

        gui.open(p);

        return true;
    }

    private Component getName(MapData mapData) {
        String name = ChatColor.WHITE + "" + ChatColor.UNDERLINE + mapData.getMapName();

        if (mapData.isLobby())
            name = ChatColor.translateAlternateColorCodes('&', "&6[&eLobby&6]&r ") + ChatColor.WHITE + name;
        if (mapData.isLoaded())
            name += ChatColor.translateAlternateColorCodes('&', "&r &2(&aLoaded&2)");
        else
            name += ChatColor.translateAlternateColorCodes('&', "&r &4(&7Not Loaded&4)");

        return Component.text(name);
    }

    private List<Component> getLore(Player p, MapData mapData) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.WHITE + "Creator: " + ChatColor.GRAY + mapData.getMapCreator()));
        lore.add(Component.text(""));
        lore.addAll(Tools.toC(Tools.wrapText(mapData.getMapDescription(), ChatColor.DARK_PURPLE)));
        lore.add(Component.text(""));
        if (mapData.isLoaded()) {
            if (p.getWorld().equals(mapData.getWorld()))
                lore.add(Tools.toC("&5[&dYou are on this map&5]"));
            else
                lore.add(Tools.toC("&bLeft Click: &3Teleport to This Map"));
            if (!mapData.isLobby())
                lore.add(Tools.toC("&bRight Click: &4Unload this Map"));
        } else {
            lore.add(Tools.toC("&bLeft Click: &2Load this Map"));
        }
        lore.add(Tools.toC("&bShift+Click: &6Configure This Map"));

        return lore;
    }

}
