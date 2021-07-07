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
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

public class SelectPotionEffectTypeGui extends ScrollingGui {
    private final Gui sendingGui;
    private final Consumer<PotionEffectType> callback;
    private final boolean allowInstant;

    public SelectPotionEffectTypeGui(HumanEntity p, Gui sendingGui, Consumer<PotionEffectType> callback, boolean allowInstant) {
        super(6, 45, "Select a Potion Effect:", ScrollType.VERTICAL, new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.sendingGui = sendingGui;
        this.callback = callback;
        this.allowInstant = allowInstant;
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

        for (PotionEffectType type : PotionEffectType.values()) {
            if (type != null) {
                if (!allowInstant && type.isInstant())
                    continue;
                addItem(ItemBuilder.from(Material.POTION).name(Component.text(ChatColor.WHITE + type.getName().toLowerCase())).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        if (e.getCurrentItem() != null) {
                            p.closeInventory();
                            callback.accept(type);
                        }
                    }
                }));
            }
        }
    }
}
