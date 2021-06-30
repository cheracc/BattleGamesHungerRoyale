package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.cheracc.battlegameshungerroyale.guis.interfaces.GetMaterialInput;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

public class SelectMaterialGui extends ScrollingGui {
    private final Gui sendingGui;
    private final GetMaterialInput callback;

    public SelectMaterialGui(HumanEntity p, Gui sendingGui, GetMaterialInput callback) {
        super(6, 45, Tools.componentalize("Select a Material Type:"));
        this.sendingGui = sendingGui;
        this.callback = callback;
        disableAllInteractions();

        if (sendingGui == null)
            setOutsideClickAction(e -> e.getWhoClicked().closeInventory());
        else
            setOutsideClickAction(e -> sendingGui.open(e.getWhoClicked()));

        populate();
        open(p);
    }

    private void populate() {
        setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Up")).asGuiItem(event -> previous()));
        setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Down")).asGuiItem(event -> next()));
        setItem(6, 5, ItemBuilder.from(Material.BARRIER).name(Component.text("Cancel and Go Back")).asGuiItem(event -> {
            event.getWhoClicked().closeInventory();
            sendingGui.open(event.getWhoClicked());
        }));

        for (Material m : Material.values()) {
            if (m != null && !m.isAir() && m.isItem() && !m.name().contains("LEGACY")) {
                addItem(ItemBuilder.from(m).name(Component.text(ChatColor.WHITE + m.name().toLowerCase())).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        if (e.getCurrentItem() != null) {
                            p.closeInventory();
                            callback.materialCallback(m);
                        }
                    }
                }));
            }
        }
    }
}
