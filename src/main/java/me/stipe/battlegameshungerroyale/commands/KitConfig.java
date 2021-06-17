package me.stipe.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitConfig implements CommandExecutor {
    Map<Kit, Gui> activeGuis = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            Kit kit = getKitFromTag(p);

            if (args.length >= 1) {
                if (kit == null) {
                    kit = BGHR.getKitManager().getKit(args[0]);
                    if (kit == null) {
                        p.sendMessage(Component.text("That's not a valid kit"));
                        return true;
                    }
                    sendGui(kit, p);
                }
                if (args[0].equalsIgnoreCase("name")) {
                    kit.setName(Tools.rebuildString(args, 1));
                    activeGuis.remove(kit);
                    sendGui(kit, p);
                }
                if (args[0].equalsIgnoreCase("desc")) {
                    kit.setDescription(Tools.rebuildString(args, 1));
                    activeGuis.remove(kit);
                    sendGui(kit, p);
                }
                if (args[0].equalsIgnoreCase("icon")) {
                    kit.setIcon(Material.valueOf(Tools.rebuildString(args, 1).toUpperCase()));
                    activeGuis.remove(kit);
                    sendGui(kit, p);
                }

                sendGui(kit, p);
                return true;
            }
            if (kit != null) {
                sendGui(kit, p);
            }
        }

        return true;
    }

    private void sendGui(Kit kit, Player player) {
        if (activeGuis.containsKey(kit)) {
            activeGuis.get(kit).open(player);
            return;
        }

        Gui gui = Gui.gui().rows(1).title(Component.text("Kit Configuration")).disableAllInteractions().create();

        gui.addItem(nameAndDescriptionIcon(kit));
        gui.addItem(kitIcon(kit));
        gui.setItem(8, saveIcon(kit));
        activeGuis.put(kit, gui);
        gui.open(player);
    }

    private void tagPlayer(Player p, Kit kit) {
        p.setMetadata("kitconfig", new FixedMetadataValue(BGHR.getPlugin(), kit));
    }

    private Kit getKitFromTag(Player p) {
        if (p.hasMetadata("kitconfig")) {
            Object o = p.getMetadata("kitconfig").get(0).value();
            if (o instanceof Kit) {
                p.removeMetadata("kitconfig", BGHR.getPlugin());
                return (Kit) o;
            }
        }
        return null;
    }

    private GuiItem saveIcon(Kit kit) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to save these changes");

        return ItemBuilder.from(kit.getIcon()).name(Tools.componentalize("&eSave and Exit"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        // send another gui to get an icon
                        p.closeInventory();
                        kit.saveConfig();
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
                        tagPlayer(p, kit);
                        pickIconGui().open(p);
                    }
                });
    }

    private GuiItem nameAndDescriptionIcon(Kit kit) {
        TextComponent name = Component.text(kit.getName());
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(""));
        lore.addAll(Tools.componentalize(Tools.wrapText(kit.getDescription(), ChatColor.GRAY)));
        lore.add(Component.text(""));
        lore.add(Tools.componentalize("&bLeft Click to change the name of this kit"));
        lore.add(Tools.componentalize("&bRight Click to change the description of this kit"));

        return ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(Tools.componentalize("&eKit Name: &f" + kit.getName())).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES)
                .asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        if (e.getClick().isLeftClick())
                            p.sendMessage(Tools.commandInstructions("/kitconfig name <new kit name>"));
                        else if (e.getClick().isRightClick())
                            p.sendMessage(Tools.commandInstructions("/kitconfig desc <new description>"));
                        else
                            return;
                        p.closeInventory();
                        tagPlayer(p, kit);
                    }
                });
    }

    private ScrollingGui pickIconGui() {
        ScrollingGui gui = Gui.scrolling().title(Component.text("Choose a Map Icon")).rows(6).pageSize(45).create();

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
