package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.datatypes.MapData;
import me.cheracc.battlegameshungerroyale.guis.interfaces.GetMap;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SelectMapGui extends Gui {
    private final GetMap callback;

    public SelectMapGui(HumanEntity player, Gui sendingGui, GetMap callback) {
        super(1, "&0Select a map:", new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.callback = callback;
        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            if (sendingGui != null)
                sendingGui.open(player);
        });
        fillGui();
        open(player);
    }

    private void fillGui() {
        for (MapData map : MapManager.getInstance().getMaps())
            addItem(mapIcon(map));
    }

    private GuiItem mapIcon(MapData map) {
        ItemBuilder icon = ItemBuilder.from(map.getIcon()).name(Tools.componentalize("&e" + map.getMapName()));
        List<String> lore = new ArrayList<>();

        lore.add("&fCreator: &7" + map.getMapCreator());
        lore.addAll(Tools.wrapText("&fDescription: &2" + map.getMapDescription(), ChatColor.DARK_GREEN));
        lore.add("");
        lore.add("&fBorder: &7" + (map.isUseBorder() ? "yes" : "no"));
        if (map.isUseBorder())
            lore.add("&fBorder Radius: &7" + map.getBorderRadius());


        return icon.lore(Tools.componentalize(lore)).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            callback.heresYourMap(map);
        });
    }
}
