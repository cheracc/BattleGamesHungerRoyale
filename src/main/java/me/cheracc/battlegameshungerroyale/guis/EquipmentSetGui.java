package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.datatypes.EquipmentSet;
import me.cheracc.battlegameshungerroyale.guis.interfaces.GetEquipmentSet;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class EquipmentSetGui extends Gui {
    private final EquipmentSet set;
    private final static Material EMPTY_ARMOR_ICON = Material.ARMOR_STAND;
    private final static EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFF_HAND};
    private final static String[] slotArmorNames = {"a helmet", "a chestplate", "some leggings",  "some boots", "an off hand item"};

    public EquipmentSetGui(HumanEntity player, EquipmentSet set, Gui sendingGui, GetEquipmentSet callback) {
        super(1, "&0Current Equipment:", new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.setDefaultTopClickAction(e -> e.setCancelled(true));
        this.set = set;
        if (set == null)
            set = EquipmentSet.newEquipmentSet();

        setOutsideClickAction(e -> sendingGui.open(e.getWhoClicked()));
        fillArmorSlots();
        fillOtherSlots();
        setItem(8, ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("Save this Equipment")).asGuiItem(e -> {
            Tools.saveObjectToPlayer("equipment", this.set, (Player) e.getWhoClicked());
            e.getWhoClicked().closeInventory();
            callback.equipmentCallback(this.set);
        }));

        open(player);
    }

    private void fillOtherSlots() {
        String instructions = "Click here to remove this item, or drop another item here to replace it.";
        String emptyInstructions = "Drop an item here to add it to this kit's hotbar.";
        String hotbarWarning = "Items placed here will be locked to the player's hotbar or off hand slots and cannot be dropped.";
        int slot = SLOTS.length;

        for (ItemStack item : set.getOtherItems()) {
            ItemStack iconItem = item.clone();
            Component name = Tools.componentalize("Hotbar Item:");
            List<Component> lore = new ArrayList<>();

            lore.add(0, iconItem.displayName());
            lore.add(Tools.BLANK_LINE);
            lore.addAll(Tools.componentalize(Tools.wrapText(instructions, ChatColor.AQUA)));
            lore.addAll(Tools.componentalize(Tools.wrapText(hotbarWarning, ChatColor.GOLD)));

            GuiItem guiItem = ItemBuilder.from(iconItem).name(name).lore(lore).asGuiItem();
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
            addItem(ItemBuilder.from(Material.LIME_STAINED_GLASS_PANE).name(name).lore(lore).asGuiItem(handleOtherItems(null)));
            slot++;
        }
    }

    private void fillArmorSlots() {
        String instructions = "&bPlace &e%s &bhere to add it to this kit's starting equipment set.";
        String offHandWarning = "Items placed here CANNOT be removed from the off hand slot. This makes it impossible for this kit to move ability items in their hotbar - use with caution!";
        String replaceInstructions = "&bClick here to remove this item, or drop another item here to replace it.";

        for (int i = 0; i < SLOTS.length; i++) {
            if (set.getArmor().get(SLOTS[i]) == null) {
                Component name = Tools.componentalize("&eEmpty Armor Slot");
                List<Component> lore = Tools.componentalize(Tools.wrapText(String.format(instructions, slotArmorNames[i]), ChatColor.AQUA));

                if (SLOTS[i] == EquipmentSlot.OFF_HAND) {
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
                ItemStack item = set.getArmor().get(SLOTS[i]).clone();
                List<Component> lore = new ArrayList<>();
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
            ItemStack cursor = event.getCursor();

            if (cursor == null || cursor.getItemMeta() == null || cursor.getType().isAir()) {
                if (set.getArmor().get(SLOTS[slotNumber]) != null) {
                    set.removeArmor(SLOTS[slotNumber]);
                    fillArmorSlots();
                    return;
                }
            }

            if (armorFitsInSlot(cursor, SLOTS[slotNumber])) {
                set.setArmor(SLOTS[slotNumber], cursor);
                event.getWhoClicked().setItemOnCursor(null);
                fillArmorSlots();
            } else {
                event.getWhoClicked().sendMessage(errorMessage);
            }

        };
    }

    private GuiAction<InventoryClickEvent> handleOtherItems(@Nullable ItemStack currentItem) {
        return event -> {
            ItemStack cursorItem = event.getWhoClicked().getItemOnCursor();

            if (cursorItem == null && currentItem != null) {
                Bukkit.getLogger().info("cursor: null current: " + currentItem.getI18NDisplayName());
                set.removeItem(currentItem);
                fillOtherSlots();
            } else if (cursorItem != null) {
                Bukkit.getLogger().info("cursor: " + cursorItem.getI18NDisplayName() + " current: " + ((currentItem == null) ? "null" : currentItem.getI18NDisplayName()) );
                if (currentItem != null)
                    set.removeItem(currentItem);
                set.addOtherItem(cursorItem);

                event.getWhoClicked().setItemOnCursor(null);
                fillOtherSlots();
            } else
                Bukkit.getLogger().info("both null");
        };
    }

    private boolean armorFitsInSlot(ItemStack armor, EquipmentSlot slot) {
        if (slot == EquipmentSlot.OFF_HAND)
            return true;
        for (int i = 0; i < SLOTS.length; i++) {
            String[] words = armor.getType().name().toLowerCase().split("_");
            for (String word : words)
                if (slot == SLOTS[i] && slotArmorNames[i].contains(word))
                    return true;
        }
        return false;
    }
}
