package me.stipe.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.stipe.battlegameshungerroyale.managers.KitManager;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfigureKitGui extends Gui {
    Kit kit;
    Gui sendingGui;

    public ConfigureKitGui(Kit kit, @Nullable Gui sendingGui, HumanEntity player) {
        super(1, Tools.componentalize("&0Configuring Kit: &1" + kit.getName()));
        this.kit = kit;
        this.sendingGui = sendingGui;

        disableAllInteractions();
        if (sendingGui == null)
            setOutsideClickAction(e -> e.getWhoClicked().closeInventory());
        else
            setOutsideClickAction(e -> sendingGui.open(e.getWhoClicked()));

        populateGui();
        open(player);
    }

    private void populateGui() {
        addItem(nameAndDescriptionIcon());
        addItem(kitIcon());
        addItem(equipmentIcon());
        setItem(7, cancelIcon());
        setItem(8, saveIcon());

        for (Ability ability : kit.getAbilities())
            addItem(existingAbilityIcon(ability));

        if (getGuiItems().entrySet().size() < 9) {
            addItem(newAbilityIcon());
        }
    }

    private void repopulateGui() {
        for (int i = 8; i >= 0; i--)
            removeItem(i);
        populateGui();
    }

    private GuiItem nameAndDescriptionIcon() {
        Component name = Tools.componentalize("&eKit Name: &f" + kit.getName());
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(""));
        lore.addAll(Tools.componentalize(Tools.wrapText(kit.getDescription(), ChatColor.GRAY)));
        lore.add(Component.text(""));
        lore.add(Tools.componentalize("&bLeft Click to change the name of this kit"));
        lore.add(Tools.componentalize("&bRight Click to change the description of this kit"));

        return ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(name).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES)
            .asGuiItem(e -> {
                if (e.getWhoClicked() instanceof Player) {
                    Player p = (Player) e.getWhoClicked();
                    if (e.getClick().isLeftClick()) {
                        p.sendMessage(Tools.componentalize("Enter a new name:"));
                        TextInputListener.getInstance().getNextInputFrom(p, text -> {
                            kit.setName(text);
                            updateTitle(Tools.componentalize("&0Configuring Kit: &1" + kit.getName()));
                            updateItem(e.getSlot(), nameAndDescriptionIcon());
                            open(p);
                        });
                    }
                    else if (e.getClick().isRightClick()) {
                        TextInputListener.getInstance().getNextInputFrom(p, text -> {
                            kit.setDescription(text);
                            updateItem(e.getSlot(), nameAndDescriptionIcon());
                            open(p);
                        });
                    }
                    else
                        return;
                    p.closeInventory();
                }
            });
    }

    private GuiItem kitIcon() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to change this kit's icon");

        return ItemBuilder.from(kit.getIcon()).name(Tools.componentalize("&eKit Icon"))
            .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                if (e.getWhoClicked() instanceof Player) {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    // send another gui to get an icon
                    new SelectMaterialGui(p,this, mat -> {
                        kit.setIcon(mat);
                        updateItem(e.getSlot(), kitIcon());
                        open(p);
                    });
                }
            });
    }

    private GuiItem equipmentIcon() {
        List<Component> lore = new ArrayList<>();
        boolean noGear = true;
        lore.add(Tools.BLANK_LINE);


        if (kit.getEquipment() != null && !kit.getEquipment().getArmor().isEmpty()) {
            lore.add(Tools.componentalize("&e&nArmor:"));
            noGear = false;
            for (ItemStack item : kit.getEquipment().getArmor().values()) {
                if (item == null) continue;
                lore.add(Tools.componentalize(item.getI18NDisplayName()));
            }
        }
        if (kit.getEquipment() != null && !kit.getEquipment().getOtherItems().isEmpty()) {
            lore.add(Tools.componentalize("&e&nOther Starting Items:"));
            noGear = false;
            for (ItemStack item : kit.getEquipment().getOtherItems()) {
                if (item == null) continue;
                lore.add(Tools.componentalize(item.getI18NDisplayName()));
            }
        }
        if (noGear)
            lore.add(Tools.componentalize("&7None"));
        lore.add(Tools.BLANK_LINE);
        lore.add(Tools.componentalize("&bClick to modify equipment"));

        return ItemBuilder.from(Material.ARMOR_STAND).name(Tools.componentalize("Kit Equipment:")).lore(lore)
                .asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    new EquipmentSetGui(e.getWhoClicked(), kit.getEquipment(), this, equipment -> {
                        kit.setEquipment(equipment);
                        new ConfigureKitGui(kit, sendingGui, e.getWhoClicked());
                    });
                });
    }

    private GuiItem existingAbilityIcon(Ability ability) {
        String iconName = "&eAbility: &f" + ability.getName();
        String abilityType = (ability instanceof ActiveAbility) ? "&aActive" : "&6Passive";
        Material icon = (ability instanceof ActiveAbility) ? Material.LIME_BANNER : Material.YELLOW_BANNER;
        List<String> lore = new ArrayList<>();
        if (ability.getCustomName() != null)
            lore.add("&eCustom Name: &f" + ability.getCustomName());
        lore.add("&eAbility Type: &f" + abilityType);
        lore.add("");
        lore.addAll(Tools.wrapText(ability.getDescription(), ChatColor.GRAY));
        lore.add("");
        lore.add("Ability Options:");

        for (String s : ability.getConfig().getKeys(false)) {
            Object o = ability.getConfig().get(s);
            String value = o.toString();
            if (o instanceof Material)
                value = ((Material) o).name().toLowerCase();
            if (o instanceof PotionEffect) {
                PotionEffect e = (PotionEffect) o;
                value = e.getType().getName().toLowerCase() + " " + Tools.integerToRomanNumeral(e.getAmplifier() + 1);
            }

            lore.addAll(Tools.wrapText(String.format("&6%s: &e%s", s, value), ChatColor.YELLOW));
        }

        lore.add("");
        lore.add("&bClick to MODIFY this ability");
        lore.add("&bShift+Click to REMOVE this ability");

        return ItemBuilder.from(icon).name(Tools.componentalize(iconName)).lore(Tools.componentalize(lore)).asGuiItem(e -> {
            if (e.getWhoClicked() instanceof Player) {
                Player p = (Player) e.getWhoClicked();

                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    kit.removeAbility(ability);
                    repopulateGui();
                    update();
                    return;
                }

                p.closeInventory();
                kit.removeAbility(ability);
                new ConfigureAbilityGui(e.getWhoClicked(), ability, this, abilityResponse -> {
                    kit.addAbility(abilityResponse);
                    repopulateGui();
                    update();
                    open(p);
                });
            }
        });
    }

    private GuiItem cancelIcon() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to cancel and discard all changes");

        return ItemBuilder.from(Material.BARRIER).name(Tools.componentalize("&eCancel and Exit"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> e.getWhoClicked().closeInventory());
    }

    private GuiItem saveIcon() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bClick here to save these changes and update this kit");

        return ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave and Exit"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        KitManager kitManager = BGHR.getKitManager();
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();

                        kitManager.replaceKit(kit);
                        p.sendMessage(Tools.componentalize("&fYour changes to &e" + kit.getName() + " &fhave been saved and reloaded."));
                    }
                });
    }

    private GuiItem newAbilityIcon() {
        String instructions = "&bClick here to customize a new ability for this kit";

        return ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eAdd an Ability"))
            .lore(Tools.componentalize(Tools.wrapText(instructions, ChatColor.AQUA))).asGuiItem(e ->
                new SelectAbilityGui(e.getWhoClicked(), this, ability -> {
                    new ConfigureAbilityGui(e.getWhoClicked(), ability, this, finalAbility -> {
                        kit.addAbility(finalAbility);
                        repopulateGui();
                        update();
                        open(e.getWhoClicked());
                    });
                }));
    }
}
