package me.cheracc.battlegameshungerroyale.guis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.*;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.*;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class AdminGui extends Gui {

    public AdminGui(HumanEntity player) {
        super(1, Trans.late("Admin Menu"), new HashSet<>(Arrays.asList(InteractionModifier.values())));
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
        icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("&7  Create or modify kits and their abilities"), ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendKitsAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem mapsIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.FILLED_MAP).name(Tools.componentalize(Trans.late("&eManage Maps")));
        icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("&7  View and modify the configuration for the available game maps, or load a map for editing (like placing chests or fixing spots where players get stuck)"), ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendMapsAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem gamesIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize(Trans.late("&eManage Game Configurations")));
        icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("&7  View, modify, or create new game configurations, timers, and rules."), ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendGameAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem mainConfigIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(Tools.componentalize(Trans.late("&eManage Plugin")));
        icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("&7  General Settings that effect how the plugin is loaded and what it does."), ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendPluginAdminGui(e.getWhoClicked());
        });
    }

    private GuiItem exitIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.BARRIER).name(Tools.componentalize(Trans.late("Close Menu")));

        return icon.asGuiItem(e -> e.getWhoClicked().closeInventory());
    }

    public void sendPluginAdminGui(HumanEntity player) {
        BGHR plugin = BGHR.getPlugin();
        FileConfiguration config = plugin.getConfig();
        List<BaseIcon> icons = new ArrayList<>();
        BaseAdminGui pluginGui = new BaseAdminGui(player, Trans.late("Plugin Configuration"), 1);

        icons.add(slot -> {
            boolean value = config.getBoolean("use mysql instead of h2", false);
            ItemBuilder icon = ItemBuilder.from(Material.NETHER_STAR).name(Tools.componentalize(Trans.late("&eUse Database: &f") +
                    (value ? "MySQL" : "Disk (H2)")));
            List<String> lore = new ArrayList<>(Tools.wrapText(Trans.late("  &7Which database to use for plugin and player data."), ChatColor.GRAY));
            lore.add("");
            if (value) {
                lore.add(Trans.late(""));
                if (DatabaseManager.get().isUsingMySql())
                    lore.add(Trans.late("&aMySQL is currently configured and connected"));
                else
                    lore.add(Trans.late("&cMySQL must be configured in config.yml."));
            }

            icon.lore(Tools.componentalize(lore));

            return icon.asGuiItem(e -> {
                if (e.isRightClick()) {
                    giveMysqlSettingsBook(e.getWhoClicked());
                    return;
                }
                    config.set("use mysql instead of h2", !value);
                    plugin.saveConfig();
                    pluginGui.updateIcon(slot);
            });
        });
        icons.add(slot -> {
            String current = Bukkit.getDefaultGameMode().name().toLowerCase();
            GameMode value = GameMode.valueOf(config.getString("main world.gamemode", current).toUpperCase());
            ItemBuilder icon = ItemBuilder.from(Material.WOODEN_PICKAXE).name(Tools.componentalize(Trans.late("&eDefault Game Mode: &f") +
                    value.name().toLowerCase()));
            icon.flags(ItemFlag.HIDE_ATTRIBUTES);
            icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7Sets the default game mode for players in the main world. If using a waiting lobby and you do not want players to build in it, choose 'adventure'"), ChatColor.GRAY)));

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
            boolean value = config.getBoolean("main world.place players at spawn on join", false);
            ItemBuilder icon = ItemBuilder.from(Material.BEACON).name(Tools.componentalize(Trans.late("&eAlways Spawn Players at Main Spawn: &f") +
                    (value ? Trans.late("no") : Trans.late("yes"))));
            icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7Whether players will always be sent to the spawn location when joining the server or teleporting back to the main world. If disabled, players will instead return to their last recorded location in the main world."), ChatColor.GRAY)));

            return icon.asGuiItem(e -> {
                config.set("main world.place players at spawn on join", !value);
                plugin.saveConfig();
                pluginGui.updateIcon(slot);
            });
        });
        icons.add(slot -> {
            boolean value = config.getBoolean("main world.kits useable in main world", false);
            ItemBuilder icon = ItemBuilder.from(Material.STONE_SWORD).name(Tools.componentalize(Trans.late("&eAllow Kits to be used in main world: &f") +
                    (value ? Trans.late("yes") : Trans.late("no"))));
            icon.flags(ItemFlag.HIDE_ATTRIBUTES);
            icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7Whether kits and kit abilities may be used in the main world. If disabled, players can still select a kit to use, but the kit will not be equipped on them until they join a game."), ChatColor.GRAY)));

            return icon.asGuiItem(e -> {
                config.set("main world.kits useable in main world", !value);
                plugin.saveConfig();
                pluginGui.updateIcon(slot);
                boolean kitsAllowed = !value;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!GameManager.getInstance().isInAGame(p)) {
                        PlayerData data = PlayerManager.getInstance().getPlayerData(p);
                        if (data.getKit() != null && !kitsAllowed)
                            data.getKit().disrobePlayer(p);
                        else if (data.getKit() != null)
                            data.getKit().outfitPlayer(p);
                    }
                }
            });
        });
        icons.add(slot -> {
            boolean current = config.getBoolean("auto-update", true);
            ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize(Trans.late("&eAuto-Update Plugin: &f") +
                    (current ? Trans.late("yes") : Trans.late("no"))));
            icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7Turns the plugin auto-updater on or off. Plugin updates are installed when the server restarts."), ChatColor.GRAY)));

            return icon.asGuiItem(e -> {
                config.set("auto-update", !current);
                plugin.saveConfig();
                pluginGui.updateIcon(slot);
            });
        });
        icons.add(slot -> {
            boolean current = config.getBoolean("show main scoreboard", true);
            ItemBuilder icon = ItemBuilder.from(Material.PAINTING).name(Tools.componentalize(Trans.late("&eShow Main Scoreboard: &f") +
                    (current ? Trans.late("yes") : Trans.late("no"))));
            icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7Disable if you use another scoreboard plugin. If enabled, players can still disable it individually with /settings"), ChatColor.GRAY)));

            return icon.asGuiItem(e -> {
                boolean newValue = !current;
                config.set("show main scoreboard", !current);
                plugin.saveConfig();
                pluginGui.updateIcon(slot);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!GameManager.getInstance().isInAGame(p))
                        if (newValue)
                            p.setScoreboard(GameManager.getInstance().getMainScoreboard());
                        else
                            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            });
        });

        pluginGui.setIcons(icons);
        player.closeInventory();
        pluginGui.open(player);
    }

    public void sendKitsAdminGui(HumanEntity player) {
        List<BaseIcon> icons = new ArrayList<>();
        List<Kit> kits = KitManager.getInstance().getLoadedKits(true);
        BaseAdminGui kitsAdminGui = new BaseAdminGui(player, Trans.late("Kits"), kits.size() / 9 + 1);

        for (Kit kit : kits) {
            icons.add(slot -> {
                ItemBuilder icon = ItemBuilder.from(kit.getIcon()).name(Tools.componentalize(kit.getName() + (kit.isEnabled() ? "" : Trans.late(" &4[&6DISABLED&4]"))));
                icon.flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                List<Component> lore = new ArrayList<>(Tools.componentalize(Tools.wrapText(kit.getDescription(), ChatColor.GRAY)));
                lore.add(0, Component.text(""));
                lore.add(Tools.BLANK_LINE);
                if (!kit.getAbilities().isEmpty()) {
                    lore.add(Tools.componentalize(Trans.late("&eKit Abilities:")));
                    for (Ability a : kit.getAbilities()) {
                        String color = (a instanceof ActiveAbility) ? "&a" : "&6";
                        String abilityString = String.format("%s%s &f- &7%s", color, a.getCustomName() != null ? a.getCustomName() : a.getName(),
                                a.getDescription());
                        lore.addAll(Tools.componentalize(Tools.wrapText(abilityString, ChatColor.GRAY)));
                    }
                }
                lore.addAll(kit.getEquipment().getDescription());

                lore.add(Tools.BLANK_LINE);
                lore.add(Tools.componentalize(Trans.late("&bClick here to modify this kit")));
                if (kit.isEnabled())
                    lore.add(Tools.componentalize(Trans.late("&cShift+Click to disable this kit")));
                else
                    lore.add(Tools.componentalize(Trans.late("&aShift+Click to enable this kit")));
                icon.lore(lore);
                return icon.asGuiItem(e -> {
                    if (!e.isShiftClick()) {
                        e.getWhoClicked().closeInventory();
                        new ConfigureKitGui(kit, this, player);
                    } else {
                        kit.setEnabled(!kit.isEnabled());
                        kit.saveConfig();
                        kitsAdminGui.updateIcon(slot);
                    }
                });
            });
        }
        icons.add(slot -> {
            ItemBuilder icon = ItemBuilder.from(Material.ENCHANTED_GOLDEN_APPLE).name(Tools.componentalize(Trans.late("Create a New Kit")));
            icon.lore(Tools.componentalize(Trans.late("&bClick here to create a new kit")));
            return icon.asGuiItem(e -> {
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(Tools.formatInstructions(Trans.late("Enter a kit id for this kit. This id is just used by the plugin as an identifier and cannot be changed later. You can call it anything you want."), ""));
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), text -> new ConfigureKitGui(new Kit(text), this, player));
            });
        });

        kitsAdminGui.setIcons(icons);
        player.closeInventory();
        kitsAdminGui.open(player);
    }

    public void sendMapsAdminGui(HumanEntity player) {
        List<BaseIcon> icons = new ArrayList<>();

        for (MapData map : MapManager.getInstance().getMaps()) {
            icons.add(slot -> {
                ItemBuilder icon = ItemBuilder.from(map.getIcon()).name(Tools.componentalize("&e" + map.getMapName()));
                List<String> lore = new ArrayList<>();
                lore.add(Trans.late("&fCreator: &7") + map.getMapCreator());
                lore.addAll(Tools.wrapText(Trans.late("&fDescription: &2") + map.getMapDescription(), ChatColor.DARK_GREEN));
                lore.add("");
                lore.add(Trans.late("&fBorder: &7") + (map.isUseBorder() ? Trans.late("yes") : Trans.late("no")));
                if (map.isUseBorder())
                    lore.add(Trans.late("&fBorder Radius: &7") + map.getBorderRadius());
                lore.add("");
                lore.add(Trans.late("&bClick to edit this map configuration"));
                lore.add(Trans.late("&bRight click to load and modify this world"));


                icon.lore(Tools.componentalize(lore));
                return icon.asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    if (e.isLeftClick())
                        new ConfigureMapGui(player, this, map);
                    else {
                        MapManager.getInstance().createNewWorldAsync(map, world -> {
                            e.getWhoClicked().teleport(world.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                            e.getWhoClicked().sendMessage(Tools.formatInstructions(Trans.late("&fThis world has been loaded for editing. Any changes to this map can be saved to the map template by typing &e/savemap&f at any time. When you are finished, type &e/quit &fto unload this world and return to the main world."), ""));
                        });
                    }
                });
            });
        }
        BaseAdminGui mapsGui = new BaseAdminGui(player, Trans.late("Map Configurations"), icons.size() / 9 + 1);
        mapsGui.setIcons(icons);
        mapsGui.open(player);
    }

    public void sendGameAdminGui(HumanEntity player) {
        Set<BaseIcon> icons = new HashSet<>();

        for (GameOptions game : GameManager.getInstance().getAllConfigs())
            icons.add(slot -> {
                ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize(Trans.late("&eGame: &f") + game.getConfigFile().getName().split("\\.")[0]));
                List<String> lore = new ArrayList<>();
                lore.add(Trans.late("&fMap: &7") + game.getMap().getMapName());
                lore.add(Trans.late("&fPlayers needed to start: &7") + game.getPlayersNeededToStart());
                lore.add(Trans.late("&fStarting style: &7") + game.getStartType().name().toLowerCase());
                lore.add(Trans.late("&fRespawns per player: &7") + (game.getLivesPerPlayer() - 1));
                lore.add(Trans.late("&fFill pre-placed chests: &7") + (game.isFillAllChests() ?  Trans.late("yes") : Trans.late("no")));
                lore.add(Trans.late("&fRandomly place loot chests: &7") + (game.isGenerateChests() ? Trans.late("yes") : Trans.late("no")));
                lore.add(Trans.late("&fLoot chest respawn time: &7") + game.getChestRespawnTime() + (game.getChestRespawnTime() > 1 ? Trans.late(" minutes") : Trans.late(" minute")));
                lore.add(Trans.late("&fLoot table: &7") + game.getLootTable().getKey().getKey());
                lore.add("");
                lore.add(Trans.late("&bClick to start a new game with this configuration"));
                lore.add(Trans.late("&bRight click to edit this configuration"));

                icon.lore(Tools.componentalize(lore));

                return icon.asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    if (e.isRightClick()) {
                        new ConfigureGameGui(e.getWhoClicked(), game, this);
                    } else {
                        Game.createNewGameWithCallback(game, GameManager.getInstance().getPlugin(), newGame -> new SelectGameGui(e.getWhoClicked()));
                    }
                });
            });
        BaseAdminGui gameGui = new BaseAdminGui(player, Trans.late("Game Configurations"), icons.size() / 9 + 1);
        gameGui.setIcons(icons);
        gameGui.open(player);
    }

    private static class BaseAdminGui extends Gui {
        Map<Integer, BaseIcon> guiIcons = new HashMap<>();
        public BaseAdminGui(HumanEntity player, String title, int rows) {
            super(rows, title, new HashSet<>(Arrays.asList(InteractionModifier.values())));
            disableAllInteractions();
            setOutsideClickAction(e -> {
                e.getWhoClicked().closeInventory();
                new AdminGui(player);
            });

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

    private void giveMysqlSettingsBook(HumanEntity player) {
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        List<String> page = new ArrayList<>();


        page.add(Trans.late("&1&lInstructions:"));
        page.add("");
        page.add(Trans.late("&0On Page 2, put the following information on the first five lines:"));
        page.add("&5hostname");
        page.add("&5port");
        page.add("&5database");
        page.add("&5user");
        page.add("&5password");
        page.add("");
        page.add(Trans.late("&8When finished, sign the book with any title."));

        Component pageComponent = Component.empty();
        for (String s : page) {
            pageComponent = pageComponent.append(Tools.componentalize(s)).append(Component.newline());
        }

        meta.addPages(pageComponent);
        meta.getPersistentDataContainer().set(Tools.PLUGIN_KEY, PersistentDataType.STRING, "mysql");

        book.setItemMeta(meta);

        player.getInventory().addItem(book);
        player.sendMessage(Tools.formatInstructions(Trans.late("You have been given a writable book. Open the book to configure your MySQL Server settings. ") +
                Trans.late("When finished, &lsign the book &fwith the title '&emysql&f' to apply the settings."), ""));
    }
}
