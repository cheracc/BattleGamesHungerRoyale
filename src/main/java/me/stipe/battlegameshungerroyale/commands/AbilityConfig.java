package me.stipe.battlegameshungerroyale.commands;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.datatypes.SoundEffect;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.tools.CommonGuis;
import me.stipe.battlegameshungerroyale.tools.Tools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AbilityConfig implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            Object o = Tools.getObjectFromPlayer("abilityconfig", p);
            Ability ability;
            if (o instanceof Ability)
                ability = (Ability) o;
            else
                return true;

            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("addsound")) {
                    o = Tools.getObjectFromPlayer("sound", p);
                    if (o instanceof SoundEffect)
                    ability.setSound((SoundEffect) o);
                    Tools.saveObjectToPlayer("abilityconfig", ability, p);
                    Bukkit.dispatchCommand(p, "abilityconfig");
                    return true;
                }
                if (args[0].equalsIgnoreCase("material") && args.length >= 2) {
                    String configOption = Tools.fieldNameToConfigOption(args[1]);
                    String material = args[2];
                    ability.setOption(configOption, material.toLowerCase());
                    Tools.saveObjectToPlayer("abilityconfig", ability, p);
                    Bukkit.dispatchCommand(p, "abilityconfig");
                    return true;
                }
                if (args[0].equalsIgnoreCase("name")) {
                    ability.setName(Tools.rebuildString(args, 1));
                    Tools.saveObjectToPlayer("abilityconfig", ability, p);
                    Bukkit.dispatchCommand(p, "abilityconfig");
                    return true;
                }
                if (args[0].equalsIgnoreCase("desc")) {
                    ability.setDescription(Tools.rebuildString(args, 1));
                    Tools.saveObjectToPlayer("abilityconfig", ability, p);
                    Bukkit.dispatchCommand(p, "abilityconfig");
                    return true;
                }
                if (args[0].equalsIgnoreCase("loadeffect")) {
                    String configOption = Tools.rebuildString(args, 1);
                    o = Tools.getObjectFromPlayer("potioneffect", p);
                    if (o instanceof PotionEffect) {
                        PotionEffect effect = (PotionEffect) o;
                        // update the ability being worked on
                        ability.setOption(configOption, effect);
                        Tools.saveObjectToPlayer("abilityconfig", ability, p);
                        Bukkit.dispatchCommand(p, "abilityconfig");
                        return true;
                    }
                }
                Set<String> fieldNames = new HashSet<>();
                for (String key : ability.getConfig().getKeys(false)) {
                    fieldNames.add(Tools.configOptionToFieldName(key).toLowerCase());
                }
                if (fieldNames.contains(args[0].toLowerCase())) {
                    String stringInput = Tools.rebuildString(args, 1);
                    ability.setOption(Tools.fieldNameToConfigOption(args[0]), stringInput);
                    Tools.saveObjectToPlayer("abilityconfig", ability, p);
                    Bukkit.dispatchCommand(p, "abilityconfig");
                    return true;
                }
            }
            sendGui(p, ability);

        }
        return true;
    }

    public Gui getGui(HumanEntity e) {
        Object o = Tools.getObjectFromPlayer("abilityGui", (Player) e);

        if (o instanceof Gui)
            return (Gui) o;
        return null;
    }

    public void sendGui(Player p, Ability ability) {
        String name = ability.getCustomName() != null ? ability.getCustomName() : ability.getName();
        String title = "&0Edit Ability: &1" + name;
        Gui gui = Gui.gui().title(Tools.componentalize(title)).rows(2).disableAllInteractions().create();

        gui.setCloseGuiAction(e -> Tools.removeObjectFromPlayer("abilityGui", (Player) e.getPlayer()));

        gui.addItem(nameDescGuiItem(ability));
        for (String s : ability.getConfig().getKeys(false)) {
            gui.addItem(genericOptionGuiItem(ability, s));
        }
        gui.addItem(soundItem(ability, gui));
        gui.setItem(16, cancelIcon(ability));
        gui.setItem(17, saveIcon(ability));
        gui.open(p);
        Tools.saveObjectToPlayer("abilityGui", gui, p);
    }

    private GuiItem soundItem(Ability ability, Gui gui) {
        SoundEffect effect = ability.getSound();
        if (effect == null)
            return ItemBuilder.from(Material.MUSIC_DISC_11).name(Tools.componentalize("&eAdd a Sound Effect")).asGuiItem(e -> {
                e.getWhoClicked().closeInventory();
                CommonGuis.soundEffectGui((Player) e.getWhoClicked(), "abilityconfig addsound", "abilityconfig").open(e.getWhoClicked());   });
        return ItemBuilder.from(Material.JUKEBOX).name(Tools.componentalize("&eCurrent Sound Effect:")).lore(Tools.componentalize("  " + effect.getSound().name().toLowerCase()),
                Tools.BLANK_LINE, Tools.componentalize("&eClick to modify"), Tools.componentalize("&eRight click to remove"))
                .asGuiItem(e -> {
                    if (e.getClick().isRightClick()) {
                        ability.setSound(null);
                        Tools.saveObjectToPlayer("abilityconfig", ability, (Player) e.getWhoClicked());
                        gui.updateItem(17, soundItem(ability, gui));
                        return;
                    }
                    e.getWhoClicked().closeInventory();
                    Tools.saveObjectToPlayer("sound", ability.getSound(), (Player) e.getWhoClicked());
                    CommonGuis.soundEffectGui((Player) e.getWhoClicked(), "abilityconfig addsound", "abilityconfig").open(e.getWhoClicked());   });
    }

    private GuiItem nameDescGuiItem(Ability ability) {
        String name = ability.getCustomName() != null ? ability.getCustomName() : ability.getName();
        String itemName = "&eAbility Name: &f" + name;
        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.addAll(Tools.wrapText(ability.getDescription(), ChatColor.GRAY));
        lore.add("");
        lore.add("&bClick to change the name of this ability");
        lore.add("&bRight click to change the description of this ability");

        return ItemBuilder.from(Material.ITEM_FRAME).name(Tools.componentalize(itemName)).lore(Tools.componentalize(lore)).asGuiItem(e -> {
            if (e.getWhoClicked() instanceof Player) {
                Player p = (Player) e.getWhoClicked();
                p.closeInventory();
                if (e.getClick() == ClickType.RIGHT) {
                    p.sendMessage(Tools.commandInstructions("/abilityconfig desc <description>", ability.getDescription() == null ? "" : ability.getDescription()));
                }
                else {
                    p.sendMessage(Tools.commandInstructions("/abilityconfig name <name>", ability.getName()));
                }
            }
        });
    }

    private GuiItem genericOptionGuiItem(Ability ability, String configOption) {
        String itemName = "&eConfig Option: &f" + configOption;
        List<String> lore = new ArrayList<>();
        lore.add("");
        Object value = ability.getConfig().get(configOption);
        String valueString;
        Material icon = Material.LEVER;
        GuiAction<InventoryClickEvent> event = null;

        if (value instanceof Boolean) {
            valueString = Boolean.toString((Boolean) value);
            lore.add("&bClick to make " + !(Boolean) value);
            if ((Boolean) value)
                icon = Material.REDSTONE_TORCH;
            event = handleBoolean(ability, configOption, (Boolean) value);
        } else if (value instanceof Integer) {
            valueString = Integer.toString((Integer) value);
            lore.add("&bClick to increase " + configOption);
            lore.add("&bRight Click to decrease " + configOption);
            icon = Material.REPEATER;
            event = handleInteger(ability, configOption, (Integer) value);
        } else if (value instanceof Double) {
            valueString = Double.toString((Double) value);
            lore.add("&bClick to increase " + configOption);
            lore.add("&bRight Click to decrease " + configOption);
            icon = Material.REPEATER;
            event = handleDouble(ability, configOption, (Double) value);
        } else if (value instanceof String) {
            valueString = (String) value;
            icon = Material.PAPER;
            event = handleString(ability, configOption, (String) value);
            try {
                Material material = Material.valueOf(((String) value).toUpperCase());
                valueString = material.name().toLowerCase();
                lore.add("&bClick to select a new " + configOption);
                icon = material;
                event = handleMaterial(ability, configOption, material);
            } catch (IllegalArgumentException ignored) {
                lore.add("&bClick to enter a new " + configOption);
            }
        } else if (value instanceof PotionEffect) {
            PotionEffect effect = (PotionEffect) value;
            lore.add("&eEffect: &f" + effect.getType().getName());
            lore.add("&eAmplifier: &f" + effect.getAmplifier());
            lore.add("&eParticles: &f" + (effect.hasParticles() ? "On" : "Off"));
            lore.add("&eEffect Icon: &f" + (effect.hasIcon() ? "On" : "Off"));
            valueString = "";
            lore.add("&bClick to select a new " + configOption);
            icon = Material.DRAGON_BREATH;
            event = handlePotionEffect(ability, configOption, (PotionEffect) value);
        }  else {
            valueString = "";
        }

        if (valueString.length() > 0)
            lore.add(0, "&eCurrent value: &7" + valueString);

        return ItemBuilder.from(icon).name(Tools.componentalize(itemName)).lore(Tools.componentalize(lore))
                .flags(ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DYE, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE,
                        ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS).asGuiItem(event);
    }

    private GuiItem saveIcon(Ability ability) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to save this ability");

        return ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave and Return to Kit Configuration"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        Object o = Tools.getObjectFromPlayer("kitconfig", p);
                        Kit kit;
                        if (o instanceof Kit) {
                            kit = (Kit) o;
                        }
                        else {
                            Bukkit.getLogger().warning(p.getName() + " tried to save an ability but is not editing a kit");
                            return;
                        }

                        kit.addAbility(ability);
                        Tools.removeObjectFromPlayer("kitconfig", p);
                        Tools.removeObjectFromPlayer("abilityconfig", p);
                        Tools.saveObjectToPlayer("kitconfig", kit, p);
                        Bukkit.dispatchCommand(p, "kitconfig");
                    }
                });
    }

    private GuiItem cancelIcon(Ability ability) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to cancel and go back");

        return ItemBuilder.from(Material.BARRIER).name(Tools.componentalize("&eCancel and Go Back"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        Object o = Tools.getObjectFromPlayer("kitconfig", p);
                        Kit kit;
                        if (o instanceof Kit) {
                            kit = (Kit) o;
                        }
                        else {
                            Bukkit.getLogger().warning(p.getName() + " tried to save an ability but is not editing a kit");
                            return;
                        }

                        kit.addAbility(ability);
                        Tools.removeObjectFromPlayer("kitconfig", p);
                        Bukkit.dispatchCommand(p, "kitconfig");
                    }
                });
    }

    private GuiAction<InventoryClickEvent> handleBoolean(Ability ability, String configOption, boolean currentValue) {
        return new GuiAction<>() {
            @Override
            public void execute(InventoryClickEvent event) {
                ability.setOption(configOption, !currentValue);
                getGui(event.getWhoClicked()).updateItem(event.getSlot(), genericOptionGuiItem(ability, configOption));
            }
        };
    }

    private GuiAction<InventoryClickEvent> handleInteger(Ability ability, String configOption, int currentValue) {
        return new GuiAction<>() {
            @Override
            public void execute(InventoryClickEvent event) {
                int newValue = currentValue;

                switch (event.getClick()) {
                    case LEFT:
                        newValue++;
                        break;
                    case RIGHT:
                        newValue--;
                        break;
                    case SHIFT_LEFT:
                        newValue += 10;
                        break;
                    case SHIFT_RIGHT:
                        newValue -= 10;
                        break;
                }

                ability.setOption(configOption, newValue);
                getGui(event.getWhoClicked()).updateItem(event.getSlot(), genericOptionGuiItem(ability, configOption));
            }
        };
    }

    private GuiAction<InventoryClickEvent> handleDouble(Ability ability, String configOption, double currentValue) {
        return new GuiAction<>() {
            @Override
            public void execute(InventoryClickEvent event) {
                double newValue = currentValue * 10;

                switch (event.getClick()) {
                    case LEFT:
                        newValue++;
                        break;
                    case RIGHT:
                        newValue--;
                        break;
                    case SHIFT_LEFT:
                        newValue += 10;
                        break;
                    case SHIFT_RIGHT:
                        newValue -= 10;
                        break;
                }

                newValue += 0.5;
                newValue = Math.floor(newValue) / 10;

                ability.setOption(configOption, newValue);
                getGui(event.getWhoClicked()).updateItem(event.getSlot(), genericOptionGuiItem(ability, configOption));
            }
        };
    }

    private GuiAction<InventoryClickEvent> handleString(Ability ability, String configOption, String currentValue) {
        return new GuiAction<>() {
            @Override
            public void execute(InventoryClickEvent event) {
                Player p = (Player) event.getWhoClicked();
                p.closeInventory();
                p.sendMessage(Tools.commandInstructions("/abilityconfig " + Tools.configOptionToFieldName(configOption), currentValue));
            }
        };
    }

    private GuiAction<InventoryClickEvent> handleMaterial(Ability ability, String configOption, Material currentValue) {
        return new GuiAction<>() {
            @Override
            public void execute(InventoryClickEvent event) {
                event.getWhoClicked().closeInventory();
                Tools.saveObjectToPlayer("abilityconfig", ability, (Player) event.getWhoClicked());
                CommonGuis.sendMaterialSelectionGui(event.getWhoClicked(), "Select a new " + configOption,
                        "abilityconfig material " + Tools.configOptionToFieldName(configOption), "abilityconfig");
            }
        };
    }

    private GuiAction<InventoryClickEvent> handlePotionEffect(Ability ability, String configOption, PotionEffect currentValue) {
        return new GuiAction<>() {
            @Override
            public void execute(InventoryClickEvent event) {
                event.getWhoClicked().closeInventory();
                Tools.saveObjectToPlayer("abilityconfig", ability, (Player) event.getWhoClicked());
                Tools.saveObjectToPlayer("potioneffect", currentValue, (Player) event.getWhoClicked());
                CommonGuis.sendPotionEffectGui(event.getWhoClicked(), false,
                        "abilityconfig loadeffect " + configOption, "abilityconfig");
            }
        };
    }


}
