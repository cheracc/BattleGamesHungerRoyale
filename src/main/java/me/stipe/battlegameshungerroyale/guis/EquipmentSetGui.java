package me.stipe.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.datatypes.EquipmentSet;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EquipmentSetGui extends Gui {
    private final EquipmentSet set;
    private final static Material EMPTY_ARMOR_ICON = Material.ARMOR_STAND;
    private final static EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFF_HAND};
    private final static String[] slotArmorNames = {"a helmet", "a chestplate", "some leggings",  "some boots", "an off hand item"};

    public EquipmentSetGui(EquipmentSet set, Gui sendingGui) {
        super(1, Tools.componentalize("&0Current Equipment:"));
        this.setDefaultTopClickAction(e -> e.setCancelled(true));
        this.set = set;

        fillArmorSlots();
        fillOtherSlots();
        setItem(8, ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("Save this Equipment")).asGuiItem(e -> {
            Tools.saveObjectToPlayer("equipment", set, (Player) e.getWhoClicked());
            e.getWhoClicked().closeInventory();
            sendingGui.open(e.getWhoClicked());
        }));
    }

    private void fillOtherSlots() {
        String instructions = "Click here to remove this item, or drop another item here to replace it.";
        String emptyInstructions = "Drop an item here to add it to this kit's hotbar.";
        String hotbarWarning = "Items placed here will be locked to the player's hotbar or off hand slots and cannot be dropped.";
        int slot = slots.length;

        for (ItemStack item : set.getOtherItems()) {
            Component name = Tools.componentalize("Hotbar Item:");
            List<Component> lore = new ArrayList<>(item.lore());

            lore.add(0, item.displayName());
            lore.add(Tools.BLANK_LINE);
            lore.addAll(Tools.componentalize(Tools.wrapText(instructions, ChatColor.AQUA)));
            lore.addAll(Tools.componentalize(Tools.wrapText(hotbarWarning, ChatColor.GOLD)));

            GuiItem guiItem = ItemBuilder.from(item).name(name).lore(lore).asGuiItem();
            guiItem.setAction(handleOtherItems(item));

            if (getGuiItem(slot) != null)
                updateItem(slot, guiItem);
            else
                setItem(slot, guiItem);
            slot++;
        }
        while (slot < 8) {
            Component name = Tools.componentalize("Empty Hotbar Item Slot");
            List<Component> lore = Tools.componentalize(Tools.wrapText(emptyInstructions, ChatColor.AQUA));
            lore.addAll(Tools.componentalize(Tools.wrapText(hotbarWarning, ChatColor.GOLD)));
            lore.add(0, Tools.BLANK_LINE);
            addItem(ItemBuilder.from(Material.LIME_STAINED_GLASS_PANE).name(name).lore(lore).asGuiItem());
            slot++;
        }
    }

    private void fillArmorSlots() {
        String instructions = "&bPlace &e%s &bhere to add it to this kit's starting equipment set.";
        String offHandWarning = "Items placed here CANNOT be removed from the off hand slot. This makes it impossible for this kit to move ability items in their hotbar - use with caution!";
        String replaceInstructions = "&bClick here to remove this item, or drop another item here to replace it.";

        for (int i = 0; i < slots.length; i++) {
            if (set.getArmor().get(slots[i]) == null) {
                Component name = Tools.componentalize("&eEmpty Armor Slot");
                List<Component> lore = Tools.componentalize(Tools.wrapText(String.format(instructions, slotArmorNames[i]), ChatColor.AQUA));

                if (slots[i] == EquipmentSlot.OFF_HAND) {
                    lore.add(Tools.BLANK_LINE);
                    lore.addAll(Tools.componentalize(Tools.wrapText(offHandWarning, ChatColor.RED)));
                }
                lore.add(0, Tools.BLANK_LINE);
                GuiItem guiItem = ItemBuilder.from(EMPTY_ARMOR_ICON).name(name).lore(lore).asGuiItem(handleArmorClicks(i));

                if (getGuiItem(i) != null)
                    updateItem(i, guiItem);
                else
                    setItem(i, guiItem);
            } else {
                ItemStack item = set.getArmor().get(slots[i]);
                List<Component> lore = new ArrayList<>();
                if (item.lore() != null)
                    lore.addAll(item.lore());
                lore.add(Tools.BLANK_LINE);
                lore.addAll(Tools.componentalize(Tools.wrapText(replaceInstructions, ChatColor.AQUA)));
                item.lore(lore);
                GuiItem guiItem = ItemBuilder.from(item).asGuiItem(handleArmorClicks(i));
                if (getGuiItem(i) != null)
                    updateItem(i, guiItem);
                else
                    setItem(i, guiItem);
            }
        }
    }

    private GuiAction<InventoryClickEvent> handleArmorClicks(int slotNumber) {
        Component errorMessage = Tools.componentalize("That won't fit there");

        return event -> {
            ItemStack input = event.getCursor();

            if (input == null || input.getItemMeta() == null || input.getType().isAir()) return;

            if (armorFitsInSlot(input, slots[slotNumber])) {
                set.setArmor(slots[slotNumber], input);
                event.getWhoClicked().setItemOnCursor(null);
                fillArmorSlots();
            } else {
                event.getWhoClicked().sendMessage(errorMessage);
            }

        };
    }

    private GuiAction<InventoryClickEvent> handleOtherItems(@Nullable ItemStack currentItem) {
        return event -> {
            ItemStack cursorItem = event.getCursor();

            if (cursorItem == null && currentItem != null) {
                set.removeItem(currentItem);
                fillArmorSlots();
            } if (cursorItem != null) {
                if (currentItem != null)
                    set.removeItem(currentItem);
                set.addOtherItem(cursorItem);

                event.getWhoClicked().setItemOnCursor(null);
                fillArmorSlots();
            }
        };
    }

    private boolean armorFitsInSlot(ItemStack armor, EquipmentSlot slot) {
        if (slot == EquipmentSlot.OFF_HAND)
            return true;
        for (int i = 0; i < slots.length; i++) {
            String[] words = armor.getType().name().toLowerCase().split("_");
            for (String word : words)
                if (slot == slots[i] && slotArmorNames[i].contains(word))
                    return true;
        }
        return false;
    }
}
