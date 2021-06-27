package me.stipe.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.guis.interfaces.GetSound;
import me.stipe.battlegameshungerroyale.tools.Tools;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SelectSoundGui extends ScrollingGui {
    String category;
    Gui sendingGui;
    GetSound callback;

    public SelectSoundGui(HumanEntity player, Gui sendingGui, String filter, GetSound callback) {
        super(6, 45, Tools.componentalize("Select a Sound:"));
        this.category = filter;
        this.sendingGui = sendingGui;
        this.callback = callback;
        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            sendingGui.open(player);
        });

        fillGui();
        open(player);
    }

    private void fillGui() {
        setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Tools.componentalize("&bScroll Up")).asGuiItem(event -> previous()));
        setItem(6, 8, ItemBuilder.from(Material.PAPER).name(Tools.componentalize("&bScroll Down")).asGuiItem(event -> next()));
        setItem(6, 9, ItemBuilder.from(Material.BARRIER).name(Tools.componentalize("&bCancel and Go Back")).asGuiItem(event -> {
            event.getWhoClicked().closeInventory();
            sendingGui.open(event.getWhoClicked());
        }));

        setItem(6, 2, ItemBuilder.from(Material.OAK_SAPLING).name(Tools.componentalize("Show only AMBIENT sounds")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new SelectSoundGui(e.getWhoClicked(), sendingGui, "AMBIENT", callback);
        }));
        setItem(6, 3, ItemBuilder.from(Material.GRASS_BLOCK).name(Tools.componentalize("Show only BLOCK sounds")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new SelectSoundGui(e.getWhoClicked(), sendingGui, "BLOCK", callback);
        }));
        setItem(6, 4, ItemBuilder.from(Material.DRAGON_HEAD).name(Tools.componentalize("Show only ENTITY sounds")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new SelectSoundGui(e.getWhoClicked(), sendingGui, "ENTITY", callback);
        }));
        setItem(6, 5, ItemBuilder.from(Material.JUKEBOX).name(Tools.componentalize("Show ALL sounds")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new SelectSoundGui(e.getWhoClicked(), sendingGui, null, callback);
        }));

        for (Sound sound : Sound.values()) {
            Material icon = null;
            String[] parts = sound.name().split("_");

            if (category == null || sound.name().contains(category)) {
                if (parts.length > 1)
                    icon = matchMaterial(parts[1]);
                if (icon == null && parts.length > 2)
                    icon = matchMaterial(parts[2]);
                if (icon == null) {
                    switch (parts[0]) {
                        case "AMBIENT":
                            icon = Material.DARK_OAK_SAPLING;
                            break;
                        case "BLOCK":
                            icon = Material.GRASS_BLOCK;
                            break;
                        case "ENTITY":
                            icon = Material.DRAGON_HEAD;
                            break;
                        case "ITEM":
                            icon = Material.ANVIL;
                            break;
                        default:
                            icon = Material.NOTE_BLOCK;
                            break;
                    }
                }
                String name = sound.name().substring(parts[0].length() + 1).toLowerCase();
                ItemBuilder item = ItemBuilder.from(icon).name(Tools.componentalize(name))
                        .lore(Tools.BLANK_LINE, Tools.componentalize("&bClick to play"), Tools.componentalize("&bRight Click to select"));

                addItem(item.asGuiItem(e -> {
                    if (e.getClick().name().toLowerCase().contains("right")) {
                        e.getWhoClicked().closeInventory();
                        callback.soundCallback(sound);
                    } else {
                        Player p = (Player) e.getWhoClicked();
                        p.playSound(p.getLocation(), sound, 1F, 1F);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                p.stopSound(sound);
                            }
                        }.runTaskLater(BGHR.getPlugin(), 80);
                    }
                }));
            }
        }
    }

    private static Material matchMaterial(String word) {
        List<Material> materials = new ArrayList<>(Arrays.asList(Material.values()));
        materials.removeIf(mat -> mat.name().contains("LEGACY"));
        materials.removeIf(Material::isAir);
        materials.removeIf(mat -> !mat.isItem());
        Collections.shuffle(materials);
        for (Material m : materials) {
            if (m.name().toLowerCase().contains(word.toLowerCase()))
                return m;
        }
        return null;

    }

}
