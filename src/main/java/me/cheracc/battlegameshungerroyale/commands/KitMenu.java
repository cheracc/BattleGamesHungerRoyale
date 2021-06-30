package me.cheracc.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.Kit;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KitMenu implements CommandExecutor {
    KitManager kitManager = BGHR.getKitManager();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;

            getGui().open(p);
        }
        return true;
    }

    private BaseGui getGui() {
        int rows = kitManager.getLoadedKits().size() / 9 + 2;
        BaseGui gui;

        if (rows >= 6) {
            gui = Gui.scrolling().rows(6).title(Component.text("Select a kit:")).create();
            gui.setDefaultClickAction(e -> e.setCancelled(true));
            gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Up")).asGuiItem(event -> ((ScrollingGui) gui).previous()));
            gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Down")).asGuiItem(event -> ((ScrollingGui) gui).next()));
        } else {
            gui = Gui.gui().rows(rows).title(Component.text("Select a kit:")).create();
            gui.setDefaultClickAction(e -> e.setCancelled(true));
        }
        for (Kit kit : kitManager.getLoadedKits()) {
            gui.addItem(createKitIcon(kit));
        }
        gui.setItem(rows, 5, ItemBuilder.from(Material.ENCHANTED_GOLDEN_APPLE).name(Tools.componentalize("Make a New Kit")).asGuiItem(e -> {
            Kit kit = new Kit(e.getWhoClicked().getName() + "_" + ThreadLocalRandom.current().nextInt(999999999), new YamlConfiguration());
            Tools.saveObjectToPlayer("kitconfig", kit, (Player) e.getWhoClicked());
            e.getWhoClicked().closeInventory();
            Bukkit.dispatchCommand(e.getWhoClicked(), "kitconfig");
        }));
        return gui;
    }

    private GuiItem createKitIcon(Kit kit) {
        List<Component> lore = new ArrayList<>(Tools.componentalize(Tools.wrapText(kit.getDescription(), ChatColor.GRAY)));
        lore.add(0, Component.text(""));
        lore.add(Tools.BLANK_LINE);
        lore.add(Tools.componentalize("&eKit Abilities:"));
        for (Ability a : kit.getAbilities()) {
            String color = (a instanceof ActiveAbility) ? "&a" : "&6";
            String abilityString = String.format("%s%s &f- &7%s", color, a.getCustomName() != null ? a.getCustomName() : a.getName(),
                            a.getDescription());
            lore.addAll(Tools.componentalize(Tools.wrapText(abilityString, ChatColor.GRAY)));
            lore.add(Tools.BLANK_LINE);
        }

        return ItemBuilder.from(kit.getIcon()).name(Component.text(ChatColor.WHITE + kit.getName())).lore(lore)
            .asGuiItem(e -> Bukkit.dispatchCommand(e.getWhoClicked(), "kit " + kit.getName()));
    }
}
