package me.stipe.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.managers.KitManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AbilityList implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            sendGui(p);
        }
        return true;
    }

    private void sendGui(Player p) {
        KitManager kitManager = BGHR.getKitManager();
        Gui gui = Gui.gui().rows(1).title(Component.text("All Generic Abilities")).disableAllInteractions().create();

        for (Ability ability : kitManager.getDefaultAbilities()) {
            gui.addItem(createAbilityIcon(ability));
        }

        gui.open(p);
    }

    private GuiItem createAbilityIcon(Ability ability) {
        Material icon = Material.BELL;
        Component name = Tools.componentalize(ChatColor.WHITE + ability.getName());
        List<Component> lore = new ArrayList<>();
        String optionsLine = "&6%s&6: &e%s";

        lore.add(Tools.BLANK_LINE);
        lore.addAll(Tools.componentalize(Tools.wrapText(ability.getDescription(), ChatColor.GRAY)));
        lore.add(Tools.BLANK_LINE);
        lore.add(Tools.componentalize("&fAbility Type:" + ((ability.isActive()) ? " &aActive" : "") + (ability.isPassive() ? " &6Passive" : "")));
        lore.add(Tools.BLANK_LINE);
        lore.add(Tools.componentalize("&fConfigurable Options/Defaults:"));

        for (String s : ability.getDefaultConfig().getKeys(false)) {
            Object o = ability.getDefaultConfig().get(s);
            String value = o.toString();
            if (o.getClass().isEnum()) {
                value = ((Enum<?>) o).name().toLowerCase();
            }
            if (o instanceof PotionEffectType)
                value = ((PotionEffectType) o).getName().toLowerCase();

            if (String.format(optionsLine, s, value).length() > 40)
                lore.addAll(Tools.componentalize(Tools.wrapText(String.format(optionsLine, s, value), ChatColor.YELLOW)));
            else
                lore.add(Tools.componentalize(String.format(optionsLine, s, value)));
        }

        return ItemBuilder.from(icon).name(name).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES).asGuiItem();
    }
}
