package me.cheracc.battlegameshungerroyale.guis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.GameOptions;
import me.cheracc.battlegameshungerroyale.types.MapData;
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
        super(1, Trans.late("Configure Game"), new HashSet<>(Arrays.asList(InteractionModifier.values())));
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
        icon = icon.name(Trans.lateToComponent("&eMap: &7%s", options.getMap().getMapName()));

        List<Component> lore = new ArrayList<>();

        lore.add(Component.space());
        lore.add(Trans.lateToComponent("&bClick to select a new map"));

        icon = icon.lore(lore);

        return icon.asGuiItem(e -> {
                e.getWhoClicked().closeInventory();
                sendSelectMapsGui(e.getWhoClicked());
        });
    }

    private GuiItem timersIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.CLOCK);
        icon = icon.name(Trans.lateToComponent("&eCurrent Phase Timers:"));

        List<String> lore = new ArrayList<>();
        lore.add(Trans.late("Pregame: &7") + Tools.secondsToMinutesAndSeconds(options.getPregameTime()));
        lore.add(Trans.late("Invincibility: &7") + Tools.secondsToMinutesAndSeconds(options.getInvincibilityTime()));
        lore.add(Trans.late("Main Phase: &7") + Tools.secondsToMinutesAndSeconds(options.getMainPhaseTime()));
        lore.add(Trans.late("Border Phase: &7") + Tools.secondsToMinutesAndSeconds(options.getBorderTime()));
        lore.add(Trans.late("Postgame: &7") + Tools.secondsToMinutesAndSeconds(options.getPostGameTime()));
        lore.add("");
        lore.add(Trans.late("&bClick to modify"));

        icon = icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendTimersGui(e.getWhoClicked());
        });
    }

    private GuiItem livesIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.TOTEM_OF_UNDYING).name(Trans.lateToComponent("&eLives per Player: &f%s", options.getLivesPerPlayer()));
        icon = icon.lore(Component.space(), Trans.lateToComponent("&bClick to increase"), Trans.lateToComponent("&bRight click to decrease"));

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
        ItemBuilder icon = ItemBuilder.from(Material.WOODEN_PICKAXE).name(Trans.lateToComponent("&eAllow Regular Building: &f%s", options.isAllowRegularBuilding()));
        icon.flags(ItemFlag.HIDE_ATTRIBUTES);
        icon = icon.lore(Component.space(), Trans.lateToComponent("&bClick to toggle"));

        return icon.asGuiItem(e -> {
            options.setAllowRegularBuilding(!options.isAllowRegularBuilding());
            updateItem(e.getSlot(), allowBuildingIcon());
        });
    }

    private GuiItem playersNeededIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.PLAYER_HEAD).name(Trans.lateToComponent("&ePlayers needed to start: &f%s", options.getPlayersNeededToStart()));
        icon = icon.lore(Component.space(), Trans.lateToComponent("&bClick to increase"), Trans.lateToComponent("&bRight click to decrease"));

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
        ItemBuilder icon = ItemBuilder.from(mat).name(Trans.lateToComponent("&eStart Type: &f%s", options.getStartType().name().toLowerCase()));
        List<String> lore = new ArrayList<>();
        if (options.getStartType() == GameOptions.StartType.ELYTRA)
            lore.addAll(Tools.wrapText(Trans.late("&7At the start of the game, players will be teleported to central spawn and then launched into the air to glide back down using a (temporary) provided elytra. The elytra will be removed at the end of the invincibility phase or when the player touches the ground."), ChatColor.GRAY));
        else
            lore.addAll(Tools.wrapText(Trans.late("&7At the start of the game, players are teleported to spawn points equally distant from the center spawn. If spawn point blocks are set, each player will spawn on one of the spawn point blocks - otherwise they will be evenly spaced around the center."), ChatColor.GRAY));

        lore.add("");
        lore.add(Trans.late("&bClick to change"));
        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            options.toggleStartType();
            updateItem(5, spawnTypeIcon());
        });
    }

    private GuiItem saveConfigIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.WRITABLE_BOOK).name(Trans.lateToComponent("&eClick Here to SAVE this Configuration"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();

            if (options.getConfigFile() == null) {
                e.getWhoClicked().sendMessage(Tools.formatInstructions(Trans.late("Enter a name for this configuration. If you enter an existing configuration name, the old configuration will be overwritten."),
                        options.getConfigFile() == null ? "" : options.getConfigFile().getName().split("\\.")[0]));
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), filename -> {
                    if (filename.matches("[^-_.A-Za-z0-9]")) {
                        e.getWhoClicked().sendMessage(Trans.lateToComponent("Config names may not contain spaces or other odd characters"));
                        open(e.getWhoClicked());
                        return;
                    }
                    options.saveConfig(filename);
                    e.getWhoClicked().sendMessage(Trans.lateToComponent("&eSaved game configuration to %s%s", filename, ".yml"));
                    new ConfigureGameGui(e.getWhoClicked(), options, new AdminGui(e.getWhoClicked()));
                });
            } else {
                options.saveConfig(options.getConfigFile().getName());
                e.getWhoClicked().sendMessage(Trans.lateToComponent("&eSaved game configuration to %s", options.getConfigFile().getName()));
                new ConfigureGameGui(e.getWhoClicked(), options, new AdminGui(e.getWhoClicked()));
            }
        });
    }

    private GuiItem startGameIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Trans.lateToComponent("&eClick Here to Start a New Game with this Configuration"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            Game.createNewGameWithCallback(options, GameManager.getInstance().getPlugin(), game -> new SelectGameGui(e.getWhoClicked()));
        });

    }

    private GuiItem lootIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.CHEST).name(Trans.lateToComponent("&eLoot Settings:"));
        List<String> lore = new ArrayList<>();

        lore.add(Trans.late("&fRandom Chests: &7") + (options.isGenerateChests() ? Trans.late("on") : Trans.late("off")));
        lore.add(Trans.late("&fChest Respawn Time: &7") + options.getChestRespawnTime());
        lore.add(Trans.late("&fMax Chests per Chunk: &7") + options.getMaxChestsPerChunk());
        lore.add(Trans.late("&fLoot Table: &7") + options.getLootTable().getKey().getKey());
        lore.add("");
        lore.add(Trans.late("&bClick to modify loot settings"));
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
            player.sendMessage(Trans.lateToComponent("There are no saved configuration files"));
            fillGui();
            open(player);
        }

        int rows = configFiles.size() / 9 + 1;
        Gui gui = Gui.gui().rows(rows).title(Trans.lateToComponent("Select a saved config:")).create();

        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        for (File file : configFiles) {
            gui.addItem(ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(Tools.componentalize(file.getName().split("\\.")[0]))
                .lore(Trans.lateToComponent("&bClick to start a game using this config"),
                    Trans.lateToComponent("&bRight click to view or modify it")).asGuiItem(e -> {
                        e.getWhoClicked().closeInventory();
                        options.loadConfig(file);
                        new ConfigureGameGui(e.getWhoClicked(), options, this);
            }));
        }

        gui.addItem(ItemBuilder.from(Material.ENCHANTED_GOLDEN_APPLE).name(Trans.lateToComponent("&eCreate New Configuration")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new ConfigureGameGui(e.getWhoClicked(), options, this);
        }));

        gui.open(player);
    }

    private void sendSelectMapsGui(HumanEntity player) {
        List<MapData> maps = MapManager.getInstance().getMaps();
        int rows = maps.size() / 9 + 1;

        Gui gui = Gui.gui().rows(rows).title(Trans.lateToComponent("&0Select a map for this Game")).create();

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

        String[] phases = { Trans.late("Pregame"), Trans.late("Invincibility"), Trans.late("Main Phase"), Trans.late("Border Shrinking"), Trans.late("Postgame") };
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
        icon.lore(Component.space(), Trans.lateToComponent("&bClick to increase"), Trans.lateToComponent("&bRight click to decrease"));

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
        Gui gui = Gui.gui().rows(1).title(Trans.lateToComponent("&0Configure Phase Timers")).create();

        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            updateItem(1, timersIcon());
            open(e.getWhoClicked());
        });

        for (int i = 0; i < 5; i++) {
            gui.setItem(i, timerIcon(gui, i));
        }

        gui.setItem(8, ItemBuilder.from(Material.WRITABLE_BOOK).name(Trans.lateToComponent("&eSave")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            updateItem(1, timersIcon());
            open(e.getWhoClicked());
        }));

        gui.open(player);
    }

    private GuiItem mapInfoIcon(MapData map) {
        ItemBuilder icon = ItemBuilder.from(map.getIcon()).name(Tools.componentalize("&e" + map.getMapName()));

        List<String> lore = new ArrayList<>();
        lore.add(Trans.late("&fCreator: &7") + map.getMapCreator());
        lore.addAll(Tools.wrapText(Trans.late("&fDescription: &7") + map.getMapDescription(), ChatColor.GRAY));
        lore.add("");
        lore.add(Trans.late("&bClick to select this map"));

        icon = icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem();
    }

}
