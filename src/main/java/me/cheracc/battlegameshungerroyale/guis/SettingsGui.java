package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.PlayerSettings;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

public class SettingsGui extends Gui {
    private final BghrApi api;
    private final PlayerSettings settings;

    public SettingsGui(HumanEntity player, BghrApi api) {
        super(1, "Player Settings", InteractionModifier.VALUES);
        this.api = api;
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());
        Player p = (Player) player;
        PlayerData data = api.getPlayerManager().getPlayerData(p);
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
        ItemBuilder item = ItemBuilder.from(Material.PAINTING).name(Trans.lateToComponent("&eMain Scoreboard: &f%s", (settings.isShowMainScoreboard() ? "&aON" : "&cOFF")));
        item.lore(Trans.lateToComponent("&bClick to toggle"));
        return item.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            settings.toggleMainScoreboard();
            if (!api.getGameManager().isInAGame(p)) {
                if (settings.isShowMainScoreboard())
                    p.setScoreboard(api.getDisplayManager().getMainScoreboard());
                else
                    p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
            updateItem(slot, mainScoreboardIcon(slot));
        });
    }

    private GuiItem gameScoreboardIcon(int slot) {
        ItemBuilder item = ItemBuilder.from(Material.ITEM_FRAME).name(Trans.lateToComponent("&eIn-Game Scoreboard: &f%s", settings.isShowGameScoreboard() ? "&aON" : "&cOFF"));
        item.lore(Trans.lateToComponent("&bClick to toggle"));
        return item.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            settings.toggleGameScoreboard();
            Game game = api.getGameManager().getPlayersCurrentGame(p);
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
        ItemBuilder item = ItemBuilder.from(Material.PLAYER_HEAD).name(Trans.lateToComponent("&eShow Hint/Help Messages: &f%s", settings.isShowHelp() ? "&aON" : "&cOFF"));
        item.lore(Trans.lateToComponent("&bClick to toggle"));
        return item.asGuiItem(e -> {
            settings.setShowHelp(!settings.isShowHelp());
            updateItem(slot, showHelpIcon(slot));
        });
    }

    private GuiItem setDefaultKit(int slot) {
        String kitName = "Random";
        if (settings.getDefaultKit() != null) {
            Kit kit = api.getKitManager().getKit(settings.getDefaultKit());
            if (kit != null)
                kitName = kit.getName();
        }

        ItemBuilder item = ItemBuilder.from(Material.GOLDEN_SWORD).name(Trans.lateToComponent("&eFavorite Kit: &f%s", kitName));
        item.lore(Tools.componentalize(Tools.wrapText("&bThis is the default kit you will use when joining a game unless you have already selected a different kit. You can change your kit from the kit menu.", ChatColor.AQUA)));
        item.flags(ItemFlag.HIDE_ATTRIBUTES);
        return item.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new SelectKitGui(e.getWhoClicked(), this, api.getKitManager(), kit -> {
                e.getWhoClicked().closeInventory();
                settings.setDefaultKit(kit.getName());
                updateItem(slot, setDefaultKit(slot));
                open(e.getWhoClicked());
            });
        });
    }

    private GuiItem saveIcon() {
        ItemBuilder item = ItemBuilder.from(Material.WRITABLE_BOOK).name(Trans.lateToComponent("&eSave Settings"));
        return item.asGuiItem(e -> {
            api.getPlayerManager().getPlayerData((Player) e.getWhoClicked()).setModified(true);
            e.getWhoClicked().closeInventory();
        });
    }
}
