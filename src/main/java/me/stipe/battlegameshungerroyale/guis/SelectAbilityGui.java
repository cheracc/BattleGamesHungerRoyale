package me.stipe.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.guis.interfaces.GetAbilityResponse;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SelectAbilityGui extends Gui {

    public SelectAbilityGui(HumanEntity player, Gui sendingGui, GetAbilityResponse callback) {
        super(1, Tools.componentalize("&0Select an Ability:"));

        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            sendingGui.open(e.getWhoClicked());
        });

        for (Ability a : BGHR.getKitManager().getDefaultAbilities())
            addItem(abilityIcon(a.newWithDefaults(), callback));

        open(player);
    }
    private GuiItem abilityIcon(Ability ability, GetAbilityResponse callback) {
        Material icon = Material.BELL;
        Component name = Tools.componentalize(ChatColor.WHITE + ability.getName());
        List<Component> lore = new ArrayList<>();

        if (ability.getDescription() != null) {
            lore.add(Tools.BLANK_LINE);
            lore.addAll(Tools.componentalize(Tools.wrapText(ability.getDescription(), ChatColor.GRAY)));
        }
        lore.add(Tools.BLANK_LINE);
        lore.add(Tools.componentalize("&fAbility Type:" + ((ability.isActive()) ? " &aActive" : "") + (ability.isPassive() ? " &6Passive" : "")));
        lore.add(Tools.BLANK_LINE);
        lore.add(Tools.componentalize("&fConfigurable Options/Defaults:"));

        for (String s : ability.getConfig().getKeys(false)) {
            Object o = ability.getConfig().get(s);
            String value = o.toString();
            if (o instanceof Map) {
                PotionEffect e = new PotionEffect((Map<String, Object>) o);
                value = e.getType().getName().toLowerCase() + " " + Tools.integerToRomanNumeral(e.getAmplifier());
            }
            if (o instanceof Material)
                value = ((Material) o).name().toLowerCase();
            if (o instanceof PotionEffect) {
                PotionEffect e = (PotionEffect) o;
                value = e.getType().getName().toLowerCase() + " " + Tools.integerToRomanNumeral(e.getAmplifier());
            }

            lore.addAll(Tools.componentalize(Tools.wrapText(String.format("&6%s: &e%s", s, value), ChatColor.YELLOW)));
        }
        lore.add(Tools.BLANK_LINE);
        lore.add(Tools.componentalize("&bClick here to add this ability"));

        return ItemBuilder.from(icon).name(name).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES).asGuiItem(e -> {
            if (e.getWhoClicked() instanceof Player) {
                Player p = (Player) e.getWhoClicked();

                p.closeInventory();
                Ability newAbility = ability.newWithDefaults();
                callback.abilityCallback(newAbility);
            }
        });
    }
}
