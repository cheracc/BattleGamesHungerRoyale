package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class Skorbord {
    private final Map<UUID, Scoreboard> scoreboards;
    private final List<ScoreboardLine> lines;
    private final String title;

    public Skorbord(String title) {
        scoreboards = new HashMap<>();
        lines = new ArrayList<>();
        this.title = title;
    }

    public void sendToPlayer(Player player) {
        if (scoreboards.containsKey(player.getUniqueId()))
            player.setScoreboard(scoreboards.get(player.getUniqueId()));
        else {
            Scoreboard scoreboard = createNewScoreboard(title);
            scoreboards.put(player.getUniqueId(), scoreboard);
            player.setScoreboard(scoreboard);
        }
    }

    public void setLine(int lineNumber, String text, Justification justification) {

    }

    public void setTitle(String title) {

    }

    private Scoreboard createNewScoreboard(String title) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("main", "dummy", Tools.componentalize(""));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.displayName(Tools.componentalize(title));
        for (int i = 15; i >= 0; i--) {
            String entry = ChatColor.values()[i] + "" + ChatColor.values()[i + 1];
            Team lineText = sb.registerNewTeam(String.format("line%s", i));
            lineText.addEntry(entry);
            obj.getScore(entry).setScore(i);
        }
        return sb;
    }

    public enum Justification {LEFT, CENTER, RIGHT}

    private static class ScoreboardLine {
        String text;
        int lineNumber;
        Justification justification;
    }
}
