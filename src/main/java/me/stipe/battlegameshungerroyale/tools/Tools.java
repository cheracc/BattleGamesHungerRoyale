package me.stipe.battlegameshungerroyale.tools;

import me.stipe.battlegameshungerroyale.BGHR;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Tools {
    public static TextComponent BLANK_LINE = Component.text("");
    public static NamespacedKey UUID_KEY = new NamespacedKey(BGHR.getPlugin(), "uuid_key");

    public static void saveUuidToItemMeta(UUID uuid, ItemMeta meta) {
        meta.getPersistentDataContainer().set(UUID_KEY, PersistentDataType.STRING, uuid.toString());
    }

    public static @Nullable UUID getUuidFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getPersistentDataContainer() == null)
            return null;

        String data = meta.getPersistentDataContainer().get(UUID_KEY, PersistentDataType.STRING);

        if (data != null) {
            return UUID.fromString(data);
        }
        return null;
    }

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

    public static List<Component> componentalize(List<String> text) {
        List<Component> components = new ArrayList<>();

        for (String s : text)
            components.add(componentalize(s));
        return components;
    }

    public static Component componentalize(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(ChatColor.translateAlternateColorCodes('&', ChatColor.WHITE + text)).decoration(TextDecoration.ITALIC, false);
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

    public static TextComponent formatInstructions(String instructions, String currentValue) {
        TextComponent borderBar = Component.text("=====================================================").color(TextColor.color(255,0,0));
        TextComponent instComp = Component.text(instructions).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, currentValue)).color(TextColor.color(255,255,255))
                                          .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click here to copy the current text into the chat input box")));

        return borderBar.append(Component.newline()).append(instComp).append(Component.newline()).append(borderBar);

    }

    public static String configOptionToFieldName(String string) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            Character c = string.charAt(i);
            if (c.equals(' ')) {
                i++;
                sb.append(Character.toUpperCase(string.charAt(i)));
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static String fieldNameToConfigOption(String string) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            Character c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(" ");
                sb.append(Character.toLowerCase(c));
            }
            else
                sb.append(c);
        }
        return sb.toString();
    }

    public static String integerToRomanNumeral(int input) {
        String[] numerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV"};

        if (input > 15)
            return "";

        return numerals[input];
    }

    public static void saveObjectToPlayer(String key, Object object, Player p) {
        if (p.hasMetadata(key)) {
            p.removeMetadata(key, BGHR.getPlugin());
        }
        p.setMetadata(key, new FixedMetadataValue(BGHR.getPlugin(), object));
    }

    public static @Nullable Object getObjectFromPlayer(String key, Player p) {
        if (p.getMetadata(key) != null && !p.getMetadata(key).isEmpty()) {
            Object o = p.getMetadata(key).get(0).value();
            if (o instanceof PotionEffect) {
                Bukkit.getLogger().info("Fetched Potion Effect: " + ((PotionEffect) o).serialize());
            }
            return o;
        }
        Bukkit.getLogger().info("no object on " + p.getName() + " with key " + key);

        return null;
    }

    public static void removeObjectFromPlayer(String key, Player p) {
        if (p.hasMetadata(key)) {
            Object o = p.getMetadata(key).get(0).value();
            p.removeMetadata(key, BGHR.getPlugin());
        }
    }

    // debug
    public static void outputSectionToConsole(ConfigurationSection section) {
        for (String s : section.getKeys(false)) {
            Bukkit.getLogger().info(s + ": " + section.get(s));
        }
    }

    public static int getLastEmptyHotbarSlot(Player p) {
        for (int i = 8; i >= 0; i--) {
            ItemStack item = p.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR)
                return i;
        }
        return -1;
    }



}
