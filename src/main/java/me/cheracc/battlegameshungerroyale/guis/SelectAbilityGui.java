package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.function.Consumer;

public class SelectAbilityGui extends Gui {

    public SelectAbilityGui(HumanEntity player, Gui sendingGui, Consumer<Ability> callback) {
        super(1, "Select an Ability:", new HashSet<>(Arrays.asList(InteractionModifier.values())));

        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            sendingGui.open(e.getWhoClicked());
        });

        for (Ability a : KitManager.getInstance().getDefaultAbilities())
            addItem(abilityIcon(a.newWithDefaults(), callback));

        open(player);
    }
    private GuiItem abilityIcon(Ability ability, Consumer<Ability> callback) {
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
            if (e.getWhoClicked() instanceof Player p) {

                p.closeInventory();
                Ability newAbility = ability.newWithDefaults();
                callback.accept(newAbility);
            }
        });
    }
}
