package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

public class Hologram {
    private final Location location;
    private final UUID uuid;
    private final Map<Integer, ArmorStand> hologramLines;
    private final Map<Integer, String> displayText;
    private String command;
    private LivingEntity clickable = null;

    // for loading from config
    public Hologram(UUID uuid, Location location, List<String> text, String command) {
        this.location = location;
        this.hologramLines = new HashMap<>();
        this.displayText = new HashMap<>();
        this.command = command;
        this.uuid = uuid;
        int line = 1;
        for (String s : text) {
            if (!s.equals("")) {
                displayText.put(line, s);
                line++;
            }
        }
        getCurrentEntities();
        if (location != null && !displayText.isEmpty())
            build();
    }

    public Hologram(Location location) {
        this.location = location;
        hologramLines = new HashMap<>();
        displayText = new HashMap<>();
        getCurrentEntities();
        this.command = null;
        uuid = UUID.randomUUID();
    }

    // for creating from item
    public Hologram(Location location, ItemStack item) {
        this.location = location;
        hologramLines = new HashMap<>();
        displayText = new HashMap<>();
        this.command = null;
        uuid = UUID.randomUUID();
        List<String> decodedText = decodeText(item.getItemMeta().getPersistentDataContainer()
                                                  .get(BghrApi.HOLOGRAM_TEXT_KEY, PersistentDataType.BYTE_ARRAY));
        setLines(decodedText);
        if (item.getItemMeta().getPersistentDataContainer().has(BghrApi.HOLOGRAM_COMMAND_KEY, PersistentDataType.STRING)) {
            String s = item.getItemMeta().getPersistentDataContainer().get(BghrApi.HOLOGRAM_COMMAND_KEY, PersistentDataType.STRING);
            if (s != null && !s.equals("")) {
                this.command = s;
                setClickable(command);
            }
        }
    }

    public Hologram(Player player, List<String> text, String command) {
        this.location = player.getLocation();
        hologramLines = new HashMap<>();
        displayText = new HashMap<>();
        for (String s : text) {
            if (s != null && !s.equals(""))
                displayText.put(text.indexOf(s) + 1, s);
        }
        this.command = command;
        uuid = UUID.randomUUID();
    }

    public void build() {
        for (Map.Entry<Integer, String> e : displayText.entrySet()) {
            hologramLines.put(e.getKey(), createArmorStand(e.getKey()));
        }
        if (command != null && !command.equals("")) {
            setClickable(command);
        }
    }

    private ArmorStand createArmorStand(int lineNumber) {
        final BGHR plugin = JavaPlugin.getPlugin(BGHR.class);
        Location placement = location.clone().add(0, 1 + (displayText.size() / 4D), 0);
        placement.add(0, -lineNumber / 4D, 0);

        ArmorStand as = (ArmorStand) placement.getWorld().spawnEntity(placement, EntityType.ARMOR_STAND);
        as.setDisabledSlots(EquipmentSlot.values());
        as.setInvulnerable(true);
        as.setPersistent(false);
        as.setCustomNameVisible(true);
        as.setInvisible(true);
        as.setSmall(true);
        as.setCanTick(false);
        as.setGravity(false);
        as.setCanMove(false);
        as.customName(Tools.componentalize(plugin.getApi().replacePlaceholders(displayText.get(lineNumber))));
        as.setMetadata(Metadata.HOLOGRAM_TAG.key(), new FixedMetadataValue(plugin, lineNumber));
        as.setMetadata(Metadata.HOLOGRAM_ID_TAG.key(), new FixedMetadataValue(plugin, uuid));

        return as;
    }

    private void getCurrentEntities() {
        for (LivingEntity e : location.getWorld().getNearbyLivingEntities(location, 5, 15, 5)) {
            if (!e.hasMetadata(Metadata.HOLOGRAM_ID_TAG.key()) || !e.getMetadata(Metadata.HOLOGRAM_ID_TAG.key()).get(0).value().equals(uuid))
                continue;
            if (e.hasMetadata(Metadata.HOLOGRAM_TAG.key()) && e instanceof ArmorStand) {
                int line = e.getMetadata(Metadata.HOLOGRAM_TAG.key()).get(0).asInt();
                hologramLines.put(line, (ArmorStand) e);
            }
            if (e.hasMetadata(BghrApi.HOLOGRAM_CLICKABLE))
                clickable = e;
        }
    }

