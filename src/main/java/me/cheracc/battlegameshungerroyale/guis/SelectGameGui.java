package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SelectGameGui extends Gui {
    private final BghrApi api;

    public SelectGameGui(HumanEntity player, BghrApi api) {
        super(1, "Current Games:", new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.api = api;
        disableAllInteractions();
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        fillGui(player);
        open(player);
    }

    private void fillGui(HumanEntity player) {
        for (Game game : api.getGameManager().getActiveGames())
            addItem(gameIcon(player, game));
    }

    private GuiItem gameIcon(HumanEntity viewer, Game game) {
        ItemBuilder item = ItemBuilder.from(game.getGameIcon()).name(Tools.componentalize(StringUtils.center(Trans.late("  &a&l%s", game.getGameTypeName()), 20)));
        List<String> lore = new ArrayList<>();
        boolean hasPermission = viewer.hasPermission("bghr.admin.games");

        lore.add("&eMap: &f" + game.getMap().getMapName());
        lore.add("&ePlayers: &f" + game.getActivePlayers().size() + "/" + game.getStartingPlayersSize());
        if (game.getPhase().equalsIgnoreCase("pregame"))
            lore.add(String.format(" &8(&7%s required to start&8)", game.getOptions().getPlayersNeededToStart()));
        lore.add("&ePhase: &7" + game.getPhase());
        if (game.getCurrentGameTime() > 0)
            lore.add("&eGame Time: &7" + Tools.secondsToMinutesAndSeconds(game.getCurrentGameTime()));
        lore.add("");
        if (game.isOpenToPlayers())
            lore.add("     &d&lClick to Join!");
        lore.add("  &bRight click to spectate");

        if (hasPermission) {
            lore.add("");
            lore.add("&4[&cShift-Click&4]:&c Close this game");
        }

        item.lore(Tools.componentalize(lore));

        return item.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            if (game.isPlaying(p) || game.isSpectating(p)) {
                p.sendMessage(Trans.lateToComponent("You are already in that game"));
                return;
            }
            Game current = api.getGameManager().getPlayersCurrentGame(p);
            if (current != null)
                current.quit(p);

            if (e.isShiftClick() && hasPermission) {
                game.endGame(g -> {
                    e.getWhoClicked().closeInventory();
                    new SelectGameGui(e.getWhoClicked(), api);
                });
            } else if (e.isLeftClick() && game.isOpenToPlayers())
                game.join(p);
            else if (e.isRightClick()) {
                game.joinAsSpectator(p);
            }
        });
    }
}
