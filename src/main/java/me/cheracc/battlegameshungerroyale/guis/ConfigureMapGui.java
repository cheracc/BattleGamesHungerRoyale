package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.datatypes.MapData;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ConfigureMapGui extends Gui {
    private final MapData map;
    private final Gui sendingGui;

    public ConfigureMapGui(HumanEntity player, Gui sendingGui, MapData map) {
        super(1, Tools.componentalize("&0Configure Map: " + map.getMapName()));
        this.map = map;
        this.sendingGui = sendingGui;

        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            if (sendingGui != null)
                sendingGui.open(player);
        });

        fillGui();
        open(player);
    }

    public void fillGui() {
        setItem(0, nameAndDescriptionIcon());
        setItem(1, centerIcon());
        setItem(2, borderIcon());
        setItem(3, spawnBlockIcon());
        setItem(4, spawnCenterIcon());
        setItem(5, spawnRadiusIcon());
        setItem(8, saveQuitIcon());
    }

    private void setBorderFromConfig(World world) {
        WorldBorder border = world.getWorldBorder();
        border.setSize(map.getBorderRadius() * 2);
        border.setCenter(map.getBorderCenter(world));
    }

    public GuiItem nameAndDescriptionIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(Tools.componentalize("&e" + map.getMapName()));
        List<String> lore = new ArrayList<>();

        lore.add("&fCreator: &7" + map.getMapCreator());
        lore.addAll(Tools.wrapText("&fDescription: &2" + map.getMapDescription(), ChatColor.DARK_GREEN));
        lore.add("");
        lore.add("&bClick to change name");
        lore.add("&bRight click to change description");
        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            if (e.isLeftClick()) {
                Tools.formatInstructions("Type a new name for this map in the chat window: ", map.getMapName());
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), text -> {
                    map.setName(text);
                    e.getWhoClicked().closeInventory();
                    updateItem(0, nameAndDescriptionIcon());
                    open(e.getWhoClicked());
                });
            }
            if (e.isRightClick()) {
                Tools.formatInstructions("Type a new description for this map in the chat window. " +
                        "You can click this message to load the current description so that you may edit it.", map.getMapDescription());
                TextInputListener.getInstance().getNextInputFrom((Player) e.getWhoClicked(), text -> {
                    map.setDescription(text);
                    e.getWhoClicked().closeInventory();
                    updateItem(0, nameAndDescriptionIcon());
                    open(e.getWhoClicked());
                });
            }

        });
    }

    public GuiItem borderIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.GLASS).name(Tools.componentalize("&eBorder: &f" + (map.isUseBorder() ? "on" : "off")));
        List<String> lore = new ArrayList<>();
        if (map.isUseBorder())
            lore.add("&fBorder Radius: &7" + map.getBorderRadius());

        lore.add("");
        if (map.isUseBorder())
            lore.add("&bClick to turn border on");
        else
            lore.add("&bClick to increase border size");
        if (map.getBorderRadius() > 0) {
            lore.add("&bRight click to decrease");
            lore.add("&7(Set to zero to turn border off)");
        }
        icon.lore(Tools.componentalize(lore));

        return icon.asGuiItem(e -> {
            int current = map.getBorderRadius();
            if (e.isLeftClick()) {
                if (!map.isUseBorder())
                    map.toggleUseBorder();
                current += 10;
            }
            if (e.isRightClick() && map.isUseBorder() && map.getBorderRadius() >= 0) {
                current -= 10;
                if (current <= 0) {
                    current = 0;
                    map.toggleUseBorder();
                }
            }
            map.setBorderRadius(current);
            setBorderFromConfig(e.getWhoClicked().getWorld());
            updateItem(2, borderIcon());
        });
    }

    public GuiItem centerIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.CONDUIT).name(Tools.componentalize("&eBorder Center"));
        icon.lore(Tools.componentalize(Tools.wrapText("Click here to set the center at your location. You can also stand where you want the center to be and type &f/mapconfig &fbordercenter&7. Right click to strike the current center with lightning.", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            if (e.isRightClick()) {
                Location center = map.getBorderCenter(e.getWhoClicked().getWorld());
                e.getWhoClicked().getWorld().strikeLightningEffect(center);
                return;
            }
            map.setBorderCenter(e.getWhoClicked().getLocation());
            e.getWhoClicked().sendMessage(Tools.componentalize("Center of border set to your location."));
        });
    }

    private GuiItem spawnBlockIcon() {
        Material type = map.getSpawnBlockType();
        if (type == null || type.isAir() || !type.isItem())
            type = Material.DIRT;
        Bukkit.getLogger().info(type.name());
        ItemBuilder icon = ItemBuilder.from(type).name(Tools.componentalize("&eSpawn Point Block"));
        icon.lore(Tools.componentalize(Tools.wrapText("Games will look for this block type to spawn players on. Click to open a gui and select a new block type. You can also stand on a spawn point and type /mapconfig spawn", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new SelectMaterialGui(e.getWhoClicked(), this, mat -> {
                map.setSpawnBlockType(mat);
                e.getWhoClicked().closeInventory();
                updateItem(3, spawnBlockIcon());
                open(e.getWhoClicked());
            });
        });
    }

    public GuiItem spawnRadiusIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.ENDER_EYE).name(Tools.componentalize("&eSpawn Radius: &f" + map.getSpawnRadius()));
        icon.lore(Tools.componentalize(Tools.wrapText("How far away players will spawn from the center. Click to increase, Right click to decrease", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            int current = map.getSpawnRadius();
            if (e.isLeftClick() && current < 24) {
                current++;
            }
            if (e.isRightClick() && current > 5) {
                current--;
            }
            map.setSpawnRadius(current);
            updateItem(5, spawnRadiusIcon());
            visualizeRadius((Player) e.getWhoClicked());
        });
    }

    public GuiItem spawnCenterIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.PLAYER_HEAD).name(Tools.componentalize("&eSpawn Center"));
        icon.lore(Tools.componentalize(Tools.wrapText("Click here to set the center of spawn at your location. You can also stand where you want the center to be and type &f/mapconfig &fspawncenter&7. Right click to strike the current center with lightning.", ChatColor.GRAY)));

        return icon.asGuiItem(e -> {
            if (e.isRightClick()) {
                Location center = map.getSpawnCenter(e.getWhoClicked().getWorld());
                e.getWhoClicked().getWorld().strikeLightningEffect(center);
                return;
            }
            map.setSpawnCenter(e.getWhoClicked().getLocation());
            e.getWhoClicked().sendMessage(Tools.componentalize("Center of spawn set to your location."));
        });
    }

    private void visualizeRadius(Player p) {
        Location center = map.getSpawnCenter(p.getWorld());
        int radius = map.getSpawnRadius();
        double angle = 2 * Math.PI / 100;

        for (int i = 0; i < 100; i++) {
            Location l = center.clone().add(radius * Math.cos(angle * i), 0, radius * Math.sin(angle * i));
            p.spawnParticle(Particle.DRIP_LAVA, l, 10, 0.2, 0.2, 0.2);
        }
    }

    public GuiItem saveQuitIcon() {
        ItemBuilder icon = ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("Save this Map Configuration"));
        icon.lore(Tools.componentalize("&bRight click to close without saving"));

        return icon.asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            if (e.isLeftClick()) {
                map.saveConfig();
            }
            if (sendingGui != null)
                sendingGui.open(e.getWhoClicked());
        });
    }

}
