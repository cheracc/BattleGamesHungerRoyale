package me.stipe.battlegameshungerroyale.tools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class Tools {
    public static TextColor RED = TextColor.color(255,0,0);
    public static TextColor WHITE = TextColor.color(255,255,255);
    public static TextColor BLUE = TextColor.color(0,0,255);
    public static TextColor GREEN = TextColor.color(0,255,0);
    public static TextColor GRAY = TextColor.color(180,180,180);
    public static TextColor YELLOW = TextColor.color(0,255,255);
    public static TextColor GOLD = TextColor.color(255,255,0);

    public static List<String> wrapText(String longText, ChatColor color) {
        List<String> wrappedText = new ArrayList<>();
        String[] words = longText.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (currentLine.length() > 28 || currentLine.toString().endsWith(".") || currentLine.toString().endsWith("!")) {
                wrappedText.add(color + currentLine.toString());
                if (currentLine.toString().endsWith(".") || currentLine.toString().endsWith("!"))
                    wrappedText.add("");
                currentLine.delete(0, currentLine.length());
            }
            String toAdd = words[i];
            if (toAdd.contains("\\n")) {
                toAdd = toAdd.substring(0, toAdd.indexOf("\\n"));
                words[i] = words[i].substring(toAdd.length() + 2);
                wrappedText.add(color + currentLine.toString() + (currentLine.length() == 0 ? "" : " ") + toAdd);
                currentLine = new StringBuilder();
                i--;
            } else {
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(toAdd);
            }
        }
        wrappedText.add(color + currentLine.toString());
        return wrappedText;
    }

    public static List<Component> toC(List<String> text) {
        List<Component> components = new ArrayList<>();

        for (String s : text)
            components.add(Component.text(ChatColor.translateAlternateColorCodes('&', s)));
        return components;
    }

    public static Component toC(String text) {
        return Component.text(ChatColor.translateAlternateColorCodes('&', text));
    }

    public static String secondsToMinutesAndSeconds(int timeInSeconds) {
        int i = Math.abs(timeInSeconds);
        int remainder = i % 3600, minutes = remainder / 60, seconds = remainder % 60;
        if (seconds == 0 && minutes == 0)
            return "No time at all";
        if (minutes == 0) {
            if (seconds == 1)
                return String.format("%s second", seconds);
            return String.format("%s seconds", seconds);
        }
        if (seconds == 0) {
            if (minutes == 1)
                return String.format("%s minute", minutes);
            return String.format("%s minutes", minutes);
        }
        if (seconds == 1) {
            if (minutes == 1)
                return String.format("%s minute, %s second", minutes, seconds);
            return String.format("%s minutes, %s second", minutes, seconds);
        }
        if (minutes == 1) {
            return String.format("%s minute, %s seconds", minutes, seconds);
        }
        return String.format("%s minutes, %s seconds", minutes, seconds);

    }

    public static String rebuildString(String[] stringArray, int startingLocation) {
        StringBuilder sb = new StringBuilder();

        if (stringArray.length < startingLocation + 1)
            return null;

        for (int i = startingLocation; i < stringArray.length; i++) {
            sb.append(stringArray[i]);
            if (i + 1 < stringArray.length)
                sb.append(" ");
        }
        return sb.toString();
    }

    public static TextComponent commandInstructions(String commandString) {
        String[] words = commandString.split(" ");
        String command = words[0];
        String argument = words[1];
        String property = argument.equalsIgnoreCase("desc") ? "description" : argument;
        return Component.text("=====================================================").color(TextColor.color(255,0,0))
                .append(Component.newline())
                .append(Component.text("                                Type").color(TextColor.color(255,255,255)))
                .append(Component.newline())
                .append(Component.text("                    " + commandString).color(TextColor.color(250,250,0)))
                .append(Component.newline())
                .append(Component.text("                 to change the " + property + " of this map").color(TextColor.color(255,255,255)))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("    (you can click on this message to start the command").color(TextColor.color(150,150,150)))
                .append(Component.newline())
                .append(Component.text(" or just type '/kitconfig' to cancel and return to the menu)").color(TextColor.color(150,150,150)))
                .append(Component.newline())
                .append(Component.text("=====================================================").color(TextColor.color(255,0,0)))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, command + " " + argument + " "))
                .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click here to save some typing!")));
    }


}
