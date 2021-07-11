package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.types.EquipmentSet;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public class EquipmentSetGui extends Gui {
    private final EquipmentSet set;
    private final Gui sendingGui;
    private final static Material EMPTY_ARMOR_ICON = Material.ARMOR_STAND;
    private final static EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFF_HAND};
    private final static String[] slotArmorNames = {"a helmet", "a chestplate", "some leggings",  "some boots", "an off hand item"};

    public EquipmentSetGui(HumanEntity player, EquipmentSet set, Gui sendingGui, Consumer<EquipmentSet> callback) {
        super(1, "Current Equipment:", new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.setDefaultTopClickAction(e -> e.setCancelled(true));
        this.set = set;
        this.sendingGui = sendingGui;
        if (set == null)
            set = EquipmentSet.newEquipmentSet();

        setOutsideClickAction(e -> sendingGui.open(e.getWhoClicked()));
        fillArmorSlots();
        fillOtherSlots();
        setItem(8, ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("Save this Equipment")).asGuiItem(e -> {
            Tools.saveObjectToPlayer("equipment", this.set, (Player) e.getWhoClicked());
            e.getWhoClicked().closeInventory();
            callback.accept(this.set);
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
            guiItem.setAction(handleClicks(slot));

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
            addItem(ItemBuilder.from(Material.LIME_STAINED_GLASS_PANE).name(name).lore(lore).asGuiItem(handleClicks(slot)));
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
                GuiItem guiItem = ItemBuilder.from(EMPTY_ARMOR_ICON).name(name).lore(lore).asGuiItem(handleClicks(i));

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
                GuiItem guiItem = ItemBuilder.from(item).asGuiItem(handleClicks(i));
                if (getGuiItem(i) != null)
                    updateItem(i, guiItem);
                else
                    setItem(i, guiItem);
            }
        }
    }


    private GuiAction<InventoryClickEvent> handleClicks(int slot) {
        return event -> {
            ItemStack itemOnCursor = event.getWhoClicked().getItemOnCursor();
            ItemStack itemInSlot = Objects.requireNonNull(getGuiItem(slot)).getItemStack();

            if (itemInSlot == null || LegacyComponentSerializer.legacyAmpersand().serialize(itemInSlot.displayName()).contains("Empty")) {
                if (itemOnCursor.getItemMeta() != null && itemFitsInSlot(itemOnCursor, slot)) {
                    set.setItem(slot, itemOnCursor);
                    event.getWhoClicked().setItemOnCursor(null);
                }
            }
            if (getGuiItem(slot) != null && itemInSlot != null) {
                itemInSlot.lore(new ArrayList<>());
                set.setItem(slot, null);

                if (!itemInSlot.displayName().toString().toLowerCase().contains("empty"))
                    event.getWhoClicked().setItemOnCursor(itemInSlot);

                if (itemOnCursor.getItemMeta() != null && itemFitsInSlot(itemOnCursor, slot))
                    set.setItem(slot, itemOnCursor);

                fillOtherSlots();
                fillArmorSlots();
            }
        };
    }


    private boolean itemFitsInSlot(ItemStack armor, int slot) {
        if (slot >= 5)
            return true;
        for (int i = 0; i < SLOTS.length; i++) {
            String[] words = armor.getType().name().toLowerCase().split("_");
            for (String word : words)
                if (EquipmentSlot.values()[slot] == SLOTS[i] && slotArmorNames[i].contains(word))
                    return true;
        }
        return false;
    }
}
