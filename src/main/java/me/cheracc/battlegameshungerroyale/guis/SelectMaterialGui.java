package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.ScrollingGui;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

public class SelectMaterialGui extends ScrollingGui {
    private final Gui sendingGui;
    private final Consumer<Material> callback;

    public SelectMaterialGui(HumanEntity p, Gui sendingGui, Consumer<Material> callback) {
        super(6, 45, "Select a Material Type:", ScrollType.VERTICAL, new HashSet<>(Arrays.asList(InteractionModifier.values())));
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
                            callback.accept(m);
                        }
                    }
                }));
            }
        }
    }
}
