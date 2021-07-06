package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.Game;
import me.cheracc.battlegameshungerroyale.datatypes.GameOptions;
import me.cheracc.battlegameshungerroyale.datatypes.Kit;
import me.cheracc.battlegameshungerroyale.datatypes.MapData;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class AdminGui extends Gui {

    public AdminGui(HumanEntity player) {
        super(1, Tools.componentalize("&0Admin Menu"));
        disableAllInteractions();
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        fillGui();

        open(player);
    }

    private void fillGui() {
        setItem(0, mainConfigIcon());
        setItem(1, kitsIcon());
        setItem(2, mapsIcon());
        setItem(3, gamesIcon());
        setItem(8, exitIcon());
    }

    private GuiItem kitsIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.STONE_SWORD).name(Tools.componentalize("&eManage Kits"));
        icon.flags(ItemFlag.HIDE_ATTRIBUTES);
        icon.lore(Tools.componentalize(Tools.wrapText("&7  Create or modify kits and their abilities", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendKitsAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem mapsIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.FILLED_MAP).name(Tools.componentalize("Manage Maps"));
        icon.lore(Tools.componentalize(Tools.wrapText("&7  View and modify the configuration for the available game maps, or load a map for editing (like placing chests or fixing spots where players get stuck)", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendMapsAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem gamesIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize("Manage Game Configurations"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendGameAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem mainConfigIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(Tools.componentalize("&eManage Plugin"));
        icon.lore(Tools.componentalize(Tools.wrapText("&7  General Settings that effect how the plugin is loaded and what it does.", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendPluginAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem exitIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.BARRIER).name(Tools.componentalize("Close Menu"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
        });
    }

    public void sendPluginAdminGui(HumanEntity player) {
        List<BaseIcon> icons = new ArrayList<>();

        new BaseAdminGui(player, "Plugin Configuration", icons);
    }

    public void sendKitsAdminGui(HumanEntity player) {
        List<BaseIcon> icons = new ArrayList<>();

        for (Kit kit : BGHR.getKitManager().getLoadedKits()) {
            icons.add(slot -> {
                ItemBuilder icon = ItemBuilder.from(Material.STONE_SWORD).name(Tools.componentalize(kit.getName()));
                icon.flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                List<Component> lore = new ArrayList<>(Tools.componentalize(Tools.wrapText(kit.getDescription(), ChatColor.GRAY)));
                lore.add(0, Component.text(""));
                lore.add(Tools.BLANK_LINE);
                lore.add(Tools.componentalize("&eKit Abilities:"));
                for (Ability a : kit.getAbilities()) {
                    String color = (a instanceof ActiveAbility) ? "&a" : "&6";
                    String abilityString = String.format("%s%s &f- &7%s", color, a.getCustomName() != null ? a.getCustomName() : a.getName(),
                            a.getDescription());
                    lore.addAll(Tools.componentalize(Tools.wrapText(abilityString, ChatColor.GRAY)));
                    lore.add(Tools.BLANK_LINE);
                }

                icon.lore(lore);
                return icon.asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    new ConfigureKitGui(kit, this, player);
                });
            });
        }
        new BaseAdminGui(player, "Kit Configurations", icons);
    }

    public void sendMapsAdminGui(HumanEntity player) {
        List<BaseIcon> icons = new ArrayList<>();

        for (MapData map : MapManager.getInstance().getMaps()) {
            icons.add(slot -> {
                ItemBuilder icon = ItemBuilder.from(Material.FILLED_MAP).name(Tools.componentalize(map.getMapName()));
                List<String> lore = new ArrayList<>();
                lore.add("&fCreator: &7" + map.getMapCreator());
                lore.addAll(Tools.wrapText("&fDescription: &2" + map.getMapDescription(), ChatColor.DARK_GREEN));
                lore.add("");
                lore.add("&fBorder: &7" + (map.isUseBorder() ? "yes" : "no"));
                if (map.isUseBorder())
                    lore.add("&fBorder Radius: &7" + map.getBorderRadius());
                icon.lore(Tools.componentalize(lore));
                return icon.asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    new ConfigureMapGui(player, this, map);
                });
            });
        }
        new BaseAdminGui(player, "Map Configurations", icons);
    }

    public void sendGameAdminGui(HumanEntity player) {
        Set<BaseIcon> icons = new HashSet<>();

        for (GameOptions game : GameManager.getInstance().getAllConfigs())
            icons.add(slot -> {
                ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize(game.getConfigFile().getName().split("\\.")[0]));
                List<String> lore = new ArrayList<>();
                lore.add("&fMap: &7" + game.getMap().getMapName());
                lore.add("&fPlayers needed to start: &7" + game.getPlayersNeededToStart());
                lore.add("&fStarting style: &7" + game.getStartType().name().toLowerCase());
                lore.add("&fRespawns per player: &7" + (game.getLivesPerPlayer() - 1));
                lore.add("&fFill pre-placed chests: &7" + (game.isFillAllChests() ?  "yes" : "no"));
                lore.add("&fRandomly place loot chests: &7" + (game.isGenerateChests() ? "yes" : "no"));
                lore.add("&fLoot chest respawn time: &7" + game.getChestRespawnTime() + (game.getChestRespawnTime() > 1 ? " minutes" : " minute"));
                lore.add("&fLoot table: &7" + game.getLootTable().getKey().getKey());
                lore.add("");
                lore.add("&bClick to start a new game with this configuration");
                lore.add("&bRight click to edit this configuration");

                icon.lore(Tools.componentalize(lore));

                return icon.asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    if (e.isRightClick()) {
                        new ConfigureGameGui(e.getWhoClicked(), game);
                    } else {
                        Game.createNewGameWithCallback(game.getMap(), game, newGame -> {
                            new SelectGameGui(e.getWhoClicked());
                        });
                    }
                });
            });
        new BaseAdminGui(player, "Game Configurations", icons);
    }

    private static class BaseAdminGui extends Gui {
        public BaseAdminGui(HumanEntity player, String title, Collection<BaseIcon> icons) {
            super(1, Tools.componentalize("&0" + title));
            disableAllInteractions();
            setOutsideClickAction(e -> {
                e.getWhoClicked().closeInventory();
                new AdminGui(player);
            });

            int slot = 0;
            for (BaseIcon icon : icons) {
                this.setItem(slot, icon.createIcon(slot));
                slot++;
            }
            this.open(player);
        }

    }

    private interface BaseIcon {
        GuiItem createIcon(int slot);
    }
}
