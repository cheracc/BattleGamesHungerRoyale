package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.datatypes.GameOptions;
import me.cheracc.battlegameshungerroyale.managers.LootManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.loot.LootTable;

import java.util.List;

public class ConfigureLootGui extends Gui {
    private final GameOptions options;

    public ConfigureLootGui(HumanEntity player, Gui sendingGui, GameOptions options) {
        super(1, Tools.componentalize("&0Loot Settings"));
        this.options = options;
        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            sendingGui.open(player);
        });

        setItem(0, generateChestsIcon());
        setItem(1, fillChestsIcon());
        setItem(2, lenientSearchingIcon());
        setItem(3, densityIcon());
        setItem(4, respawnTimeIcon());
        setItem(5, lootTableIcon());
        setItem(8, saveIcon());

        open(player);
    }

    private GuiItem generateChestsIcon() {
        GuiItem genChestsIcon = ItemBuilder.from(Material.CHEST).name(Tools.componentalize("&eGenerate Random Chests: &f" +
                (options.isGenerateChests() ? "on" : "off"))).asGuiItem();
        genChestsIcon.setAction(e -> {
            options.toggleGenerateChests();
            updateItem(0, generateChestsIcon());
        });

        return genChestsIcon;
    }

    private GuiItem fillChestsIcon() {
        GuiItem fillChestsIcon = ItemBuilder.from(Material.CHEST_MINECART).name(Tools.componentalize("&eFill all chests: &f" +
                (options.isFillAllChests() ? "on" : "off"))).asGuiItem();
        fillChestsIcon.setAction(e -> {
            options.toggleFillAllChests();
            updateItem(1, fillChestsIcon());
        });

        return fillChestsIcon;
    }

    private GuiItem lenientSearchingIcon() {
        GuiItem loosenSearchIcon = ItemBuilder.from(Material.GRASS_BLOCK).name(Tools.componentalize("&eLoosen Chest Search Restrictions: &f" +
                (options.isLoosenSearchRestrictions() ? "on" : "off"))).asGuiItem();
        loosenSearchIcon.setAction(e -> {
            options.toggleLoosenSearchRestrictions();
            updateItem(2, lenientSearchingIcon());
        });

        return loosenSearchIcon;
    }

    private GuiItem densityIcon() {
        GuiItem icon = ItemBuilder.from(Material.PAINTING).name(Tools.componentalize("&eMax Chest Spawns Per Chunk: &f" + options.getMaxChestsPerChunk())).asGuiItem();
        icon.setAction(e -> {
            int value = options.getMaxChestsPerChunk();
            int change = 1;

            if (e.isShiftClick())
                change = 5;

            if (e.isLeftClick())
                value += change;
            if (e.isRightClick() && value > 1 + change)
                value -= change;

            options.setMaxChestsPerChunk(value);
            updateItem(3, densityIcon());
        });
        return icon;
    }

    private GuiItem respawnTimeIcon() {
        GuiItem icon = ItemBuilder.from(Material.BARREL).name(Tools.componentalize("&eTime Between Chest Respawns: &f" + options.getMaxChestsPerChunk())).asGuiItem();
        icon.setAction(e -> {
            int value = options.getMaxChestsPerChunk();
            int change = 1;

            if (e.isShiftClick())
                change = 5;

            if (e.isLeftClick())
                value += change;
            if (e.isRightClick() && value > 1 + change)
                value -= change;

            options.setMaxChestsPerChunk(value);
            updateItem(4, respawnTimeIcon());
        });

        return icon;
    }

    private GuiItem lootTableIcon() {
        ItemBuilder lootTableIcon = ItemBuilder.from(Material.ENDER_CHEST);
        String[] split = options.getLootTable().getKey().getKey().split("/");
        lootTableIcon.name(Tools.componentalize("&eLoot Table: &f" + split[split.length - 1]));

        lootTableIcon.lore(Tools.componentalize("&bClick to cycle through loot tables"), Tools.componentalize("&bRight click to cycle backwards"));
        List<LootTable> allLootTables = LootManager.getLootTables();

        GuiAction<InventoryClickEvent> action = (e -> {
            int index = 0;
            for (LootTable t : allLootTables) {
                if (t.equals(options.getLootTable())) {
                    index = allLootTables.indexOf(t);
                }
            }
            if (e.isLeftClick())
                index++;
            else
                index--;

            if (index < 0)
                index = allLootTables.size() - 1;
            else if (index >= allLootTables.size() - 1)
                index = 0;

            options.setLootTable(allLootTables.get(index));
            updateItem(5, lootTableIcon());
        });

        return lootTableIcon.asGuiItem(action);
    }

    private GuiItem saveIcon() {
        GuiItem saveIcon = ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave Loot Settings&f")).asGuiItem();
        saveIcon.setAction(e -> {
            e.getWhoClicked().closeInventory();
            if (options.getConfigFile() != null) {
                options.saveConfig(options.getConfigFile().getName());
                new ConfigureGameGui(e.getWhoClicked(), options);
            }
            else {
                Tools.formatInstructions("You need to save the game configuration first. Enter a name for these game settings (only letter, numbers, dash and underscores - no spaces):", "");
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), text -> {
                    options.saveConfig(text);
                    new ConfigureGameGui(e.getWhoClicked(), options);
                });
            }
            updateItem(8, saveIcon());
        });

        return saveIcon;
    }
}
