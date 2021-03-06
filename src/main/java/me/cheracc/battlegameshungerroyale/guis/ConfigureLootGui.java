package me.cheracc.battlegameshungerroyale.guis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.games.GameOptions;
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
    private final BghrApi api;
    private final GameOptions options;
    private final Gui sendingGui;

    public ConfigureLootGui(HumanEntity player, Gui sendingGui, GameOptions options, BghrApi api) {
        super(1, Trans.late("Loot Settings"), new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.api = api;
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
        ItemBuilder genChestsIcon = ItemBuilder.from(Material.CHEST).name(Trans.lateToComponent("&eGenerate Random Chests: &f%s",
                                                                                                (options.isGenerateChests() ? Trans.late("on") : Trans.late("off"))));
        genChestsIcon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7Whether or not the plugin will search the map for locations to place loot chests. If disabled, no additional chests will be placed on the map."), ChatColor.GRAY)));

        return genChestsIcon.asGuiItem(e -> {
            options.toggleGenerateChests();
            updateItem(0, generateChestsIcon());
        });
    }

    private GuiItem fillChestsIcon() {
        ItemBuilder fillChestsIcon = ItemBuilder.from(Material.CHEST_MINECART).name(Trans.lateToComponent("&eFill all chests: &f%s",
                                                                                                          (options.isFillAllChests() ? Trans.late("on") : Trans.late("off"))));
        fillChestsIcon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7Whether or not the plugin will fill chests with loot from the selected loot table. If disabled, the plugin will not place any loot in any chests - this is useful for maps that have pre-filled loot chests"), ChatColor.GRAY)));

        return fillChestsIcon.asGuiItem(e -> {
            options.toggleFillAllChests();
            updateItem(1, fillChestsIcon());
        });
    }

    private GuiItem lenientSearchingIcon() {
        ItemBuilder loosenSearchIcon = ItemBuilder.from(Material.GRASS_BLOCK).name(Trans.lateToComponent("&eLoosen Chest Search Restrictions: &f%s",
                                                                                                         (options.isLoosenSearchRestrictions() ? Trans.late("on") : Trans.late("off"))));
        loosenSearchIcon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7Determines how strictly the plugin searches for loot chest locations. If enabled, chests will be more 'tucked-away' - if disabled, chests will spawn in more obvious/open areas. Enable this for small maps or maps with little to no below-surface areas. Disable this for larger maps or maps with lots of caves and structures."), ChatColor.GRAY)));

        return loosenSearchIcon.asGuiItem(e -> {
            options.toggleLoosenSearchRestrictions();
            updateItem(2, lenientSearchingIcon());
        });
    }

    private GuiItem densityIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.PAINTING).name(Trans.lateToComponent("&eMax Chest Spawns Per Chunk: &f%s", options.getMaxChestsPerChunk()));
        icon.lore(Tools.componentalize(Tools.wrapText(Trans.late("  &7How many chest locations to look for in each chunk. The plugin will stop searching a chunk when it finds this many locations. This does not guarantee that this many locations will be found. Increase this for smaller maps."), ChatColor.GRAY)));

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
        GuiItem icon = ItemBuilder.from(Material.BARREL).name(Trans.lateToComponent("&eTime Between Chest Respawns: &f%s", options.getChestRespawnTime())).asGuiItem();
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
        lootTableIcon.name(Trans.lateToComponent("&eLoot Table: &f%s", split[split.length - 1]));

        lootTableIcon.lore(Trans.lateToComponent("&bClick to cycle through loot tables"), Trans.lateToComponent("&bRight click to cycle backwards"));
        List<LootTable> allLootTables = api.getGameManager().getLootTables();

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
        GuiItem saveIcon = ItemBuilder.from(Material.WRITABLE_BOOK).name(Trans.lateToComponent("&eSave Loot Settings&f")).asGuiItem();
        saveIcon.setAction(e -> {
            e.getWhoClicked().closeInventory();
            if (options.getConfigFile() != null) {
                options.saveConfig(options.getConfigFile().getName(), api.getPlugin());
                new ConfigureGameGui(e.getWhoClicked(), options, sendingGui, api);
            } else {
                Tools.formatInstructions(Trans.late("You need to save the game configuration first. Enter a name for these game settings (only letter, numbers, dash and underscores - no spaces):"), "");
                api.getTextInputListener().getNextInputFrom((Player) e.getWhoClicked(), text -> {
                    options.saveConfig(text, api.getPlugin());
                    new ConfigureGameGui(e.getWhoClicked(), options, sendingGui, api);
                });
            }
            updateItem(8, saveIcon());
        });

        return saveIcon;
    }
}
