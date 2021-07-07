package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.datatypes.GameOptions;
import me.cheracc.battlegameshungerroyale.managers.LootManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.loot.LootTable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ConfigureLootGui extends Gui {
    private final GameOptions options;
    private final Gui sendingGui;

    public ConfigureLootGui(HumanEntity player, Gui sendingGui, GameOptions options) {
        super(1, "&0Loot Settings", new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.options = options;
        this.sendingGui = sendingGui;
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
        ItemBuilder genChestsIcon = ItemBuilder.from(Material.CHEST).name(Tools.componentalize("&eGenerate Random Chests: &f" +
                (options.isGenerateChests() ? "on" : "off")));
        genChestsIcon.lore(Tools.componentalize(Tools.wrapText("  &7Whether or not the plugin will search the map for locations to place loot chests. If disabled, no additional chests will be placed on the map.", ChatColor.GRAY)));

        return genChestsIcon.asGuiItem(e -> {
            options.toggleGenerateChests();
            updateItem(0, generateChestsIcon());
        });

    }

    private GuiItem fillChestsIcon() {
        ItemBuilder fillChestsIcon = ItemBuilder.from(Material.CHEST_MINECART).name(Tools.componentalize("&eFill all chests: &f" +
                (options.isFillAllChests() ? "on" : "off")));
        fillChestsIcon.lore(Tools.componentalize(Tools.wrapText("  &7Whether or not the plugin will fill chests with loot from the selected loot table. If disabled, the plugin will not place any loot in any chests - this is useful for maps that have pre-filled loot chests", ChatColor.GRAY)));

        return fillChestsIcon.asGuiItem(e -> {
            options.toggleFillAllChests();
            updateItem(1, fillChestsIcon());
        });
    }

    private GuiItem lenientSearchingIcon() {
        ItemBuilder loosenSearchIcon = ItemBuilder.from(Material.GRASS_BLOCK).name(Tools.componentalize("&eLoosen Chest Search Restrictions: &f" +
                (options.isLoosenSearchRestrictions() ? "on" : "off")));
        loosenSearchIcon.lore(Tools.componentalize(Tools.wrapText("  &7Determines how strictly the plugin searches for loot chest locations. If enabled, chests will be more 'tucked-away' - if disabled, chests will spawn in more obvious/open areas. Enable this for small maps or maps with little to no below-surface areas. Disable this for larger maps or maps with lots of caves and structures.", ChatColor.GRAY)));

        return loosenSearchIcon.asGuiItem(e -> {
            options.toggleLoosenSearchRestrictions();
            updateItem(2, lenientSearchingIcon());
        });
    }

    private GuiItem densityIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.PAINTING).name(Tools.componentalize("&eMax Chest Spawns Per Chunk: &f" + options.getMaxChestsPerChunk()));
        icon.lore(Tools.componentalize(Tools.wrapText("  &7How many chest locations to look for in each chunk. The plugin will stop searching a chunk when it finds this many locations. This does not guarantee that this many locations will be found. Increase this for smaller maps.", ChatColor.GRAY)));


        return icon.asGuiItem(e -> {
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
    }

    private GuiItem respawnTimeIcon() {
        GuiItem icon = ItemBuilder.from(Material.BARREL).name(Tools.componentalize("&eTime Between Chest Respawns: &f" + options.getChestRespawnTime())).asGuiItem();
        icon.setAction(e -> {
            int value = options.getChestRespawnTime();
            int change = 1;

            if (e.isShiftClick())
                change = 5;

            if (e.isLeftClick())
                value += change;
            if (e.isRightClick() && value > 1 + change)
                value -= change;

            options.setChestRespawnTime(value);
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
                new ConfigureGameGui(e.getWhoClicked(), options, sendingGui);
            }
            else {
                Tools.formatInstructions("You need to save the game configuration first. Enter a name for these game settings (only letter, numbers, dash and underscores - no spaces):", "");
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), text -> {
                    options.saveConfig(text);
                    new ConfigureGameGui(e.getWhoClicked(), options, sendingGui);
                });
            }
            updateItem(8, saveIcon());
        });

        return saveIcon;
    }
}
