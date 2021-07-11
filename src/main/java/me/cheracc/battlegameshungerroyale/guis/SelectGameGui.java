package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SelectGameGui extends Gui {

    public SelectGameGui(HumanEntity player) {
        super(1, "Current Games:", new HashSet<>(Arrays.asList(InteractionModifier.values())));
        disableAllInteractions();
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        fillGui(player);
        open(player);
    }

    private void fillGui(HumanEntity player) {
        for (Game game : GameManager.getInstance().getActiveGames())
            addItem(gameIcon(player, game));
    }

    private GuiItem gameIcon(HumanEntity viewer, Game game) {
        ItemBuilder item = ItemBuilder.from(Material.HEART_OF_THE_SEA).name(Tools.componentalize("&eMap: &f" + game.getMap().getMapName()));
        List<String> lore = new ArrayList<>();
        boolean hasPermission = false;
        if (BGHR.getPerms() != null)
            hasPermission = BGHR.getPerms().has(viewer, "bghr.admin.games");
        else if (viewer.isOp())
            hasPermission = true;

        lore.add("&ePlayers: &7" + game.getActivePlayers().size() + "/" + game.getStartingPlayersSize());
        lore.add("&ePhase: &7" + game.getPhase());
        if (game.getCurrentGameTime() > 0)
            lore.add("&eGame Time: &7" + Tools.secondsToMinutesAndSeconds(game.getCurrentGameTime()));
        lore.add("");
        if (game.isOpenToPlayers())
            lore.add("    &d&lClick to Join!");
        lore.add("&bRight click to spectate");

        if (hasPermission) {
            lore.add("");
            lore.add("&4[&cShift-Click&4]:&f Close this game");
        }

        item.lore(Tools.componentalize(lore));

        boolean finalHasPermission = hasPermission;
        return item.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            if (game.isPlaying(p) || game.isSpectating(p)) {
                p.sendMessage(Tools.componentalize("You are already in that game"));
                return;
            }
            Game current = GameManager.getInstance().getPlayersCurrentGame(p);
            if (current != null)
                current.quit(p);

            if (e.isShiftClick() && finalHasPermission) {
                game.endGame();
                e.getWhoClicked().closeInventory();
                new SelectGameGui(e.getWhoClicked());
            }
            else if (e.isLeftClick() && game.isOpenToPlayers())
                game.join(p);
            else if (e.isRightClick()) {
                game.joinAsSpectator(p);
            }
        });
    }
}
