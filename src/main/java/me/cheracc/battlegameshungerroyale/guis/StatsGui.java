package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.builder.item.SkullBuilder;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

public class StatsGui extends Gui {
    private final PlayerData data;
    private final PlayerStats stats;

    public StatsGui(HumanEntity viewer, PlayerData data) {
        super(GuiType.DISPENSER, data.getName() + "'s Statistics:", InteractionModifier.VALUES);
        this.data = data;
        this.stats = data.getStats();
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        fillGui();
        open(viewer);
    }

    private void fillGui() {
        setItem(1, playHistoryIcon());
        setItem(4, playerStatsIcon());
        setItem(7, otherStatsIcon());
    }

    private GuiItem playHistoryIcon() {
        SkullBuilder icon = ItemBuilder.skull().owner(Bukkit.getOfflinePlayer(data.getUuid())).name(Tools.componentalize("&ePlayer History"));
        List<String> lore = new ArrayList<>();

        lore.add("&fGames Played&7: " + stats.getPlayed());
        lore.add("&fGames Won&7: " + stats.getWins());
        lore.add("&fSecond Place Finishes&7: " + stats.getSecondPlaceFinishes());
        lore.add("&fGames Quit&7: " + stats.getGamesQuit());
        lore.add("&fTime Played&7: " + Tools.secondsToAbbreviatedMinsSecs((int) stats.getTotalTimePlayed()));

        icon.lore(Tools.componentalize(lore));
        return icon.asGuiItem();
    }

    private GuiItem playerStatsIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.NETHERITE_CHESTPLATE).name(Tools.componentalize("&ePlayer Statistics"));
        icon.flags(ItemFlag.HIDE_ATTRIBUTES);
        List<String> lore = new ArrayList<>();

        lore.add("&fKills&7: " + stats.getKills());
        lore.add("&fDeaths&7: " + stats.getDeaths());
        lore.add("&fDamage Dealt&7: " + stats.getDamageDealt());
        lore.add("&fDamage Taken&7: " + stats.getDamageTaken());
        lore.add("&fHighest Kill Streak&7: " + stats.getKillStreak());

        icon.lore(Tools.componentalize(lore));
        return icon.asGuiItem();
    }

    private GuiItem otherStatsIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.GOLDEN_BOOTS).name(Tools.componentalize("&eOther Stats"));
        icon.flags(ItemFlag.HIDE_ATTRIBUTES);
        List<String> lore = new ArrayList<>();

        lore.add("&fAbilities Used&7: " + stats.getActiveAbilitiesUsed());
        lore.add("&fChests Opened&7: " + stats.getChestsOpened());
        lore.add("&fItems Looted&7: " + stats.getItemsLooted());
        lore.add("&fArrows Shot&7: " + stats.getArrowsShot());
        lore.add("&fFood Eaten&7: " + stats.getFoodEaten());
        lore.add("&fMonsters Killed&7: " + stats.getMonstersKilled());
        lore.add("&fAnimals Killed&7: " + stats.getAnimalsKilled());

        icon.lore(Tools.componentalize(lore));
        return icon.asGuiItem();
    }
}
