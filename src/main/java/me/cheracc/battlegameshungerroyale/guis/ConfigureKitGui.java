package me.cheracc.battlegameshungerroyale.guis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.PassiveAbility;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.WordUtils;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ConfigureKitGui extends Gui {
    Kit kit;
    Gui sendingGui;

    public ConfigureKitGui(Kit kit, @Nullable Gui sendingGui, HumanEntity player) {
        super(1, Trans.late("Configuring Kit: ") + kit.getName(), new HashSet<>(Arrays.asList(InteractionModifier.values())));
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
        Component name = Trans.lateToComponent("&eKit Name: &f%s", kit.getName());
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(Trans.late("")));
        lore.addAll(Tools.componentalize(Tools.wrapText(kit.getDescription(), ChatColor.GRAY)));
        lore.add(Component.text(""));
        lore.add(Trans.lateToComponent("&bLeft Click to change the name of this kit"));
        lore.add(Trans.lateToComponent("&bRight Click to change the description of this kit"));

        return ItemBuilder.from(Material.KNOWLEDGE_BOOK).name(name).lore(lore).flags(ItemFlag.HIDE_ATTRIBUTES)
            .asGuiItem(e -> {
                if (e.getWhoClicked() instanceof Player) {
                    Player p = (Player) e.getWhoClicked();
                    if (e.getClick().isLeftClick()) {
                        p.sendMessage(Tools.formatInstructions(Trans.late("    Enter a new name for this kit:"), kit.getName()));
                        TextInputListener.getInstance().getNextInputFrom(p, text -> {
                            kit.setName(text);
                            updateTitle(Trans.late("Configuring Kit: ") + kit.getName());
                            updateItem(e.getSlot(), nameAndDescriptionIcon());
                            open(p);
                        });
                    }
                    else if (e.getClick().isRightClick()) {
                        p.sendMessage(Tools.formatInstructions(Trans.late("Enter a new description for this kit. If you want to edit the current description, you can click on this message to enter it into the chat box"), kit.getDescription()));
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
        lore.add(Trans.late("&bClick here to change this kit's icon"));

        return ItemBuilder.from(kit.getIcon()).name(Trans.lateToComponent("&eKit Icon"))
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
        lore.add(Tools.BLANK_LINE);
        lore.addAll(kit.getEquipment().getDescription());

        if (lore.size() <= 1)
            lore.add(Trans.lateToComponent("  &7None"));

        lore.add(Tools.BLANK_LINE);
        lore.add(Trans.lateToComponent("&bClick to modify equipment"));

        return ItemBuilder.from(Material.ARMOR_STAND).name(Trans.lateToComponent("Kit Equipment:")).lore(lore)
                .asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    new EquipmentSetGui(e.getWhoClicked(), kit.getEquipment(), this, equipment -> {
                        kit.setEquipment(equipment);
                        updateItem(e.getSlot(), equipmentIcon());
                        new ConfigureKitGui(kit, null, e.getWhoClicked());
                    });
                });
    }

    private GuiItem existingAbilityIcon(Ability ability) {
        String iconName = Trans.late("&eAbility: &f") + ability.getName();
        String abilityType = (ability instanceof ActiveAbility) ? Trans.late("&aActive") : (ability instanceof PassiveAbility) ? Trans.late("&6Passive") : Trans.late("&bTriggered");
        Material icon = (ability instanceof ActiveAbility) ? Material.LIME_BANNER : Material.YELLOW_BANNER;
        List<String> lore = new ArrayList<>();
        if (ability.getCustomName() != null)
            lore.add(Trans.late("&eCustom Name: &f") + ability.getCustomName());
        lore.add(Trans.late("&eAbility Type: &f") + abilityType);
        lore.add("");
        lore.addAll(Tools.wrapText(ability.getDescription(), ChatColor.GRAY));
        lore.add("");
        lore.add(Trans.late("Ability Options:"));

        for (String s : ability.getConfig().getKeys(false)) {
            Object o = ability.getConfig().get(s);
            String value = "";
            if (o != null)
                value = o.toString();
            if (o instanceof Material)
                value = ((Material) o).name().toLowerCase();
            if (o instanceof ItemStack)
                value = WordUtils.capitalize(((ItemStack) o).getType().name().toLowerCase().replace("_", " "));
            if (o instanceof PotionEffect) {
                PotionEffect e = (PotionEffect) o;
                value = e.getType().getName().toLowerCase() + " " + Tools.integerToRomanNumeral(e.getAmplifier() + 1);
            }

            lore.addAll(Tools.wrapText(String.format("&6%s: &e%s", s, value), ChatColor.YELLOW));
        }

        lore.add("");
        lore.add(Trans.late("&bClick to MODIFY this ability"));
        lore.add(Trans.late("&bShift+Click to REMOVE this ability"));

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
        lore.add(Trans.late("&bClick here to cancel and discard all changes"));

        return ItemBuilder.from(Material.BARRIER).name(Trans.lateToComponent("&eCancel and Exit"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> e.getWhoClicked().closeInventory());
    }

    private GuiItem saveIcon() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(Trans.late("&bClick here to save these changes and update this kit"));

        return ItemBuilder.from(Material.WRITABLE_BOOK).name(Trans.lateToComponent("&eSave and Exit"))
                .lore(Tools.componentalize(lore)).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        KitManager kitManager = KitManager.getInstance();
                        Player p = (Player) e.getWhoClicked();
                        p.closeInventory();

                        kitManager.replaceKit(kit);
                        p.sendMessage(Trans.lateToComponent("&fYour changes to &e%s &fhave been saved and reloaded.", kit.getName()));
                        if (sendingGui != null)
                            sendingGui.open(p);
                    }
                });
    }

    private GuiItem newAbilityIcon() {
        String instructions = Trans.late("&bClick here to customize a new ability for this kit");

        return ItemBuilder.from(Material.ENCHANTED_GOLDEN_APPLE).name(Tools.componentalize(Trans.late("&eAdd an Ability")))
            .lore(Tools.componentalize(Tools.wrapText(instructions, ChatColor.AQUA))).asGuiItem(e ->
                new SelectAbilityGui(e.getWhoClicked(), this, ability -> new ConfigureAbilityGui(e.getWhoClicked(), ability, this, finalAbility -> {
                    kit.addAbility(finalAbility);
                    repopulateGui();
                    update();
                    open(e.getWhoClicked());
                })));
    }
}
