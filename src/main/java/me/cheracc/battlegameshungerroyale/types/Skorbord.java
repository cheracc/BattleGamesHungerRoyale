package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.*;

import java.util.*;

public class Skorbord {
    private final Scoreboard scoreboard;
    private final List<ScoreboardLine> lines;
    private final BghrApi api;

    public Skorbord(String title, List<String> text, BghrApi api) {
        scoreboard = createNewScoreboard(title);
        lines = new ArrayList<>();
        this.api = api;
        loadLines(text);
        api.logr().debug("created sb %s", title);
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void update() {
        List<String> scoreboardText = generateText();
        for (int index = 0; index < 15; index++) {
            String lineText = scoreboardText.get(index);
            if (lines.get(index).center)
                lineText = center(scoreboardText, index);
            setLine(index + 1, lineText);
        }
    }

    private void loadLines(List<String> text) {
        for (String s : text) {
            lines.add(new ScoreboardLine(text.indexOf(s) + 1, s.startsWith("@c"), s.replace("@c", "")));
        }
    }

    private String stripColor(String string) {
        String translated = ChatColor.translateAlternateColorCodes('&', string);
        String stripped = ChatColor.stripColor(translated);
        return stripped.replaceAll("[&][a-zA-Z0-9]", "");
    }

    private List<String> generateText() {
        List<String> finalText = new ArrayList<>();
        Map<Integer, String> replaceAfter = new HashMap<>();

        for (int i = 0; i < 15; i++) {
            int index = i;
            String pretext = lines.stream().filter(l -> l.lineNumber == index + 1).findFirst().orElse(new ScoreboardLine(0, false, "")).text;
            if (pretext.contains("%bghr_skorbord_bar%")) {
                pretext = pretext.replace("%bghr_skorbord_bar%", "@BAR");
                replaceAfter.put(index, pretext);
            }
            finalText.add(api.replacePlaceholders(pretext));
        }
        for (Map.Entry<Integer, String> e : replaceAfter.entrySet()) {
            finalText.remove(e.getValue());
            finalText.add(e.getKey(), e.getValue().replace("@BAR", equalsBar(finalText)));
        }
        return finalText;
    }

    private String equalsBar(List<String> text) {
        List<String> strippedText = new ArrayList<>();
        text.forEach(t -> strippedText.add(stripColor(t)));

        String longestString = strippedText.stream().max(Comparator.comparing(String::length)).orElse("");
        int longest = longestString.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < longest * 0.8; i++)
            sb.append("=");
        return sb.toString();
    }

    private void setLine(int line, String text) {
        Team lineTeam = scoreboard.getTeam("line" + line);
        String entry = lineTeam.getEntries().stream().findFirst().orElse("");
        Score score = scoreboard.getObjective("main").getScore(entry);
        if (text.equals("")) {
            // remove that line
            scoreboard.resetScores(entry);
        } else {
            lineTeam.prefix(Tools.componentalize(text));
            score.setScore(16 - line);
        }
    }

    private String center(List<String> text, int index) {
        List<String> strippedText = new ArrayList<>();
        text.forEach(t -> strippedText.add(stripColor(t)));

        int longest = strippedText.stream().max(Comparator.comparing(String::length)).orElse("").length();

        if (strippedText.get(index).length() == longest)
            return text.get(index);

        else {
            int difference = longest - strippedText.get(index).length();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < difference / 2; i++) {
                sb.append(" ");
            }
            return sb.append(text.get(index)).toString();
        }
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

    private static class ScoreboardLine {
        String text;
        int lineNumber;
        boolean center;

        ScoreboardLine(int lineNumber, boolean center, String text) {
            this.text = text;
            this.lineNumber = lineNumber;
            this.center = center;
        }
    }
}
