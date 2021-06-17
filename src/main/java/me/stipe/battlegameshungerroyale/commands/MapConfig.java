package me.stipe.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.MapData;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MapConfig implements CommandExecutor {
    Map<UUID, MapData> configurators = new HashMap<>();
    Map<MapData, Gui> activeGuis = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player))
            return true;

        Player p = (Player) commandSender;
        MapManager mapManager = BGHR.getMapManager();
        MapData data = mapManager.getPlayersCurrentMap(p);

        if (configurators.containsKey(p.getUniqueId()))
            data = configurators.get(p.getUniqueId());

        if (args.length != 0) {
            String mapName = args[0];

            for (MapData m : mapManager.getMaps()) {
                if (m.getMapDirectory().getName().equalsIgnoreCase(mapName))
                    data = m;
                if (m.getMapName().equalsIgnoreCase(mapName))
                    data = m;
            }
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("name"))
            changeName(p, data, args);
        else if (args.length >= 2 && args[0].equalsIgnoreCase("creator"))
            changeCreator(p, data, args);
        else if (args.length >= 2 && args[0].equalsIgnoreCase("desc"))
            changeDescription(p, data, args);
        else if (args.length >= 2 && args[0].equalsIgnoreCase("icon"))
            changeIcon(p, data, args);

        configurators.put(p.getUniqueId(), data);
        getGui(p, data).open(p);
        return true;
    }

    private Gui getGui(Player player, MapData mapData) {
        if (activeGuis.containsKey(mapData))
            return activeGuis.get(mapData);

        Gui gui = Gui.gui().title(Component.text("mapconfig.yml")).rows(1).create();

        gui.setDefaultClickAction(e -> e.setCancelled(true));
        gui.addItem(nameItem(mapData));
        gui.addItem(creatorItem(mapData));
        gui.addItem(descriptionItem(mapData));
        gui.addItem(iconItem(mapData));
        gui.addItem(borderItem(player, mapData));
        gui.addItem(borderSizeItem(mapData));
        gui.setItem(8, ItemBuilder.from(Material.STRUCTURE_VOID).name(Component.text(ChatColor.WHITE + "Close"))
                .asGuiItem(e -> e.getWhoClicked().closeInventory()));

        activeGuis.put(mapData, gui);
        return gui;
    }

    private GuiItem nameItem(MapData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to change the name of this map");

        return ItemBuilder.from(Material.ITEM_FRAME).name(Tools.componentalize("&eMap Name: &f" + data.getMapName()))
            .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                if (e.getWhoClicked() instanceof Player) {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    p.sendMessage(commandInstructions("/mapconfig name <name>"));
                }
            });
    }

    private void changeName(Player p, MapData data, String[] args) {
        if (!configurators.containsKey(p.getUniqueId())) {
            p.sendMessage(Tools.componentalize("You are not currently editing a map configuration"));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1)
                sb.append(" ");
            sb.append(args[i]);
        }
        data.setName(sb.toString());
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(p, "mapconfig");
            }
        }.runTaskLater(BGHR.getPlugin(), 10L);
    }

    private GuiItem descriptionItem(MapData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(Tools.wrapText(data.getMapDescription(), ChatColor.DARK_GREEN));
        lore.add("");
        lore.add("&bClick here to change the description for this map");

        return ItemBuilder.from(Material.BOOK).name(Tools.componentalize("&eMap Description: &f"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        p.sendMessage(commandInstructions("/mapconfig desc <description>"));
                    }
                });
    }

    private void changeDescription(Player p, MapData data, String[] args) {
        if (!configurators.containsKey(p.getUniqueId())) {
            p.sendMessage(Tools.componentalize("You are not currently editing a map configuration"));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1)
                sb.append(" ");
            sb.append(args[i]);
        }
        data.setDescription(sb.toString());
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(p, "mapconfig");
            }
        }.runTaskLater(BGHR.getPlugin(), 10L);
    }

    private GuiItem creatorItem(MapData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to change the name of the creator");
        lore.add("&bof this map");

        return ItemBuilder.from(Material.TOTEM_OF_UNDYING).name(Tools.componentalize("&fCreator: &7" + data.getMapCreator()))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        p.sendMessage(commandInstructions("/mapconfig creator <name>"));
                    }
                });
    }

    private void changeCreator(Player p, MapData data, String[] args) {
        if (!configurators.containsKey(p.getUniqueId())) {
            p.sendMessage(Tools.componentalize("You are not currently editing a map configuration"));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1)
                sb.append(" ");
            sb.append(args[i]);
        }
        data.setCreator(sb.toString());
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(p, "mapconfig");
            }
        }.runTaskLater(BGHR.getPlugin(), 10L);
    }

    private GuiItem iconItem(MapData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to change this map's icon");

        return ItemBuilder.from(data.getIcon()).name(Tools.componentalize("&eMap Icon"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        // send another gui to get an icon
                        p.closeInventory();
                        pickIconGui().open(p);
                    }
                });
    }

    private void changeIcon(Player p, MapData data, String[] args) {
        if (!configurators.containsKey(p.getUniqueId())) {
            p.sendMessage(Tools.componentalize("You are not currently editing a map configuration"));
            return;
        }
        if (args[1] != null) {
            Material type = Material.valueOf(args[1].toUpperCase());
            data.setIcon(type);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.dispatchCommand(p, "mapconfig");
            }
        }.runTaskLater(BGHR.getPlugin(), 10L);
    }

    private GuiItem borderItem(Player player, MapData data) {
        List<String> lore = new ArrayList<>();
        Material icon = (data.isUseBorder() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        lore.add("");
        lore.add(String.format("&6Center: (&e%s&6, &e%s&6, &e%s&6)", data.getCenterX(), data.getCenterY(), data.getCenterZ()));
        lore.add("");
        lore.add("&bClick here to toggle the border on or off");

        if (BGHR.getMapManager().getPlayersCurrentMap(player).equals(data)) {
            lore.add("&bRight Click to indicate the current center");
            lore.add("&bShift+Click to center the border at your location");
        }

        return ItemBuilder.from(icon).name(Tools.componentalize("&eMap Border: " + (data.isUseBorder() ? "&aON" : "&cOFF")))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();

                        if (e.getClick() == ClickType.SHIFT_LEFT && BGHR.getMapManager().getPlayersCurrentMap(p).equals(data)) {
                            data.setCenter(p.getLocation());
                            if (data.isLoaded())
                                data.updateBorder();
                            getGui(p, data).updateItem(e.getSlot(), borderItem(p, data));
                        }
                        else if (e.getClick() == ClickType.RIGHT && BGHR.getMapManager().getPlayersCurrentMap(p).equals(data)) {
                            data.getWorld().strikeLightningEffect(new Location(data.getWorld(), data.getCenterX(), data.getCenterY(), data.getCenterZ()));
                        } else {
                            // change the setting and update the icon
                            toggleBorder(data);
                            if (data.isLoaded())
                                data.updateBorder();
                            getGui(p, data).updateItem(e.getSlot(), borderItem(p, data));
                        }
                    }
                });
    }

    private void toggleBorder(MapData data) {
        data.toggleUseBorder();
    }

    private GuiItem borderSizeItem(MapData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(" &7This is the distance from the");
        lore.add(" &7center of the map to an edge");
        lore.add(" &7of the square border");
        lore.add("");
        lore.add("&bLeft Click to &aincrease");
        lore.add("&bRight Click to &cdecrease");

        return ItemBuilder.from(Material.END_CRYSTAL).name(Tools.componentalize("&eBorder Radius: &f" + data.getBorderRadius()))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        int newRadius = data.getBorderRadius();

                        switch (e.getClick()) {
                            case LEFT:
                                newRadius += 10;
                                break;
                            case RIGHT:
                                newRadius -= 10;
                                break;
                            case SHIFT_LEFT:
                                newRadius += 100;
                                break;
                            case SHIFT_RIGHT:
                                newRadius -= 100;
                        }
                        if (newRadius < 0)
                            newRadius = 0;

                        data.setBorderRadius(newRadius);
                        if (data.isLoaded())
                            data.updateBorder();

                        int slot = e.getSlot();
                        getGui(p, data).updateItem(slot, borderSizeItem(data));
                    }
                });
    }

    private TextComponent commandInstructions(String commandString) {
        String[] words = commandString.split(" ");
        String command = words[0];
        String argument = words[1];
        String property = argument.equalsIgnoreCase("desc") ? "description" : argument;
        return Component.text("=====================================================").color(TextColor.color(255,0,0))
                .append(Component.newline())
                .append(Component.text("                                Type").color(TextColor.color(255,255,255)))
                .append(Component.newline())
                .append(Component.text("                    " + commandString).color(TextColor.color(250,250,0)))
                .append(Component.newline())
                .append(Component.text("                 to change the " + property + " of this map").color(TextColor.color(255,255,255)))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("    (you can click on this message to start the command").color(TextColor.color(150,150,150)))
                .append(Component.newline())
                .append(Component.text(" or just type '/mapconfig' to cancel and return to the menu)").color(TextColor.color(150,150,150)))
                .append(Component.newline())
                .append(Component.text("=====================================================").color(TextColor.color(255,0,0)))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, command + " " + argument + " "))
                .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click here to save some typing!")));
    }

    private ScrollingGui pickIconGui() {
        ScrollingGui gui = Gui.scrolling().title(Component.text("Choose a Map Icon")).rows(6).pageSize(45).create();

        gui.setDefaultClickAction(e -> e.setCancelled(true));
        gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Up")).asGuiItem(event -> gui.previous()));
        gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Down")).asGuiItem(event -> gui.next()));
        gui.setItem(6, 5, ItemBuilder.from(Material.BARRIER).name(Component.text("Cancel and Go Back")).asGuiItem(event -> {
            event.getWhoClicked().closeInventory();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(event.getWhoClicked(), "mapconfig");
                }
            }.runTaskLater(BGHR.getPlugin(), 5L);
        }));

        for (Material m : Material.values()) {
            if (m != null && !m.isAir() && m.isItem() && !m.name().contains("LEGACY")) {
                gui.addItem(ItemBuilder.from(m).name(Component.text(ChatColor.WHITE + m.name().toLowerCase())).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        if (e.getCurrentItem() != null) {
                            String material = e.getCurrentItem().getType().name().toLowerCase();
                            p.closeInventory();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Bukkit.dispatchCommand(p, "mapconfig icon " + material);
                                }
                            }.runTaskLater(BGHR.getPlugin(), 5L);
                        }
                    }
                }));
            }
        }

        return gui;
    }

}
