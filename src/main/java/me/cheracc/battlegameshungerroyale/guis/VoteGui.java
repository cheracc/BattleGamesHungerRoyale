package me.cheracc.battlegameshungerroyale.guis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.MapData;
import me.cheracc.battlegameshungerroyale.types.games.GameOptions;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VoteGui extends Gui {
    private final BghrApi api;

    public VoteGui(HumanEntity player, BghrApi api) {
        super(api.getMapManager().getMaps().size() / 9 + 1, "Cast Your Vote:", InteractionModifier.VALUES);
        this.api = api;
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        fillGui();
        open(player);
    }

    private void forceUpdateAll() {
        int slot = 0;
        for (MapData map : api.getMapManager().getMaps()) {
            List<GameOptions> games = new ArrayList<>();
            api.getGameManager().getAllConfigs().forEach(opt -> {
                if (opt.getMap().getMapName().equals(map.getMapName()))
                    games.add(opt);
            });
            updateItem(slot, mapIcon(map, games));
            slot++;
        }
    }

    private void fillGui() {
        int slot = 0;
        for (MapData map : api.getMapManager().getMaps()) {
            List<GameOptions> games = new ArrayList<>();
            api.getGameManager().getAllConfigs().forEach(opt -> {
                if (opt.getMap().getMapName().equals(map.getMapName()))
                    games.add(opt);
            });
            setItem(slot, mapIcon(map, games));
            slot++;
        }
    }

    private GuiItem mapIcon(MapData map, List<GameOptions> games) {
        List<String> lore = new ArrayList<>();
        int votes = api.getGameManager().getVotes(map.getMapName());
        ItemBuilder icon = ItemBuilder.from(map.getIcon()).name(Tools.componentalize(ChatColor.YELLOW + map.getMapName() +
                                                                                             ((votes > 0) ? " &8(&7" + votes + " vote" + (votes > 1 ? "s" : "") + "&8)" : "")));

        Integer[] needed = new Integer[games.size()];
        Integer[] respawns = new Integer[games.size()];
        String[] types = new String[games.size()];

        int i = 0;
        for (GameOptions g : games) {
            needed[i] = g.getPlayersNeededToStart();
            respawns[i] = g.getLivesPerPlayer() - 1;
            types[i] = g.getGameType();
            i++;
        }

        StringBuilder gameTypes = new StringBuilder();
        for (String s : types) {
            if (!gameTypes.toString().isEmpty() && !gameTypes.toString().contains(s)) {
                gameTypes.append(", ");
            }
            if (!gameTypes.toString().contains(s))
                gameTypes.append(s);
        }

        int min = Collections.min(Arrays.asList(needed));
        int max = Collections.max(Arrays.asList(needed));
        String neededString;

        if (max == min)
            neededString = Integer.toString(max);
        else
            neededString = String.format("%s-%s", min, max);

        min = Collections.min(Arrays.asList(respawns));
        max = Collections.max(Arrays.asList(respawns));
        String respawnsString;

        if (max == min)
            respawnsString = Integer.toString(max);
        else
            respawnsString = String.format("%s-%s", min, max);

        lore.add("&fPlayers Needed to Start: &7" + neededString);
        lore.add("&fRespawns: &7" + respawnsString);
        lore.add("&fGame Type: &7" + gameTypes);
        lore.add("&fBorder Size: &7" + map.getBorderRadius() * 2);
        lore.add("&fTimes Played: &7" + map.getTimesPlayed());
        lore.add("&fAvg. Game Length: &7" + Tools.secondsToAbbreviatedMinsSecs((int) map.getAverageLength()));
        lore.add("");
        lore.add("&bClick To Vote For This Map");

        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            api.getGameManager().addVote((Player) e.getWhoClicked(), map.getMapName());
            forceUpdateAll();
        });
    }
}
