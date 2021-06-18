package me.stipe.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.managers.KitManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AbilityConfig implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            KitManager kitManager = BGHR.getKitManager();

            Ability ability = kitManager.getGenericAbility(args[0]);

            sendGui(p, ability);

        }
        return true;
    }

    public void sendGui(Player p, Ability ability) {
        Gui gui = Gui.gui().title(Tools.componentalize(ChatColor.BLACK + ability.getName())).rows(2).disableAllInteractions().create();

        gui.addItem(nameDescGuiItem(ability));
        for (String s : ability.getConfig().getKeys(false)) {
            gui.addItem(genericOptionGuiItem(ability, s));
        }
        gui.open(p);
    }

    private GuiItem nameDescGuiItem(Ability ability) {
        String itemName = "&eAbility Name: &f" + ability.getName();
        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.addAll(Tools.wrapText(ability.getDescription(), ChatColor.GRAY));
        lore.add("");
        lore.add("&bClick to change the name of this ability");
        lore.add("&bRight click to change the description of this ability");

        return ItemBuilder.from(Material.ITEM_FRAME).name(Tools.componentalize(itemName)).lore(Tools.componentalize(lore)).asGuiItem();
    }

    private GuiItem genericOptionGuiItem(Ability ability, String configOption) {
        String itemName = "&eConfig Option: &f" + configOption;
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("");
        Object value = ability.getConfig().get(configOption);
        String valueString;
        Material icon = Material.REDSTONE_LAMP;

        if (value instanceof Boolean) {
            valueString = Boolean.toString((Boolean) value);
            lore.add("&bClick to toggle this value");
        } else if (value instanceof Integer) {
            valueString = Integer.toString((Integer) value);
            lore.add("&bClick to increase this value");
            lore.add("&bRight Click to decrease this value");
            icon = Material.REPEATER;
        } else if (value instanceof Double) {
            valueString = Double.toString((Double) value);
            lore.add("&bClick to increase this value");
            lore.add("&bRight Click to decrease this value");
            icon = Material.COMPARATOR;
        } else if (value instanceof String) {
            valueString = (String) value;
            lore.add("&bClick to enter a new value");
            icon = Material.PAPER;
        } else if (value instanceof Material) {
            valueString = ((Material) value).name().toLowerCase();
            lore.add("&bClick to select a new value");
            icon = ((Material) value);
        } else if (value instanceof PotionEffectType) {
            valueString = ((PotionEffectType) value).getName().toLowerCase();
            lore.add("&bClick to select a new value");
            icon = Material.POTION;
        }  else {
            valueString = "";
        }

        lore.add(1, "&6Current value: &e" + valueString);

        return ItemBuilder.from(icon).name(Tools.componentalize(itemName)).lore(Tools.componentalize(lore)).asGuiItem();
    }
}
