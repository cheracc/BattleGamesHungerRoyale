package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.*;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
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
        ItemBuilder icon = ItemBuilder.from(Material.FILLED_MAP).name(Tools.componentalize("&eManage Maps"));
        icon.lore(Tools.componentalize(Tools.wrapText("&7  View and modify the configuration for the available game maps, or load a map for editing (like placing chests or fixing spots where players get stuck)", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendMapsAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem gamesIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize("&eManage Game Configurations"));
        icon.lore(Tools.componentalize(Tools.wrapText("&7  View, modify, or create new game configurations, timers, and rules.", ChatColor.GRAY)));

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
        BGHR plugin = BGHR.getPlugin();
        FileConfiguration config = plugin.getConfig();
        List<BaseIcon> icons = new ArrayList<>();
        BaseAdminGui pluginGui = new BaseAdminGui(player, "&0Plugin Configuration", null);

        icons.add(slot -> {
            boolean value = config.getBoolean("reset main world on each restart", false);
            ItemBuilder icon = ItemBuilder.from(Material.NETHER_STAR).name(Tools.componentalize("&eReset Main World on Restart: &f" +
                    (value ? "no" : "yes")));
            icon.lore(Tools.componentalize(Tools.wrapText("  &7Whether to reset the main world each restart. If enabled, the main world will be reset to the specified map each time the server restarts. &c&lThis &c&lsetting &c&lwill &c&ldelete &c&lyour &c&lmain &c&lworld &c&levery &c&ltime &c&lthe &c&lserver &c&lrestarts!", ChatColor.GRAY)));

            return icon.asGuiItem(e -> {
                config.set("reset main world on each restart", !value);
                plugin.saveConfig();
                pluginGui.updateIcon(slot);
            });
        });
        icons.add(slot -> {
            GameMode value = GameMode.valueOf(config.getString("main world.gamemode", "adventure").toUpperCase());
            ItemBuilder icon = ItemBuilder.from(Material.WOODEN_PICKAXE).name(Tools.componentalize("&eDefault Game Mode: &f" +
                    value.name().toLowerCase()));
            icon.flags(ItemFlag.HIDE_ATTRIBUTES);
            icon.lore(Tools.componentalize(Tools.wrapText("  &7Sets the default game mode for players in the main world. If using a waiting lobby and you do not want players to build in it, choose 'adventure'", ChatColor.GRAY)));

            return icon.asGuiItem(e -> {
                int index = value.ordinal();
                index++;
                if (index >= GameMode.values().length)
                    index = 0;
                config.set("main world.gamemode", GameMode.values()[index].name().toLowerCase());
                plugin.saveConfig();
                for (Player p : MapManager.getInstance().getLobbyWorld().getPlayers()) {
                    p.setGameMode(GameMode.values()[index]);
                }
                pluginGui.updateIcon(slot);
            });
        });
        icons.add(slot -> {
            boolean value = config.getBoolean("main world.place players at spawn on join", true);
            ItemBuilder icon = ItemBuilder.from(Material.BEACON).name(Tools.componentalize("&eAlways Spawn Players at Main Spawn: &f" +
                    (value ? "no" : "yes")));
            icon.lore(Tools.componentalize(Tools.wrapText("  &7Whether players will always be sent to the spawn location when joining the server or teleporting back to the main world. If disabled, players will instead return to their last recorded location in the main world.", ChatColor.GRAY)));

            return icon.asGuiItem(e -> {
                config.set("main world.place players at spawn on join", !value);
                plugin.saveConfig();
                pluginGui.updateIcon(slot);
            });
        });
        icons.add(slot -> {
            boolean value = config.getBoolean("main world.kits useable in main world", true);
            ItemBuilder icon = ItemBuilder.from(Material.STONE_SWORD).name(Tools.componentalize("&eAllow Kits to be used in main world: &f" +
                    (value ? "no" : "yes")));
            icon.flags(ItemFlag.HIDE_ATTRIBUTES);
            icon.lore(Tools.componentalize(Tools.wrapText("  &7Whether kits and kit abilities may be used in the main world. If disabled, players can still select a kit to use, but the kit will not be equipped on them until they join a game.", ChatColor.GRAY)));

            return icon.asGuiItem(e -> {
                config.set("main world.kits useable in main world", !value);
                plugin.saveConfig();
                pluginGui.updateIcon(slot);
                boolean kitsAllowed = !value;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (GameManager.getInstance().getPlayersCurrentGame(p) == null) {
                        PlayerData data = PlayerManager.getInstance().getPlayerData(p);
                        if (data.getKit() != null && !kitsAllowed)
                            data.getKit().disrobePlayer(data);
                        else if (data.getKit() != null && kitsAllowed)
                            data.getKit().outfitPlayer(p, data);
                    }
                }
            });
        });
        icons.add(slot -> {
            ItemBuilder icon = ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eReturn to Admin Gui"));
            return icon.asGuiItem(e -> {
                e.getWhoClicked().closeInventory();
                new AdminGui(e.getWhoClicked());
            });
        });

        pluginGui.setIcons(icons);
        pluginGui.open(player);
    }

    public void sendKitsAdminGui(HumanEntity player) {
        List<BaseIcon> icons = new ArrayList<>();

        for (Kit kit : KitManager.getInstance().getLoadedKits()) {
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
        new BaseAdminGui(player, "&0Kit Configurations", icons);
    }

    public void sendMapsAdminGui(HumanEntity player) {
        List<BaseIcon> icons = new ArrayList<>();

        for (MapData map : MapManager.getInstance().getMaps()) {
            icons.add(slot -> {
                ItemBuilder icon = ItemBuilder.from(map.getIcon()).name(Tools.componentalize("&e" + map.getMapName()));
                List<String> lore = new ArrayList<>();
                lore.add("&fCreator: &7" + map.getMapCreator());
                lore.addAll(Tools.wrapText("&fDescription: &2" + map.getMapDescription(), ChatColor.DARK_GREEN));
                lore.add("");
                lore.add("&fBorder: &7" + (map.isUseBorder() ? "yes" : "no"));
                if (map.isUseBorder())
                    lore.add("&fBorder Radius: &7" + map.getBorderRadius());
                lore.add("");
                lore.add("&bClick to edit this map configuration");
                lore.add("&bRight click to load and modify this world");


                icon.lore(Tools.componentalize(lore));
                return icon.asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    if (e.isLeftClick())
                        new ConfigureMapGui(player, this, map);
                    else {
                        MapManager.getInstance().createNewWorldAsync(map, world -> {
                            e.getWhoClicked().setCooldown(Material.AIR, 2);
                            e.getWhoClicked().teleport(world.getSpawnLocation());
                            e.getWhoClicked().sendMessage(Tools.formatInstructions("&fThis world has been loaded for editing. Any changes to this map can be saved to the map template by typing &e/savemap&f at any time. When you are finished, type &e/quit &fto unload this world and return to the main world.", ""));
                        });
                    }
                });
            });
        }
        new BaseAdminGui(player, "&0Map Configurations", icons);
    }

    public void sendGameAdminGui(HumanEntity player) {
        Set<BaseIcon> icons = new HashSet<>();

        for (GameOptions game : GameManager.getInstance().getAllConfigs())
            icons.add(slot -> {
                ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize("&eGame: &f" + game.getConfigFile().getName().split("\\.")[0]));
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
                        new ConfigureGameGui(e.getWhoClicked(), game, this);
                    } else {
                        Game.createNewGameWithCallback(game.getMap(), game, newGame -> {
                            new SelectGameGui(e.getWhoClicked());
                        });
                    }
                });
            });
        new BaseAdminGui(player, "&0Game Configurations", icons);
    }

    private static class BaseAdminGui extends Gui {
        Map<Integer, BaseIcon> guiIcons = new HashMap<>();
        public BaseAdminGui(HumanEntity player, String title, Collection<BaseIcon> icons) {
            super(1, Tools.componentalize("&0" + title));
            disableAllInteractions();
            setOutsideClickAction(e -> {
                e.getWhoClicked().closeInventory();
                new AdminGui(player);
            });

            int slot = 0;
            if (icons != null) {
                for (BaseIcon icon : icons) {
                    this.setItem(slot, icon.createIcon(slot));
                    guiIcons.put(slot, icon);
                    slot++;
                }
            }
            if (slot > 0)
                this.open(player);
        }

        public void updateIcon(int slot) {
            this.updateItem(slot, guiIcons.get(slot).createIcon(slot));
        }

        public void setIcons(Collection<BaseIcon> icons) {
            int slot = 0;
            for (BaseIcon icon : icons) {
                this.setItem(slot, icon.createIcon(slot));
                guiIcons.put(slot, icon);
                slot++;
            }
        }
    }

    private interface BaseIcon {
        GuiItem createIcon(int slot);
    }
}
