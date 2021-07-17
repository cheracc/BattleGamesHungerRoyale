package me.cheracc.battlegameshungerroyale.tools;

import com.destroystokyo.paper.Namespaced;
import me.cheracc.battlegameshungerroyale.BGHR;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Tools {
    public static TextComponent BLANK_LINE = Component.text("");
    public static NamespacedKey PLUGIN_KEY = new NamespacedKey(BGHR.getPlugin(), "battlegameshungerroyale");

    public static ItemStack tagAsPluginItem(ItemStack item) {
        if (item == null)
            return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        meta.getPersistentDataContainer().set(PLUGIN_KEY, PersistentDataType.LONG, System.currentTimeMillis());
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isPluginItem(ItemStack item) {
        if (item == null)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        return (meta.getPersistentDataContainer().has(PLUGIN_KEY, PersistentDataType.LONG));
    }

    public static String getTimestamp() {
        return Instant.now().toString().replace(":","-").split("\\.")[0];
    }

    public static List<String> wrapText(String longText, ChatColor color) {
        List<String> wrappedText = new ArrayList<>();
        String[] words = longText.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', currentLine.toString())).length() > 28 || currentLine.toString().endsWith(".") || currentLine.toString().endsWith("!")) {
                wrappedText.add(color + currentLine.toString());
                if (currentLine.toString().endsWith(".") || currentLine.toString().endsWith("!"))
                    wrappedText.add("");
                currentLine.delete(0, currentLine.length());
            }
            String toAdd = words[i];
            if (toAdd.contains("\\n")) {
                toAdd = toAdd.substring(0, toAdd.indexOf("\\n"));
                words[i] = words[i].substring(toAdd.length() + 2);
                wrappedText.add(color + currentLine.toString() + (ChatColor.stripColor(currentLine.toString()).length() == 0 ? "" : " ") + toAdd);
                currentLine = new StringBuilder();
                i--;
            } else {
                currentLine.append(ChatColor.stripColor(currentLine.toString()).length() == 0 ? "" : " ").append(toAdd);
            }
        }
        wrappedText.add(color + currentLine.toString());
        return wrappedText;
    }

    public static void uncheckedTeleport(Player player, Location location) {
        double value = Bukkit.getServer().spigot().getSpigotConfig().getDouble("moved-too-quickly-multiplier", 10);

        Bukkit.getServer().spigot().getSpigotConfig().set("moved-too-quickly-multiplier", Double.MAX_VALUE);
        player.teleport(location);
        Bukkit.getServer().spigot().getSpigotConfig().set("moved-too-quickly-multiplier", value);
    }

    public static String decomponentalize(Component component) {
        if (component == null)
            return null;
        String string = LegacyComponentSerializer.legacyAmpersand().serialize(component);
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public static List<String> decomponentalize(List<Component> componentList) {
        List<String> strings = new ArrayList<>();
        if (componentList == null || componentList.isEmpty())
            return strings;
        componentList.forEach(c -> {
            if (c != null)
                strings.add(decomponentalize(c));
        });
        return strings;
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

    public static String keyToDisplayName(String key) {
        return WordUtils.capitalize(key.replace("_", " "));
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

    public static String secondsToAbbreviatedMinsSecs(int timeInSeconds) {
        int i = Math.abs(timeInSeconds);
        int remainder = i % 3600, minutes = remainder / 60, seconds = remainder % 60;
        if (seconds == 0 && minutes == 0)
            return "No time at all";
        if (minutes == 0) {
            if (seconds == 1)
                return String.format("%s second", seconds);
            return String.format("%s seconds", seconds);
        }
        else {
            return String.format("%s:%02d", minutes, seconds);
        }

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
        TextComponent formattedInstructions = LegacyComponentSerializer.legacyAmpersand().deserialize(instructions);
        TextComponent borderBar = Component.text("=====================================================").color(TextColor.color(255,0,0));
        TextComponent instComp = formattedInstructions.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, currentValue)).color(TextColor.color(255,255,255))
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

    public static ItemStack makeItemPlaceable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        Set<Namespaced> allBlocks = new HashSet<>();

        if (meta == null)
            return item;

        for (Material m : Material.values()) {
            if (m.isBlock() && !m.name().contains("LEGACY"))
                allBlocks.add(m.getKey());
        }
        meta.setPlaceableKeys(allBlocks);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Extract the contents of a .zip resource file to a destination directory.
     * <p>
     * Overwrite existing files.
     *
     * @param myClass     The class used to find the zipResource.
     * @param zipResource Must end with ".zip".
     * @param destDir     The path of the destination directory, which must exist.
     */
    public static void extractZipResource(Class myClass, String zipResource, Path destDir)
    {
        if (myClass == null || zipResource == null || !zipResource.toLowerCase().endsWith(".zip") || !Files.isDirectory(destDir))
        {
            throw new IllegalArgumentException("myClass=" + myClass + " zipResource=" + zipResource + " destDir=" + destDir);
        }

        try (InputStream is = myClass.getResourceAsStream(zipResource);
             BufferedInputStream bis = new BufferedInputStream(is);
             ZipInputStream zis = new ZipInputStream(bis))
        {
            ZipEntry entry;
            byte[] buffer = new byte[2048];
            while ((entry = zis.getNextEntry()) != null)
            {
                // Build destination file
                File destFile = destDir.resolve(entry.getName()).toFile();

                if (entry.isDirectory())
                {
                    // Directory, recreate if not present
                    if (!destFile.exists() && !destFile.mkdirs())
                    {
                        Bukkit.getLogger().warning("extractZipResource() can't create destination folder : " + destFile.getAbsolutePath());
                    }
                    continue;
                }
                // Plain file, copy it
                try (FileOutputStream fos = new FileOutputStream(destFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length))
                {
                    int len;
                    while ((len = zis.read(buffer)) > 0)
                    {
                        bos.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException ex)
        {
            ex.printStackTrace();
            Bukkit.getLogger().warning("extractZipResource() problem extracting resource for myClass=" + myClass + " zipResource=" + zipResource);
        }
    }
}
