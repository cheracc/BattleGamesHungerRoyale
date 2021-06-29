package me.stipe.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Game;
import me.stipe.battlegameshungerroyale.datatypes.GameOptions;
import me.stipe.battlegameshungerroyale.datatypes.MapData;
import me.stipe.battlegameshungerroyale.managers.GameManager;
import me.stipe.battlegameshungerroyale.managers.MapManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigureGameGui extends Gui {
    private final GameOptions options;

    public ConfigureGameGui(HumanEntity player) {
        super(1, Tools.componentalize("&0Configure Game"));
        options = new GameOptions();
        disableAllInteractions();
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        fillGui();
        open(player);
    }

    private void fillGui() {
        setItem(0,mapsIcon());
        setItem(1,timersIcon());
        setItem(2,livesIcon());
        setItem(3,playersNeededIcon());
        setItem(4,allowBuildingIcon());

        setItem(6, loadConfigIcon());
        setItem(7, saveConfigIcon());
        setItem(8, startGameIcon());
    }

    private void updateAll() {
        updateItem(0, mapsIcon());
        updateItem(1, timersIcon());
        updateItem(2, livesIcon());
        updateItem(3, playersNeededIcon());
        updateItem(4, allowBuildingIcon());
    }

    private GuiItem mapsIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.FILLED_MAP);
        icon = icon.name(Tools.componentalize("&eEligible Maps:"));

        List<Component> lore = new ArrayList<>();

        if (options.getMaps().isEmpty())
            lore.add(Tools.componentalize("None!"));
        else {
            for (MapData map : options.getMaps())
                lore.add(Tools.componentalize(map.getMapName()));
        }
        lore.add(Component.space());
        lore.add(Tools.componentalize("&bClick to add maps to this list"));
        lore.add(Tools.componentalize("&bRight click to clear"));

        icon = icon.lore(lore);

        return icon.asGuiItem(e -> {
            if (e.isLeftClick()) {
                e.getWhoClicked().closeInventory();
                sendSelectMapsGui(e.getWhoClicked());
            }
            if (e.isRightClick()) {
                options.clearMaps();
            }
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
        ItemBuilder icon = ItemBuilder.from(Material.WOODEN_AXE).name(Tools.componentalize("&eAllow Regular Building: &f" + options.isAllowRegularBuilding()));
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

    private GuiItem loadConfigIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.ENDER_CHEST).name(Tools.componentalize("&eClick Here to LOAD a Saved Configuration"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendSelectConfigGui(e.getWhoClicked());
        });
    }

    private GuiItem saveConfigIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.SHULKER_BOX).name(Tools.componentalize("&eClick Here to SAVE this Configuration"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), filename -> {
                options.saveConfig(filename);
                e.getWhoClicked().sendMessage(Tools.componentalize("&eSaved game configuration to " + filename + ".yml"));
                open(e.getWhoClicked());
            });
        });
    }

    private GuiItem startGameIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize("&eClick Here to Start a New Game with this Configuration"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            sendStartGameGui(e.getWhoClicked());
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
            open(player);
        }

        int rows = configFiles.size() / 9 + 1;
        Gui gui = Gui.gui().rows(rows).title(Tools.componentalize("&0Select a configuration file:")).create();

        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            open(e.getWhoClicked());
        });

        for (File file : configFiles) {
            gui.addItem(ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(Tools.componentalize(file.getName()))
                    .lore(Tools.componentalize("&bClick to load this file")).asGuiItem(e -> {
                e.getWhoClicked().closeInventory();
                options.loadConfig(file);
                updateAll();
                open(e.getWhoClicked());
            }));
        }
        gui.open(player);
    }

    private void sendSelectMapsGui(HumanEntity player) {
        List<MapData> maps = MapManager.getInstance().getMaps();
        int rows = (maps.size() - options.getMaps().size()) / 9 + 1;

        Gui gui = Gui.gui().rows(rows).title(Tools.componentalize("&0Add a Map to this Game")).create();

        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            open(e.getWhoClicked());
        });

        for (MapData map : maps) {
            if (!options.getMaps().contains(map)) {
                GuiItem icon = mapInfoIcon(map);
                icon.setAction(e -> {
                    options.addMap(map);
                    e.getWhoClicked().closeInventory();
                    updateItem(0, mapsIcon());
                    open(e.getWhoClicked());
                });
                gui.addItem(icon);
            }
        }
        gui.open(player);
    }

    private interface SetTimerValue {
        void set(int value);
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
        SetTimerValue[] updates = {
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

            updates[record].set(value);
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

    private void sendStartGameGui(HumanEntity player) {
        int rows = options.getMaps().size() / 9 + 1;
        Gui gui = Gui.gui().rows(rows).title(Tools.componentalize("&0Select a Map for This Game:")).create();
        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            open(e.getWhoClicked());
        });

        for (MapData map : options.getMaps()) {
            GuiItem icon = mapInfoIcon(map);
            icon.setAction(e -> {
                e.getWhoClicked().closeInventory();
                Game game = new Game(map, options);

                GameManager.getInstance().setupGame(game);
            });
            gui.addItem(icon);
        }
        gui.open(player);
    }
}