    public boolean isClickable() {
        return command != null && !command.equals("");
    }

    public void setClickable(String command) {
        BGHR plugin = JavaPlugin.getPlugin(BGHR.class);
        this.command = command;
        clickable = (LivingEntity) location.getWorld().spawnEntity(location.clone().add(0, 1.5, 0), EntityType.RAVAGER);
        clickable.setMetadata(Metadata.HOLOGRAM_ID_TAG.key(), new FixedMetadataValue(plugin, uuid));
        clickable.setMetadata(Metadata.HOLOGRAM_TAG.key(), new FixedMetadataValue(plugin, 0));
        clickable.setMetadata(Metadata.HOLOGRAM_CLICKABLE.key(), new FixedMetadataValue(plugin, command));
        clickable.setInvulnerable(true);
        clickable.setPersistent(false);
        clickable.setInvisible(true);
        clickable.setAI(false);
        clickable.setCollidable(false);
        clickable.setCustomNameVisible(false);
        clickable.setSilent(true);
        clickable.setGravity(false);
    }

    private List<String> decodeText(byte[] encodedText) {
        List<String> decodedText = new ArrayList<>();

        try (ByteArrayInputStream is = new ByteArrayInputStream(encodedText);
             DataInputStream in = new DataInputStream(is)) {

            int length = Integer.parseInt(in.readUTF());

            for (int i = 0; i < length; i++) {
                String s = in.readUTF();
                decodedText.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return decodedText;
    }

    public void setLines(List<String> textLines) {
        displayText.clear();
        int i = 1;
        for (String s : textLines) {
            displayText.put(i, s);
            i++;
        }
    }

    public Location getLocation() {
        return location;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getDisplayText() {
        List<String> text = new ArrayList<>();
        List<Integer> lines = new ArrayList<>(displayText.keySet());
        lines.sort(Integer::compareTo);

        for (int line : lines) {
            text.add(displayText.get(line));
        }
        return text;
    }

    public UUID getId() {
        return uuid;
    }

    public void update() {
        for (Map.Entry<Integer, String> e : displayText.entrySet()) {
            String updatedLine = JavaPlugin.getPlugin(BGHR.class).getApi().replacePlaceholders(e.getValue());
            ArmorStand entity = hologramLines.get(e.getKey());
            if (updatedLine.equals("") && entity != null) {
                entity.remove();
                hologramLines.remove(e.getKey());
                continue;
            } else if (!updatedLine.equals("") && entity == null) {
                entity = createArmorStand(e.getKey());
                hologramLines.put(e.getKey(), entity);
            }
            if (entity != null)
                entity.customName(Tools.componentalize(updatedLine));
        }
    }

    public ItemStack createItem(String itemName) {
        ItemStack item = new ItemStack(Material.SEA_PICKLE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Trans.lateToComponent(itemName));
        meta.lore(Tools.componentalize(Tools.wrapText(Trans.late("&7Place this item where you would like to locate the hologram. After placed, you can right click it while crouching to reclaim it."), ChatColor.GRAY)));
        meta.getPersistentDataContainer().set(BghrApi.HOLOGRAM_TEXT_KEY, PersistentDataType.BYTE_ARRAY, encodeText());
        if (command != null && !command.equals(""))
            meta.getPersistentDataContainer().set(BghrApi.HOLOGRAM_COMMAND_KEY, PersistentDataType.STRING, command);

        item.setItemMeta(meta);
        return item;
    }

    private byte[] encodeText() {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(os)) {
            out.writeUTF(String.valueOf(displayText.size()));

            for (String line : displayText.values()) {
                out.writeUTF(line);
            }
            return os.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void remove() {
        for (Entity e : location.getNearbyLivingEntities(5, 5, 5)) {
            if (e.hasMetadata(Metadata.HOLOGRAM_ID_TAG.key())) {
                UUID uuid = (UUID) e.getMetadata(Metadata.HOLOGRAM_ID_TAG.key()).get(0).value();
                if (uuid != null && uuid.equals(this.uuid))
                    e.remove();
            }
        }
    }
}
