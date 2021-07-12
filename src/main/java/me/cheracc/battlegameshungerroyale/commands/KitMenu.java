package me.cheracc.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.cheracc.battlegameshungerroyale.guis.ConfigureKitGui;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
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
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KitMenu implements CommandExecutor {
    KitManager kitManager = KitManager.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;

            getGui().open(p);
        }
        return true;
    }

    private BaseGui getGui() {
        int rows = kitManager.getLoadedKits().size() / 9 + 1;
        BaseGui gui;

        if (rows >= 5) {
            gui = Gui.scrolling().rows(6).title(Component.text("Select a kit:")).create();
            gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Up")).asGuiItem(event -> ((ScrollingGui) gui).previous()));
            gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Down")).asGuiItem(event -> ((ScrollingGui) gui).next()));
        } else {
            gui = Gui.gui().rows(rows).title(Component.text("Select a kit:")).create();
        }
        for (Kit kit : kitManager.getLoadedKits()) {
            gui.addItem(createKitIcon(kit));
        }
        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> e.getWhoClicked().closeInventory());
        return gui;
    }

    private GuiItem createKitIcon(Kit kit) {
        ItemBuilder icon = ItemBuilder.from(kit.getIcon()).name(Tools.componentalize(kit.getName()));
        icon.flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        List<Component> lore = new ArrayList<>(Tools.componentalize(Tools.wrapText(kit.getDescription(), ChatColor.GRAY)));
        lore.add(0, Component.text(""));
        lore.add(Tools.BLANK_LINE);
        if (!kit.getAbilities().isEmpty()) {
            lore.add(Tools.componentalize("&eKit Abilities:"));
            for (Ability a : kit.getAbilities()) {
                String color = (a instanceof ActiveAbility) ? "&a" : "&6";
                String abilityString = String.format("%s%s &f- &7%s", color, a.getCustomName() != null ? a.getCustomName() : a.getName(),
                        a.getDescription());
                lore.addAll(Tools.componentalize(Tools.wrapText(abilityString, ChatColor.GRAY)));
            }
        }
        lore.addAll(kit.getEquipment().getDescription());

        lore.add(Tools.BLANK_LINE);
        icon.lore(lore);
        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            Bukkit.dispatchCommand(e.getWhoClicked(), "kit " + kit.getName().toLowerCase());
        });

    }
}
