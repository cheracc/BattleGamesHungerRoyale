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
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

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
        String hotbarWarning = "These items will simply be given to the player wherever they have room.";
        GuiItem guiItem;
        int slot = SLOTS.length;

        for (ItemStack item : set.getOtherItems()) {
            if (item == null || item.getType() == Material.AIR) {
                Component name = Tools.componentalize("Empty Hotbar Item Slot");
                List<Component> lore = Tools.componentalize(Tools.wrapText(emptyInstructions, ChatColor.AQUA));
                lore.addAll(Tools.componentalize(Tools.wrapText(hotbarWarning, ChatColor.GOLD)));
                lore.add(0, Tools.BLANK_LINE);
                guiItem = ItemBuilder.from(getIconItem(EquipmentSet.EquipmentSetSlot.values()[slot])).name(name).lore(lore).asGuiItem(handleClicks(slot));
            }
            else {
                ItemStack iconItem = item.clone();
                List<Component> lore = new ArrayList<>();

                lore.add(Tools.BLANK_LINE);
                lore.addAll(Tools.componentalize(Tools.wrapText(instructions, ChatColor.AQUA)));
                lore.addAll(Tools.componentalize(Tools.wrapText(hotbarWarning, ChatColor.GOLD)));

                guiItem = ItemBuilder.from(iconItem).lore(lore).asGuiItem();
            }
            guiItem.setAction(handleClicks(slot));

            if (getGuiItem(slot) != null)
                updateItem(slot, guiItem);
            else
                setItem(slot, guiItem);
            slot++;
        }
    }

    private void fillArmorSlots() {
        String instructions = "&bPlace &e%s &bhere to add it to this kit's starting equipment set.";
        String offHandWarning = "Items placed here CANNOT be removed from the off hand slot. This makes it impossible for this kit to move ability items in their hotbar - use with caution!";
        String replaceInstructions = "&bClick here to remove this item, or drop another item here to replace it.";

        for (int i = 0; i < SLOTS.length; i++) {
            if (set.getArmor().get(SLOTS[i]) == null || set.getArmor().get(SLOTS[i]).getType().isAir()) {
                Component name = Tools.componentalize("&eEmpty Armor Slot");
                List<Component> lore = Tools.componentalize(Tools.wrapText(String.format(instructions, slotArmorNames[i]), ChatColor.AQUA));

                if (SLOTS[i] == EquipmentSlot.OFF_HAND) {
                    lore.add(Tools.BLANK_LINE);
                    lore.addAll(Tools.componentalize(Tools.wrapText(offHandWarning, ChatColor.RED)));
                }
                lore.add(0, Tools.BLANK_LINE);
                GuiItem guiItem = ItemBuilder.from(getIconItem(EquipmentSet.EquipmentSetSlot.values()[i])).name(name).lore(lore).asGuiItem(handleClicks(i));

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
                if (itemOnCursor.getItemMeta() != null && doesThisFit(itemOnCursor, slot)) {
                    set.setItem(slot, itemOnCursor);
                    event.getWhoClicked().setItemOnCursor(null);
                }
            }
            if (getGuiItem(slot) != null && itemInSlot != null) {
                itemInSlot.lore(new ArrayList<>());
                set.setItem(slot, null);
                Component displayName = itemInSlot.getItemMeta().displayName();
                if (displayName == null)
                    displayName = Component.space();

                if (!displayName.toString().toLowerCase().contains("empty"))
                    event.getWhoClicked().setItemOnCursor(itemInSlot);

                if (itemOnCursor.getItemMeta() != null && doesThisFit(itemOnCursor, slot)) {
                    set.setItem(slot, itemOnCursor);
                    event.getWhoClicked().setItemOnCursor(null);
                }

                fillOtherSlots();
                fillArmorSlots();
            }
        };
    }

    private ItemStack getIconItem(EquipmentSet.EquipmentSetSlot slot) {
        ItemStack icon;
        switch (slot) {
            case HEAD:
                icon = new ItemStack(Material.LEATHER_HELMET);
                break;
            case CHEST:
                icon = new ItemStack(Material.LEATHER_CHESTPLATE);
                break;
            case LEGS:
                icon = new ItemStack(Material.LEATHER_LEGGINGS);
                break;
            case FEET:
                icon = new ItemStack(Material.LEATHER_BOOTS);
                break;
            case OFF_HAND:
                icon = new ItemStack(Material.SHIELD);
                break;
            default:
                icon = new ItemStack(Material.GLASS_BOTTLE);
        }
        if (icon.getType().name().toLowerCase().contains("leather")) {
            LeatherArmorMeta leatherMeta;
            leatherMeta = (LeatherArmorMeta) icon.getItemMeta();
            leatherMeta.setColor(Color.GRAY);
            icon.setItemMeta(leatherMeta);
        }
        if (icon.getType() == Material.SHIELD) {
            BlockStateMeta meta = (BlockStateMeta) icon.getItemMeta();
            BlockState state = meta.getBlockState();

            Banner banner = (Banner) state;
            banner.setBaseColor(DyeColor.GRAY);
            banner.update();
            meta.setBlockState(banner);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private boolean doesThisFit(ItemStack item, EquipmentSlot slot) {
        String typeAsString = item.getType().name().toLowerCase().replace("_", " ");
        String slotDescriptor = "";

        switch (slot) {
            case HEAD:
                slotDescriptor = "helmet";
                break;
            case CHEST:
                slotDescriptor = "chestplate";
                break;
            case LEGS:
                slotDescriptor = "leggings";
                break;
            case FEET:
                slotDescriptor = "boots";
                break;
        }

        return typeAsString.contains(slotDescriptor);
    }

    private boolean doesThisFit(ItemStack armor, int slot) {
        if (slot >= 4)
            return true;
        switch (slot) {
            case 0:
                return doesThisFit(armor, EquipmentSlot.HEAD);
            case 1:
                return doesThisFit(armor, EquipmentSlot.CHEST);
            case 2:
                return doesThisFit(armor, EquipmentSlot.LEGS);
            case 3:
                return doesThisFit(armor, EquipmentSlot.FEET);
        }
        return true;
    }
}
