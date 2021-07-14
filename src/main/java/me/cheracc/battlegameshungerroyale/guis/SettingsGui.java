package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

public class SettingsGui extends Gui {
    private final PlayerSettings settings;

    public SettingsGui(HumanEntity player) {
        super(1, "Player Settings", InteractionModifier.VALUES);
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());
        Player p = (Player) player;
        PlayerData data = PlayerManager.getInstance().getPlayerData(p);
        this.settings = data.getSettings();

        fillGui();
        open(player);
    }

    private void fillGui() {
        setItem(0, mainScoreboardIcon(0));
        setItem(1, gameScoreboardIcon(1));
        setItem(2, showHelpIcon(2));
        setItem(3, setDefaultKit(3));

        setItem(8, saveIcon());
    }

    private GuiItem mainScoreboardIcon(int slot) {
        ItemBuilder item = ItemBuilder.from(Material.PAINTING).name(Tools.componentalize("&eMain Scoreboard: &f" + (settings.isShowMainScoreboard() ? "&aON" : "&cOFF")));
        item.lore(Tools.componentalize("&bClick to toggle"));
        return item.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            settings.toggleMainScoreboard();
            if (!GameManager.getInstance().isInAGame(p)) {
                if (settings.isShowMainScoreboard())
                    p.setScoreboard(GameManager.getInstance().getMainScoreboard());
                else
                    p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
            updateItem(slot, mainScoreboardIcon(slot));
        });
    }

    private GuiItem gameScoreboardIcon(int slot) {
        ItemBuilder item = ItemBuilder.from(Material.ITEM_FRAME).name(Tools.componentalize("&eIn-Game Scoreboard: &f" + (settings.isShowGameScoreboard() ? "&aON" : "&cOFF")));
        item.lore(Tools.componentalize("&bClick to toggle"));
        return item.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            settings.toggleGameScoreboard();
            Game game = GameManager.getInstance().getPlayersCurrentGame(p);
            if (game != null) {
                if (settings.isShowGameScoreboard())
                    p.setScoreboard(game.getScoreboard());
                else
                    p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
            updateItem(slot, gameScoreboardIcon(slot));
        });
    }

    private GuiItem showHelpIcon(int slot) {
        ItemBuilder item = ItemBuilder.from(Material.PLAYER_HEAD).name(Tools.componentalize("&eShow Hint/Help Messages: &f" + (settings.isShowHelp() ? "&aON" : "&cOFF")));
        item.lore(Tools.componentalize("&bClick to toggle"));
        return item.asGuiItem(e -> {
            settings.setShowHelp(!settings.isShowHelp());
            updateItem(slot, showHelpIcon(slot));
        });
    }

    private GuiItem setDefaultKit(int slot) {
        String kitName = "Random";
        if (settings.getDefaultKit() != null) {
            Kit kit = KitManager.getInstance().getKit(settings.getDefaultKit());
            if (kit != null)
                kitName = kit.getName();
        }

        ItemBuilder item = ItemBuilder.from(Material.GOLDEN_SWORD).name(Tools.componentalize("&eFavorite Kit: &f" + kitName));
        item.lore(Tools.componentalize(Tools.wrapText("&bThis is the default kit you will use when joining a game unless you have already selected a different kit. You can change your kit from the kit menu.", ChatColor.AQUA)));
        item.flags(ItemFlag.HIDE_ATTRIBUTES);
        return item.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new SelectKitGui(e.getWhoClicked(), this, kit -> {
                e.getWhoClicked().closeInventory();
                settings.setDefaultKit(kit.getName());
                updateItem(slot, setDefaultKit(slot));
                open(e.getWhoClicked());
            });
        });

    }

    private GuiItem saveIcon() {
        ItemBuilder item = ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave Settings"));
        return item.asGuiItem(e -> {
            PlayerManager.getInstance().getPlayerData((Player) e.getWhoClicked()).setModified(true);
            e.getWhoClicked().closeInventory();
        });
    }
}
