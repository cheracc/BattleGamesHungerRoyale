package me.cheracc.battlegameshungerroyale.guis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.listeners.TextInputListener;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.SoundEffect;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.AbilityOptionEnum;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public class ConfigureAbilityGui extends Gui {
    private final Ability ability;
    private final Consumer<Ability> callback;
    private final Gui sendingGui;
    private final BGHR plugin;
    private final TextInputListener textInputListener;

    public ConfigureAbilityGui(HumanEntity p, Ability ability, Gui sendingGui, BGHR plugin, TextInputListener textInputListener, Consumer<Ability> callback) {
        super(2, Trans.late("Edit Ability: ") + ability.getCustomName(), new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.ability = ability;
        this.callback = callback;
        this.sendingGui = sendingGui;
        this.plugin = plugin;
        this.textInputListener = textInputListener;
        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            sendingGui.open(e.getWhoClicked());
        });

        fillGui();
        open(p);
    }

    private void fillGui() {
        addItem(nameDescGuiItem());
        for (String s : ability.getConfig().getKeys(false)) {
            addItem(genericOptionGuiItem(s));
        }
        addItem(soundItem());
        setItem(16, cancelIcon());
        setItem(17, saveIcon());
    }

    private GuiItem nameDescGuiItem() {
        String name = ability.getCustomName();
        String itemName = Trans.late("&eAbility Name: &f") + name;
        List<String> lore = new ArrayList<>();

        lore.add("");
        lore.addAll(Tools.wrapText(ability.getDescription(), ChatColor.GRAY));
        lore.add("");
        lore.add(Trans.late("&bClick to change the name of this ability"));
        lore.add(Trans.late("&bRight click to change the description of this ability"));

        return ItemBuilder.from(Material.ITEM_FRAME).name(Tools.componentalize(itemName)).lore(Tools.componentalize(lore)).asGuiItem(e -> {
            if (e.getWhoClicked() instanceof Player) {
                Player p = (Player) e.getWhoClicked();
                p.closeInventory();
                if (e.getClick() == ClickType.RIGHT) {
                    Component message = Tools.formatInstructions(Trans.late("&eEnter a new value. You can also click on this message to enter the current value of this option in the chat box for easy editing."), ability.getDescription());
                    p.sendMessage(message);
                    textInputListener.getNextInputFrom(p, text -> {
                        ability.setOption("description", text);
                        updateItem(e.getSlot(), nameDescGuiItem());
                        open(p);
                    });
                }
                else {
                    Component message = Tools.formatInstructions(Trans.late("&eEnter a new value. You can also click on this message to enter the current value of this option in the chat box for easy editing."), ability.getCustomName());
                    p.sendMessage(message);
                    textInputListener.getNextInputFrom(p, text -> {
                        ability.setOption("custom name", text);
                        updateItem(e.getSlot(), nameDescGuiItem());
                        open(p);
                    });
                }
            }
        });
    }

    private GuiItem genericOptionGuiItem(String configOption) {
        String itemName = Trans.late("&eConfig Option: &f") + configOption;
        List<String> lore = new ArrayList<>();
        lore.add("");
        Object value = ability.getConfig().get(configOption);
        String valueString;
        Material icon = Material.LEVER;
        GuiAction<InventoryClickEvent> event;

        if (value instanceof Boolean) {
            valueString = Boolean.toString((Boolean) value);
            lore.add(Trans.late("&bClick to make ") + !(Boolean) value);
            if ((Boolean) value)
                icon = Material.REDSTONE_TORCH;
            event = handleBoolean(configOption, (Boolean) value);
        } else if (value instanceof Integer) {
            valueString = Integer.toString((Integer) value);
            lore.add(Trans.late("&bClick to increase ") + configOption);
            lore.add(Trans.late("&bRight Click to decrease ") + configOption);
            icon = Material.REPEATER;
            event = handleInteger(configOption, (Integer) value);
        } else if (value instanceof Double) {
            valueString = Double.toString((Double) value);
            lore.add(Trans.late("&bClick to increase ") + configOption);
            lore.add(Trans.late("&bRight Click to decrease ") + configOption);
            icon = Material.REPEATER;
            event = handleDouble(configOption, (Double) value);
        } else if (value instanceof String) {
            valueString = (String) value;
            icon = Material.PAPER;
            event = handleString(configOption, (String) value);
            if (configOption.contains("type")) {
                try {
                    Material material = Material.valueOf(((String) value).toUpperCase());
                    valueString = material.name().toLowerCase();
                    lore.add(Trans.late("&bClick to select a new ") + configOption);
                    icon = material;
                    event = handleMaterial(configOption);
                } catch (IllegalArgumentException ignored) {
                    lore.add(Trans.late("&bClick to enter a new ") + configOption);
                }
            }
        } else if (value instanceof PotionEffect) {
            PotionEffect effect = (PotionEffect) value;
            lore.add(Trans.late("&eEffect: &f") + effect.getType().getName());
            lore.add(Trans.late("&eAmplifier: &f") + effect.getAmplifier());
            lore.add(Trans.late("&eParticles: &f") + (effect.hasParticles() ? Trans.late("On") : Trans.late("Off")));
            lore.add(Trans.late("&eEffect Icon: &f") + (effect.hasIcon() ? Trans.late("On") : Trans.late("Off")));
            valueString = "";
            lore.add(Trans.late("&bClick to select a new ") + configOption);
            icon = Material.DRAGON_BREATH;
            event = handlePotionEffect(configOption, (PotionEffect) value);
        } else if (value instanceof ItemStack) {
            ItemStack item = (ItemStack) value;
            icon = item.getType();
            valueString = WordUtils.capitalize(item.getType().name().toLowerCase().replace("_", " "));
            lore.addAll(Tools.decomponentalize(item.lore()));
            event = handleItemStack(configOption, item);
        } else if (value instanceof AbilityOptionEnum) {
            AbilityOptionEnum abilityEnum = (AbilityOptionEnum) value;
            icon = Material.BOOKSHELF;
            valueString = abilityEnum.name().toLowerCase();
            lore.add(Trans.late("&bClick to change"));
            event = handleEnums(configOption, abilityEnum);
        } else
            return ItemBuilder.from(Material.BARRIER).asGuiItem();

        if (valueString.length() > 0)
            lore.add(0, Trans.late("&eCurrent value: &7") + valueString);

        return ItemBuilder.from(icon).name(Tools.componentalize(itemName)).lore(Tools.componentalize(lore))
                .flags(ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DYE, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE,
                        ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS).asGuiItem(event);
    }

    private GuiItem soundItem() {
        SoundEffect effect = ability.getSound();

        if (effect == null)
            return ItemBuilder.from(Material.MUSIC_DISC_11).name(Trans.lateToComponent("&eAdd a Sound Effect")).asGuiItem(e -> {
                e.getWhoClicked().closeInventory();
                new ConfigureSoundGui(e.getWhoClicked(), this, new SoundEffect(), soundEffect -> {
                    ability.setSound(soundEffect);
                    updateItem(e.getSlot(), soundItem());
                    open(e.getWhoClicked());
                });
            });

        return ItemBuilder.from(Material.JUKEBOX).name(Trans.lateToComponent("&eCurrent Sound Effect:")).lore(Tools.componentalize("  " + effect.getSound().name().toLowerCase()),
            Tools.BLANK_LINE, Trans.lateToComponent("&bClick to modify"), Trans.lateToComponent("&bRight click to remove"))
            .asGuiItem(e -> {
                if (e.getClick().isRightClick()) {
                    ability.setSound(null);
                    updateItem(e.getSlot(), soundItem());
                    return;
                }
                e.getWhoClicked().closeInventory();
                new ConfigureSoundGui(e.getWhoClicked(), this, effect, soundEffect -> {
                    ability.setSound(soundEffect);
                    updateItem(e.getSlot(), soundItem());
                    open(e.getWhoClicked());
                });
            });
    }

    private GuiItem saveIcon() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(Trans.late("&bClick here to save this ability"));

        return ItemBuilder.from(Material.WRITABLE_BOOK).name(Trans.lateToComponent("&eSave and Return to Kit Configuration"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        callback.accept(ability);
                    }
                });
    }

    private GuiItem cancelIcon() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(Trans.late("&bClick here to cancel and go back"));

        return ItemBuilder.from(Material.BARRIER).name(Trans.lateToComponent("&eCancel and Go Back"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();
                        sendingGui.open(p);
                    }
                });
    }

    private GuiAction<InventoryClickEvent> handleEnums(String configOption, AbilityOptionEnum currentValue) {
        return event -> {
            ability.setOption(configOption, currentValue.next());
            updateItem(event.getSlot(), genericOptionGuiItem(configOption));
        };
    }

    private GuiAction<InventoryClickEvent> handleBoolean(String configOption, boolean currentValue) {
        return event -> {
            ability.setOption(configOption, !currentValue);
            updateItem(event.getSlot(), genericOptionGuiItem(configOption));
        };
    }

    private GuiAction<InventoryClickEvent> handleInteger(String configOption, int currentValue) {
        return event -> {
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
            updateItem(event.getSlot(), genericOptionGuiItem(configOption));
        };
    }

    private GuiAction<InventoryClickEvent> handleDouble(String configOption, double currentValue) {
        return event -> {
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
            updateItem(event.getSlot(), genericOptionGuiItem(configOption));
        };
    }

    private GuiAction<InventoryClickEvent> handleString(String configOption, String currentValue) {
        return event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            Component message = Tools.formatInstructions(Trans.late("&eEnter a new value. You can also click on this message to enter the current value of this option in the chat box for easy editing."), currentValue);
            p.sendMessage(message);
            textInputListener.getNextInputFrom(p, text -> {
                ability.setOption(configOption, text);
                updateItem(event.getSlot(), genericOptionGuiItem(configOption));
                open(p);
            });
        };
    }

    private GuiAction<InventoryClickEvent> handleMaterial(String configOption) {
        return event -> {
            event.getWhoClicked().closeInventory();
            new SelectMaterialGui(event.getWhoClicked(),this, mat -> {
                ability.setOption(configOption, mat.name().toLowerCase());
                updateItem(event.getSlot(), genericOptionGuiItem(configOption));
                open(event.getWhoClicked());
            });
        };
    }

    private GuiAction<InventoryClickEvent> handlePotionEffect(String configOption, PotionEffect currentValue) {
        return event -> {
            event.getWhoClicked().closeInventory();
            new ConfigurePotionEffectGui(event.getWhoClicked(), currentValue, this, effect -> {
                ability.setOption(configOption, effect);
                updateItem(event.getSlot(), genericOptionGuiItem(configOption));
                open(event.getWhoClicked());
            });
        };
    }

    private GuiAction<InventoryClickEvent> handleItemStack(String configOption, ItemStack item) {
        return event -> {
            event.getWhoClicked().closeInventory();
            inputItem(item, configOption, event.getSlot(), newItem -> {
                ability.setOption(configOption, newItem);
                updateItem(event.getSlot(), genericOptionGuiItem(configOption));
                open(event.getWhoClicked());
            }).open(event.getWhoClicked());
        };
    }

    private Gui inputItem(ItemStack currentItem, String configOption, int slot, Consumer<ItemStack> callback) {
        Gui gui = Gui.gui().type(GuiType.DISPENSER).title(Trans.lateToComponent("&0Change Item")).create();
        gui.disableAllInteractions();
        gui.setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            open(e.getWhoClicked());
        });
        List<String> lore = new ArrayList<>(Tools.wrapText(Trans.late("&bDrop any item or item stack here. You can also click here ") +
                Trans.late("with an empty cursor to close this and give you time to create one."), ChatColor.AQUA));
        gui.setItem(4, ItemBuilder.from(currentItem).lore(Tools.componentalize(lore)).asGuiItem(e -> {
            ItemStack item = e.getWhoClicked().getItemOnCursor();
            if (item != null && !item.getType().isAir()) {
                callback.accept(item);
            } else {
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(Tools.formatInstructions(Trans.late("Create/Rename/Enchant the item you want to add to ") +
                        Trans.late("this kit's loot table. When you are finished, hold the item in your hand and type &e/abilityitem"), ""));
                e.getWhoClicked().setMetadata("ability_config_gui", new FixedMetadataValue(plugin, this));
                e.getWhoClicked().setMetadata("ability_config_option", new FixedMetadataValue(plugin, configOption));
                e.getWhoClicked().setMetadata("ability_config_slot", new FixedMetadataValue(plugin, slot));
            }
        }));

        return gui;
    }

    public void updateConfigOptionForSlot(String configOption, int slot, Object o) {
        ability.setOption(configOption, o);
        updateItem(slot, genericOptionGuiItem(configOption));
    }
}
