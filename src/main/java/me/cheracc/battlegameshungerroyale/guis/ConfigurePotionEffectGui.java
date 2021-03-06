package me.cheracc.battlegameshungerroyale.guis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

public class ConfigurePotionEffectGui extends Gui {
    PotionEffect effect;
    Consumer<PotionEffect> callback;

    public ConfigurePotionEffectGui(HumanEntity player, PotionEffect effect, Gui sendingGui, Consumer<PotionEffect> callback) {
        super(1, Trans.late("Customize Potion Effect"), new HashSet<>(Arrays.asList(InteractionModifier.values())));
        if (effect != null)
            this.effect = effect;
        else
            this.effect = PotionEffectType.SPEED.createEffect(Integer.MAX_VALUE, 0);
        this.callback = callback;
        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            sendingGui.open(e.getWhoClicked());
        });

        fillGui();
        open(player);
    }

    private void fillGui() {
        addItem(effectTypeIcon());
        addItem(amplifierIcon());
        addItem(durationIcon());
        addItem(particlesIcon());
        addItem(ambientIcon());
        addItem(showIconIcon());
        setItem(8, saveEffectIcon());
    }

    private GuiItem effectTypeIcon() {
        Component name;
        PotionEffectType type = effect.getType();
        int amplifier = effect.getAmplifier();

        name = Trans.lateToComponent("&eEffect: &f%s", type.getName().toLowerCase());

        return ItemBuilder.from(Material.SPLASH_POTION).name(name).lore(Trans.lateToComponent("&bClick here to change the effect")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            new SelectPotionEffectTypeGui(e.getWhoClicked(), this, returnedType -> {
                int duration = effect.getDuration();
                if (returnedType.isInstant())
                    duration = 0;
                this.effect = returnedType.createEffect(duration, amplifier);
                updateItem(e.getSlot(), effectTypeIcon());
                open(e.getWhoClicked());
            }, true);
        });
    }

    private GuiItem amplifierIcon() {
        return ItemBuilder.from(Material.COMPARATOR).name(Trans.lateToComponent("&eCurrent Amplifier: &f%s", effect.getAmplifier()))
                .lore(Tools.componentalize(""),Trans.lateToComponent("&bClick to increase amplifier"), Trans.lateToComponent("&bRight Click to decrease amplifier"))
                .asGuiItem(e -> {
                    int amplifier = effect.getAmplifier();
                    switch (e.getClick()) {
                        case LEFT:
                            if (amplifier < 5)
                                amplifier++;
                            break;
                        case RIGHT:
                            if (amplifier > 0)
                                amplifier--;
                            break;
                        default:
                            return;
                    }
                    effect = effect.withAmplifier(amplifier);
                    updateItem(e.getSlot(), amplifierIcon());
                });
    }

    private GuiItem durationIcon() {
        return ItemBuilder.from(Material.CLOCK).name(Trans.lateToComponent("&eCurrent Effect Duration: &f%s", effect.getDuration() / 20))
                .lore(Component.space(),Trans.lateToComponent("&bClick to increase duration"), Trans.lateToComponent("&bRight Click to decrease duration"))
                .asGuiItem(e -> {
                    int duration = effect.getDuration();
                    switch (e.getClick()) {
                        case LEFT:
                            duration += 20;
                            break;
                        case RIGHT:
                            if (duration > 20)
                                duration -= 20;
                            else
                                duration = 0;
                            break;
                        default:
                            return;
                    }
                    effect = effect.withDuration(duration);
                    updateItem(e.getSlot(), durationIcon());
                });
    }

    private GuiItem particlesIcon() {
        return ItemBuilder.from(Material.BEACON).name(Trans.lateToComponent("&eParticles: &f%s", effect.hasParticles()))
                .lore(Trans.lateToComponent("&bClick to toggle whether this effect shows particles"))
                .asGuiItem(e -> {
                    effect = effect.withParticles(!effect.hasParticles());
                    updateItem(e.getSlot(), particlesIcon());
                });
    }

    private GuiItem ambientIcon() {
        return ItemBuilder.from(Material.BEACON).name(Tools.componentalize(Trans.late("&eAmbient Effect: &f") + effect.isAmbient()))
                .lore(Tools.componentalize(Tools.wrapText(Trans.late("&bClick to toggle whether this effect shows ambient particles. (Ambient particles are more translucent and not as obvious - this has no effect if particles are turned off)"), ChatColor.AQUA)))
                .asGuiItem(e -> {
                    effect = effect.withAmbient(!effect.isAmbient());
                    updateItem(e.getSlot(), ambientIcon());
                });
    }

    private GuiItem showIconIcon() {
        return ItemBuilder.from(Material.PAINTING).name(Tools.componentalize(Trans.late("&eShow Effect Icon: &f") + effect.hasIcon()))
                .lore(Tools.componentalize(Tools.wrapText(Trans.late("&bClick to toggle whether the target of this effect sees an effect icon for the effect"), ChatColor.AQUA)))
                .asGuiItem(e -> {
                    effect = effect.withIcon(!effect.hasIcon());
                    updateItem(e.getSlot(), showIconIcon());
                });
    }

    private GuiItem saveEffectIcon() {
        return ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize(Trans.late("&eSave this Effect")))
                .asGuiItem(e -> {
                    e.getWhoClicked().closeInventory();
                    callback.accept(effect);
                });
    }

}
