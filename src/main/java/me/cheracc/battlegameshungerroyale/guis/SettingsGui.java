package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

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

        setItem(8, saveIcon());
    }

    private GuiItem mainScoreboardIcon(int slot) {
        ItemBuilder item = ItemBuilder.from(Material.PAINTING).name(Tools.componentalize("&eMain Scoreboard: &f" + (settings.isAlwaysShowScoreboard() ? "on" : "off")));
        return item.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            settings.toggleScoreboard();
            if (settings.isAlwaysShowScoreboard())
                p.setScoreboard(GameManager.getInstance().getMainScoreboard());
            else
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            updateItem(slot, mainScoreboardIcon(slot));
        });
    }

    private GuiItem saveIcon() {
        ItemBuilder item = ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave Settings"));
        return item.asGuiItem(e -> {
            //TODO save
            e.getWhoClicked().closeInventory();
        });
    }
}
