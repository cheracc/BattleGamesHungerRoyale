package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.GameOptions;
import me.cheracc.battlegameshungerroyale.types.MapData;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.IntConsumer;

public class ConfigureGameGui extends Gui {
    private final GameOptions options;

    public ConfigureGameGui(HumanEntity player, GameOptions options, Gui sendingGui) {
        super(1, "Configure Game", new HashSet<>(Arrays.asList(InteractionModifier.values())));
        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            sendingGui.open(e.getWhoClicked());
        });

        if (options == null) {
            this.options = new GameOptions();
            sendSelectConfigGui(player);
            return;
        }
        this.options = options;

        fillGui();
        open(player);
    }

    private void fillGui() {
        setItem(0, mapsIcon());
        setItem(1, timersIcon());
        setItem(2, livesIcon());
        setItem(3, playersNeededIcon());
        setItem(4, allowBuildingIcon());
        setItem(5, spawnTypeIcon());
        setItem(6, lootIcon());

        setItem(7, saveConfigIcon());
        setItem(8, startGameIcon());
    }

    private GuiItem mapsIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.FILLED_MAP);
        icon = icon.name(Tools.componentalize("&eMap: &7" + options.getMap().getMapName()));

        List<Component> lore = new ArrayList<>();

        lore.add(Component.space());
        lore.add(Tools.componentalize("&bClick to select a new map"));

        icon = icon.lore(lore);

        return icon.asGuiItem(e -> {
                e.getWhoClicked().closeInventory();
                sendSelectMapsGui(e.getWhoClicked());
        });
    }

    private GuiItem timersIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.CLOCK);
        icon = icon.name(Tools.componentalize("&eCurrent Phase Timers:"));

        List<String> lore = new ArrayList<>();
        lore.add("Pregame: &7" + Tools.secondsToMinutesAndSeconds(options.getPregameTime()));
        lore.add("Invincibility: &7" + Tools.secondsToMinutesAndSeconds(options.getInvincibilityTime()));
        lore.add("Main Phase: &7" + Tools.secondsToMinutesAndSeconds(options.getMainPhaseTime()));
        lore.add("Border Phase: &7" + Tools.secondsToMinutesAndSeconds(options.getBorderTime()));
        lore.add("Postgame: &7" + Tools.secondsToMinutesAndSeconds(options.getPostGameTime()));
        lore.add("");
        lore.add("&bClick to modify");

        icon = icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendTimersGui(e.getWhoClicked());
        });
    }

    private GuiItem livesIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.TOTEM_OF_UNDYING).name(Tools.componentalize("&eLives per Player: &f" + options.getLivesPerPlayer()));
        icon = icon.lore(Component.space(), Tools.componentalize("&bClick to increase"), Tools.componentalize("&bRight click to decrease"));

        return icon.asGuiItem(e -> {
            int lives = options.getLivesPerPlayer();

            if (e.isLeftClick() && lives < 5)
                options.setLivesPerPlayer(lives + 1);
            else if (e.isRightClick() && lives > 1)
                options.setLivesPerPlayer(lives - 1);

            updateItem(e.getSlot(), livesIcon());
        });
    }

    private GuiItem allowBuildingIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.WOODEN_PICKAXE).name(Tools.componentalize("&eAllow Regular Building: &f" + options.isAllowRegularBuilding()));
        icon.flags(ItemFlag.HIDE_ATTRIBUTES);
        icon = icon.lore(Component.space(), Tools.componentalize("&bClick to toggle"));

        return icon.asGuiItem(e -> {
            options.setAllowRegularBuilding(!options.isAllowRegularBuilding());
            updateItem(e.getSlot(), allowBuildingIcon());
        });
    }

    private GuiItem playersNeededIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.PLAYER_HEAD).name(Tools.componentalize("&ePlayers needed to start: &f" + options.getPlayersNeededToStart()));
        icon = icon.lore(Component.space(), Tools.componentalize("&bClick to increase"), Tools.componentalize("&bRight click to decrease"));

        return icon.asGuiItem(e -> {
            int needed = options.getPlayersNeededToStart();

            if (e.isLeftClick() && needed < 50)
                options.setPlayersNeededToStart(needed + 1);
            else if (e.isRightClick() && needed > 2)
                options.setPlayersNeededToStart(needed - 1);

            updateItem(e.getSlot(), playersNeededIcon());
        });
    }

    private GuiItem spawnTypeIcon() {
        Material mat = options.getStartType() == GameOptions.StartType.ELYTRA ? Material.ELYTRA : Material.CHEST;
        ItemBuilder icon = ItemBuilder.from(mat).name(Tools.componentalize("&eStart Type: &f" + options.getStartType().name().toLowerCase()));
        List<String> lore = new ArrayList<>();
        if (options.getStartType() == GameOptions.StartType.ELYTRA)
            lore.addAll(Tools.wrapText("&7At the start of the game, players will be teleported to central spawn and then launched into the air to glide back down using a (temporary) provided elytra. The elytra will be removed at the end of the invincibility phase or when the player touches the ground.", ChatColor.GRAY));
        else
            lore.addAll(Tools.wrapText("&7At the start of the game, players are teleported to spawn points equally distant from the center spawn. If spawn point blocks are set, each player will spawn on one of the spawn point blocks - otherwise they will be evenly spaced around the center.", ChatColor.GRAY));

        lore.add("");
        lore.add("&bClick to change");
        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            options.toggleStartType();
            updateItem(5, spawnTypeIcon());
        });
    }

    private GuiItem saveConfigIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eClick Here to SAVE this Configuration"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();

            if (options.getConfigFile() == null) {
                e.getWhoClicked().sendMessage(Tools.formatInstructions("Enter a name for this configuration. If you enter an existing configuration name, the old configuration will be overwritten.",
                        options.getConfigFile() == null ? "" : options.getConfigFile().getName().split("\\.")[0]));
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), filename -> {
                    if (filename.matches("[^-_.A-Za-z0-9]")) {
                        e.getWhoClicked().sendMessage(Tools.componentalize("Config names may not contain spaces or other odd characters"));
                        open(e.getWhoClicked());
                        return;
                    }
                    options.saveConfig(filename);
                    e.getWhoClicked().sendMessage(Tools.componentalize("&eSaved game configuration to " + filename + ".yml"));
                    new ConfigureGameGui(e.getWhoClicked(), options, new AdminGui(e.getWhoClicked()));
                });
            } else {
                options.saveConfig(options.getConfigFile().getName());
                e.getWhoClicked().sendMessage(Tools.componentalize("&eSaved game configuration to " + options.getConfigFile().getName()));
                new ConfigureGameGui(e.getWhoClicked(), options, new AdminGui(e.getWhoClicked()));
            }
        });
    }

    private GuiItem startGameIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize("&eClick Here to Start a New Game with this Configuration"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            Game.createNewGameWithCallback(options.getMap(), options, game -> new SelectGameGui(e.getWhoClicked()));
        });

    }

    private GuiItem lootIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.CHEST).name(Tools.componentalize("&eLoot Settings:"));
        List<String> lore = new ArrayList<>();

        lore.add("&fRandom Chests: &7" + (options.isGenerateChests() ? "on" : "off"));
        lore.add("&fChest Respawn Time: &7" + options.getChestRespawnTime());
        lore.add("&fMax Chests per Chunk: &7" + options.getMaxChestsPerChunk());
        lore.add("&fLoot Table: &7" + options.getLootTable().getKey().getKey());
        lore.add("");
        lore.add("&bClick to modify loot settings");
        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
           e.getWhoClicked().closeInventory();
           new ConfigureLootGui(e.getWhoClicked(), this, options);
        });

    }

    private void sendSelectConfigGui(HumanEntity player) {
        File configDir = new File(BGHR.getPlugin().getDataFolder(), "gameconfigs/");
        List<File> configFiles = new ArrayList<>();

        for (File file : configDir.listFiles()) {
            if (file.exists() && file.getName().contains(".yml"))
                configFiles.add(file);
        }

        if (configFiles.isEmpty()) {
            player.sendMessage(Tools.componentalize("There are no saved configuration files"));
            fillGui();
            open(player);
        }

        int rows = configFiles.size() / 9 + 1;
        Gui gui = Gui.gui().rows(rows).title(Tools.componentalize("Select a saved config:")).create();

        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        for (File file : configFiles) {
            gui.addItem(ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(Tools.componentalize(file.getName().split("\\.")[0]))
                .lore(Tools.componentalize("&bClick to start a game using this config"),
                    Tools.componentalize("&bRight click to view or modify it")).asGuiItem(e -> {
                        e.getWhoClicked().closeInventory();
                        options.loadConfig(file);
                        new ConfigureGameGui(e.getWhoClicked(), options, this);
            }));
        }

        gui.addItem(ItemBuilder.from(Material.ENCHANTED_GOLDEN_APPLE).name(Tools.componentalize("&eCreate New Configuration")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new ConfigureGameGui(e.getWhoClicked(), options, this);
        }));

        gui.open(player);
    }

    private void sendSelectMapsGui(HumanEntity player) {
        List<MapData> maps = MapManager.getInstance().getMaps();
        int rows = maps.size() / 9 + 1;

        Gui gui = Gui.gui().rows(rows).title(Tools.componentalize("&0Select a map for this Game")).create();

        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            open(e.getWhoClicked());
        });

        for (MapData map : maps) {
            GuiItem icon = mapInfoIcon(map);
            icon.setAction(e -> {
                options.setMap(map);
                e.getWhoClicked().closeInventory();
                new ConfigureGameGui(e.getWhoClicked(), options, this);
            });
            gui.addItem(icon);
        }
        gui.open(player);
    }

    private GuiItem timerIcon(Gui gui, int record) {
        if (record < 0 || record > 4)
            return null;

        String[] phases = { "Pregame", "Invincibility", "Main Phase", "Border Shrinking", "Postgame" };
        int[] values = {
               options.getPregameTime(),
               options.getInvincibilityTime(),
               options.getMainPhaseTime(),
               options.getBorderTime(),
               options.getPostGameTime()
        };
        IntConsumer[] updates = {
                options::setPregameTime,
                options::setInvincibilityTime,
                options::setMainPhaseTime,
                options::setBorderTime,
                options::setPostGameTime
        };

        ItemBuilder icon = ItemBuilder.from(Material.CLOCK).name(Tools.componentalize("&e" + phases[record] + ": &f" + Tools.secondsToMinutesAndSeconds(values[record])));
        icon.lore(Component.space(), Tools.componentalize("&bClick to increase"), Tools.componentalize("&bRight click to decrease"));

        return icon.asGuiItem(e -> {
            int value = values[record];
            int change = 5;

            if (e.isShiftClick())
                change = 30;

            if (e.isLeftClick())
                value += change;
            if (e.isRightClick() && value > 10 + change)
                value -= change;

            updates[record].accept(value);
            gui.updateItem(record, timerIcon(gui, record));
        });
    }

    private void sendTimersGui(HumanEntity player) {
        Gui gui = Gui.gui().rows(1).title(Tools.componentalize("&0Configure Phase Timers")).create();

        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            updateItem(1, timersIcon());
            open(e.getWhoClicked());
        });

        for (int i = 0; i < 5; i++) {
            gui.setItem(i, timerIcon(gui, i));
        }

        gui.setItem(8, ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            updateItem(1, timersIcon());
            open(e.getWhoClicked());
        }));

        gui.open(player);
    }

    private GuiItem mapInfoIcon(MapData map) {
        ItemBuilder icon = ItemBuilder.from(map.getIcon()).name(Tools.componentalize("&e" + map.getMapName()));

        List<String> lore = new ArrayList<>();
        lore.add("&fCreator: &7" + map.getMapCreator());
        lore.addAll(Tools.wrapText("&fDescription: &7" + map.getMapDescription(), ChatColor.GRAY));
        lore.add("");
        lore.add("&bClick to select this map");

        icon = icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem();
    }

}
