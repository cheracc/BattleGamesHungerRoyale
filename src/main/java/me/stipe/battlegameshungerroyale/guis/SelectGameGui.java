package me.stipe.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.datatypes.Game;
import me.stipe.battlegameshungerroyale.managers.GameManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SelectGameGui extends Gui {

    public SelectGameGui(HumanEntity player) {
        super(1, Tools.componentalize("&0Current Games:"));
        disableAllInteractions();
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        fillGui();
        open(player);
    }

    private void fillGui() {
        for (Game game : GameManager.getInstance().getActiveGames())
            addItem(gameIcon(game));
    }

    private GuiItem gameIcon(Game game) {
        ItemBuilder item = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize("&eMap: &f" + game.getMap().getMapName()));
        List<String> lore = new ArrayList<>();

        lore.add("&ePlayers: &7" + game.getActivePlayers().size() + "/" + game.getStartingPlayersSize());
        lore.add("&ePhase: &7" + game.getPhase());
        if (game.getCurrentGameTime() > 0)
            lore.add("&eGame Time: &7" + Tools.secondsToMinutesAndSeconds(game.getCurrentGameTime()));
        lore.add("");
        if (game.isOpenToPlayers())
            lore.add("    &d&lClick to Join!");
        lore.add("&bRight click to spectate");

        item = item.lore(Tools.componentalize(lore));

        return item.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            if (game.isPlaying(p) || game.isSpectating(p)) {
                p.sendMessage(Tools.componentalize("You are already in that game"));
                return;
            }
            if (e.isLeftClick() && game.isOpenToPlayers()) {
                game.join(p);
            }
            else if (e.isRightClick()) {
                e.getWhoClicked().teleport(game.getWorld().getSpawnLocation());
                e.getWhoClicked().setGameMode(GameMode.SPECTATOR);
            }
        });
    }
}
