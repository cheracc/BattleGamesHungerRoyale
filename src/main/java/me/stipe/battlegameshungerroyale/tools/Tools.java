package me.stipe.battlegameshungerroyale.tools;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class Tools {
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
}
