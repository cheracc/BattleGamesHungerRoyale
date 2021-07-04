package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.datatypes.GameOptions;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

public class ConfigureLootGui extends Gui {
    private final GameOptions options;

    public ConfigureLootGui(HumanEntity player, Gui sendingGui, GameOptions options) {
        super(1, Tools.componentalize("&0Loot Settings"));
        Bukkit.getLogger().info("Called it!");
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
        //setItem(5, lootTableIcon());
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
        });

        return icon;
    }

    private GuiItem lootTableIcon() {
        GuiItem icon = null;

        return icon;
    }

    private GuiItem saveIcon() {
        GuiItem saveIcon = ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave Loot Settings&f" +
                (options.isFillAllChests() ? "on" : "off"))).asGuiItem();
        saveIcon.setAction(e -> {
            if (options.getConfigFile() != null)
                options.saveConfig(options.getConfigFile().getName());
            else {
                Tools.formatInstructions("You need to save the game configuration first. Enter a name for these game settings (only letter, numbers, dash and underscores - no spaces):", "");
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), text -> {
                    options.saveConfig(text);
                    new ConfigureGameGui(e.getWhoClicked(), options);
                });
            }
            updateItem(1, fillChestsIcon());
        });

        return saveIcon;
    }
}
