package me.stipe.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.stipe.battlegameshungerroyale.guis.EquipmentSetGui;
import me.stipe.battlegameshungerroyale.managers.KitManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class KitConfig implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            Kit kit = getModifiedKitFromPlayer(p);

            if (kit != null) {
                if (args.length == 0) {
                    sendGui(kit, p);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("name")) {
                    kit.setName(Tools.rebuildString(args, 1));
                    sendGui(kit, p);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("desc")) {
                    kit.setDescription(Tools.rebuildString(args, 1));
                    sendGui(kit, p);
                    return true;
                }
                else if (args[0].equalsIgnoreCase("icon")) {
                    kit.setIcon(Material.valueOf(Tools.rebuildString(args, 1).toUpperCase()));
                    sendGui(kit, p);
                    return true;
                }
                else {
                    p.sendMessage(Tools.componentalize("You must save or discard the current changes before modifying any other kits."));
                    return true;
                }

            }

            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("new") && args.length >= 2) {
                    Kit newKit = new Kit(args[1] + "_" + p.getName());
                    newKit.setName(args[1]);
                    saveModifiedKitToPlayer(newKit, p);
                    sendGui(newKit, p);
                }

                kit = BGHR.getKitManager().getKit(Tools.rebuildString(args, 0));
                if (kit == null) {
                    p.sendMessage(Component.text("That's not a valid kit"));
                    return true;
                }
                sendGui(kit, p);
                return true;
            }
        }
        return true;
    }

    private void saveModifiedKitToPlayer(Kit kit, Player p) {
        Kit copy = kit.copyThis();
        Tools.saveObjectToPlayer("kitconfig", copy, p);
    }

    private Kit getModifiedKitFromPlayer(Player p) {
        if (p.hasMetadata("kitconfig")) {
            Object o = Tools.getObjectFromPlayer("kitconfig", p);
            if (o instanceof Kit) {
                return (Kit) o;
            }
        }
        return null;
    }

    private void sendGui(Kit kit, Player player) {
        Gui gui = Gui.gui().rows(1).title(Component.text("Kit Configuration")).disableAllInteractions().create();

        gui.addItem(nameAndDescriptionIcon(kit));
        gui.addItem(kitIcon(kit));
        gui.addItem(equipmentIcon(kit, gui));
        gui.setItem(8, saveIcon(kit));
        gui.setItem(7, cancelIcon(kit));
        for (Ability a : kit.getAbilities()) {
            gui.addItem(editAbilityIcon(kit, a, gui));
        }
        gui.addItem(newAbilityIcon(kit));
        gui.open(player);
    }

    private GuiItem equipmentIcon(Kit kit, Gui gui) {
        List<Component> lore = new ArrayList<>();
        boolean noGear = true;
        lore.add(Tools.BLANK_LINE);


        if (!kit.getEquipment().getArmor().isEmpty()) {
            lore.add(Tools.componentalize("Armor"));
            noGear = false;
            for (ItemStack item : kit.getEquipment().getArmor().values()) {
                lore.add(item.displayName());
            }
        }
        if (!kit.getEquipment().getOtherItems().isEmpty()) {
            lore.add(Tools.componentalize("Other Starting Items:"));
            noGear = false;
            for (ItemStack item : kit.getEquipment().getOtherItems())
                lore.add(item.displayName());
        }
        if (noGear)
            lore.add(Tools.componentalize("&7None"));
        lore.add(Tools.BLANK_LINE);
        lore.add(Tools.componentalize("&bClick to modify equipment"));

        return ItemBuilder.from(Material.ARMOR_STAND).name(Tools.componentalize("Kit Equipment:")).lore(lore)
                .asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    new EquipmentSetGui(kit.getEquipment(), gui).open(e.getWhoClicked());
        });
    }

    private GuiItem cancelIcon(Kit kit) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to cancel and discard all changes");

        return ItemBuilder.from(Material.BARRIER).name(Tools.componentalize("&eCancel and Exit"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        KitManager kitManager = BGHR.getKitManager();
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();

                        kitManager.replaceKit(kit);
                        Tools.removeObjectFromPlayer("kitconfig", p);
                        Tools.removeObjectFromPlayer("abilityconfig", p);
                        Tools.removeObjectFromPlayer("potioneffect", p);
                    }
                });
    }

    private GuiItem saveIcon(Kit kit) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to save these changes and update this kit");

        return ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave and Exit"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        KitManager kitManager = BGHR.getKitManager();
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();

                        kitManager.replaceKit(kit);
                        Tools.removeObjectFromPlayer("kitconfig", p);
                        Tools.removeObjectFromPlayer("abilityconfig", p);
                        Tools.removeObjectFromPlayer("potioneffect", p);
                        p.sendMessage(Tools.componentalize("&fYour changes to &e" + kit.getName() + " &fhave been saved and reloaded."));
                        kit.saveConfig();
                    }
                });
    }

    private GuiItem newAbilityIcon(Kit kit) {
        String iconName = "&eAdd an Ability&f";
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to add a new ability to this kit");

        return ItemBuilder.from(Material.ENCHANTED_GOLDEN_APPLE).name(Tools.componentalize(iconName)).lore(Tools.componentalize(lore))
                .asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    saveModifiedKitToPlayer(kit, (Player) e.getWhoClicked());
                    Bukkit.dispatchCommand(e.getWhoClicked(),"abilities");
                });
    }

    private GuiItem editAbilityIcon(Kit kit, Ability ability, Gui gui) {
        String iconName = "&eAbility: &f" + ability.getName();
        String abilityType = (ability instanceof ActiveAbility) ? "&aActive" : "&6Passive";
        Material icon = (ability instanceof ActiveAbility) ? Material.LIME_BANNER : Material.YELLOW_BANNER;
        List<String> lore = new ArrayList<>();
        if (ability.getCustomName() != null)
            lore.add("&eCustom Name: &f" + ability.getCustomName());
        lore.add("&eAbility Type: &f" + abilityType);
        lore.add("");
        lore.addAll(Tools.wrapText(ability.getDescription(), ChatColor.GRAY));
        lore.add("");
        lore.add("Ability Options:");

        for (String s : ability.getConfig().getKeys(false)) {
            Object o = ability.getConfig().get(s);
            String value = o.toString();
            if (o instanceof Material)
                value = ((Material) o).name().toLowerCase();
            if (o instanceof PotionEffect) {
                PotionEffect e = (PotionEffect) o;
                value = e.getType().getName().toLowerCase() + " " + Tools.integerToRomanNumeral(e.getAmplifier() + 1);
            }

            lore.addAll(Tools.wrapText(String.format("&6%s: &e%s", s, value), ChatColor.YELLOW));
        }

        lore.add("");
        lore.add("&bClick to MODIFY this ability");
        lore.add("&bShift+Click to REMOVE this ability");

        return ItemBuilder.from(icon).name(Tools.componentalize(iconName)).lore(Tools.componentalize(lore)).asGuiItem(e -> {
            if (e.getWhoClicked() instanceof Player) {
                Player p = (Player) e.getWhoClicked();

                Kit kitCopy = kit.copyThis();
                kitCopy.removeAbility(ability);
                saveModifiedKitToPlayer(kitCopy, p);

                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    e.getWhoClicked().closeInventory();
                    Bukkit.dispatchCommand(e.getWhoClicked(), "kitconfig");
                    return;
                }

                p.closeInventory();
                Tools.saveObjectToPlayer("abilityconfig", ability.newWithDefaults(), p);
                Bukkit.dispatchCommand(p, "abilityconfig");
            }
        });
    }

    private GuiItem kitIcon(Kit kit) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to change this kit's icon");

        return ItemBuilder.from(kit.getIcon()).name(Tools.componentalize("&eKit Icon"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        // send another gui to get an icon
                        p.closeInventory();
                        saveModifiedKitToPlayer(kit, p);
                        pickIconGui().open(p);
                    }
                });
    }

    private GuiItem nameAndDescriptionIcon(Kit kit) {
        Component name = Tools.componentalize("&eKit Name: &f" + kit.getName());
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(""));
        lore.addAll(Tools.componentalize(Tools.wrapText(kit.getDescription(), ChatColor.GRAY)));
        lore.add(Component.text(""));
        lore.add(Tools.componentalize("&bLeft Click to change the name of this kit"));
        lore.add(Tools.componentalize("&bRight Click to change the description of this kit"));

        return ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(name).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        if (e.getClick().isLeftClick())
                            p.sendMessage(Tools.commandInstructions("/kitconfig name <new kit name>", kit.getName()));
                        else if (e.getClick().isRightClick())
                            p.sendMessage(Tools.commandInstructions("/kitconfig desc <new description>", kit.getDescription()));
                        else
                            return;
                        p.closeInventory();
                        saveModifiedKitToPlayer(kit, p);
                    }
                });
    }

    private ScrollingGui pickIconGui() {
        ScrollingGui gui = Gui.scrolling().title(Component.text("Choose a Kit Icon")).rows(6).pageSize(45).create();

        gui.setDefaultClickAction(e -> e.setCancelled(true));
        gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Up")).asGuiItem(event -> gui.previous()));
        gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Component.text("Scroll Down")).asGuiItem(event -> gui.next()));
        gui.setItem(6, 5, ItemBuilder.from(Material.BARRIER).name(Component.text("Cancel and Go Back")).asGuiItem(event -> {
            event.getWhoClicked().closeInventory();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(event.getWhoClicked(), "kitconfig ");
                }
            }.runTaskLater(BGHR.getPlugin(), 5L);
        }));

        for (Material m : Material.values()) {
            if (m != null && !m.isAir() && m.isItem() && !m.name().contains("LEGACY")) {
                gui.addItem(ItemBuilder.from(m).name(Component.text(ChatColor.WHITE + m.name().toLowerCase())).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        if (e.getCurrentItem() != null) {
                            String material = e.getCurrentItem().getType().name().toLowerCase();
                            p.closeInventory();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Bukkit.dispatchCommand(p, "kitconfig icon " + material);
                                }
                            }.runTaskLater(BGHR.getPlugin(), 5L);
                        }
                    }
                }));
            }
        }
        return gui;
    }

}
