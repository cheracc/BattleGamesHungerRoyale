package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SelectKitGui extends ScrollingGui {
    Consumer<Kit> callback;

    public SelectKitGui(HumanEntity player, Gui sendingGui, Consumer<Kit> callback) {
        super(Math.min(6, KitManager.getInstance().getLoadedKits().size() / 9 + 1), Math.max(45, ((KitManager.getInstance().getLoadedKits().size() / 9) * 9)),
                "Select a Kit", ScrollType.VERTICAL, InteractionModifier.VALUES);

        this.callback = callback;
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            if (sendingGui != null)
                sendingGui.open(player);
        });

        for (Kit kit : KitManager.getInstance().getLoadedKits()) {
            addItem(createKitIcon(kit));
        }
        open(player);
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
            callback.accept(kit);
        });
    }

}