package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.datatypes.MapData;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ConfigureMapGui extends Gui {
    private final MapData map;
    private final Gui sendingGui;

    public ConfigureMapGui(HumanEntity player, Gui sendingGui, MapData map) {
        super(1, Tools.componentalize("&0Configure Map: " + map.getMapName()));
        this.map = map;
        this.sendingGui = sendingGui;

        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            if (sendingGui != null)
                sendingGui.open(player);
        });

        fillGui();
        open(player);
    }

    public void fillGui() {
        setItem(0, nameAndDescriptionIcon());
        setItem(1, centerIcon());
        setItem(2, borderIcon());
        setItem(8, saveQuitIcon());

    }

    public GuiItem nameAndDescriptionIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(Tools.componentalize("&e" + map.getMapName()));
        List<String> lore = new ArrayList<>();

        lore.add("&fCreator: &7" + map.getMapCreator());
        lore.addAll(Tools.wrapText("&fDescription: &2" + map.getMapDescription(), ChatColor.DARK_GREEN));
        lore.add("");
        lore.add("&bClick to change name");
        lore.add("&bRight click to change description");
        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            if (e.isLeftClick()) {
                Tools.formatInstructions("Type a new name for this map in the chat window: ", map.getMapName());
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), text -> {
                    map.setName(text);
                    e.getWhoClicked().closeInventory();
                    updateItem(0, nameAndDescriptionIcon());
                    open(e.getWhoClicked());
                });
            }
            if (e.isRightClick()) {
                Tools.formatInstructions("Type a new description for this map in the chat window. " +
                        "You can click this message to load the current description so that you may edit it.", map.getMapDescription());
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), text -> {
                    map.setDescription(text);
                    e.getWhoClicked().closeInventory();
                    updateItem(0, nameAndDescriptionIcon());
                    open(e.getWhoClicked());
                });
            }

        });
    }

    public GuiItem borderIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.GLASS).name(Tools.componentalize("&eBorder: &f" + (map.isUseBorder() ? "on" : "off")));
        List<String> lore = new ArrayList<>();
        if (map.isUseBorder())
            lore.add("&fBorder Radius: &7" + map.getBorderRadius());

        lore.add("");
        if (map.isUseBorder())
            lore.add("&bClick to turn border on");
        else
            lore.add("&bClick to increase border size");
        if (map.getBorderRadius() > 0) {
            lore.add("&bRight click to decrease");
            lore.add("&7(Set to zero to turn border off)");
        }
        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            int current = map.getBorderRadius();
            if (e.isLeftClick()) {
                if (!map.isUseBorder())
                    map.toggleUseBorder();
                current += 10;
            }
            if (e.isRightClick() && map.isUseBorder() && map.getBorderRadius() >= 0) {
                current -= 10;
                if (current <= 0) {
                    current = 0;
                    map.toggleUseBorder();
                }
            }
            map.setBorderRadius(current);
            updateItem(2, borderIcon());
        });
    }

    public GuiItem centerIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.COMPASS).name(Tools.componentalize("&eMap Center"));
        icon.lore(Tools.componentalize(Tools.wrapText("Click here to set the center at this location. You can also stand where you want the center to be and type /mapconfig center", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            map.setCenter(e.getWhoClicked().getLocation());
            e.getWhoClicked().sendMessage(Tools.componentalize("Center of world border to your location."));
        });
    }

    public GuiItem saveQuitIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("Save this Map Configuration"));
        icon.lore(Tools.componentalize("&bRight click to close without saving"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            if (e.isLeftClick()) {
                map.saveConfig();
            }
            if (sendingGui != null)
                sendingGui.open(e.getWhoClicked());
        });
    }

}
