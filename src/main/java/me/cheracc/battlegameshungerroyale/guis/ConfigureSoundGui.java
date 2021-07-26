package me.cheracc.battlegameshungerroyale.guis;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.SoundEffect;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

public class ConfigureSoundGui extends Gui {
    SoundEffect soundEffect;
    Consumer<SoundEffect> callback;

    public ConfigureSoundGui(HumanEntity player, Gui sendingGui, SoundEffect currentSound, Consumer<SoundEffect> callback) {
        super(1, Trans.late("Configuring Sound Effect:"), new HashSet<>(Arrays.asList(InteractionModifier.values())));
        this.soundEffect = currentSound;
        if (soundEffect == null)
            soundEffect = new SoundEffect();
        this.callback = callback;
        disableAllInteractions();
        setOutsideClickAction(e -> {
            e.getWhoClicked().closeInventory();
            sendingGui.open(player);
        });

        fillGui();
        open(player);
    }

    private void fillGui() {
        addItem(soundItem());
        addItem(volumeItem());
        addItem(pitchItem());
        addItem(playSoundItem());
        setItem(8, saveItem());
    }

    private GuiItem soundItem() {
        return ItemBuilder.from(Material.MUSIC_DISC_CAT).name(Trans.lateToComponent("Sound: %s", soundEffect.getSound().name().toLowerCase()))
            .lore(Tools.BLANK_LINE, Trans.lateToComponent("&bClick to change")).asGuiItem(e -> {
                e.getWhoClicked().closeInventory();

                new SelectSoundGui(e.getWhoClicked(), this, null, sound -> {
                    soundEffect.setSound(sound);
                    updateItem(e.getSlot(), soundItem());
                    open(e.getWhoClicked());
                });
            });
    }

    private GuiItem volumeItem() {
        return ItemBuilder.from(Material.JUKEBOX).name(Trans.lateToComponent("&eCurrent Volume: %s", soundEffect.getVolume())).asGuiItem(event -> {
            float newValue = soundEffect.getVolume() * 10;

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
            newValue = (float) (Math.floor(newValue) / 10);
            soundEffect.setVolume(newValue);
            soundEffect.play((Player) event.getWhoClicked(), event.getWhoClicked().getLocation());
            updateItem(event.getSlot(), volumeItem());
        });
    }

    private GuiItem pitchItem() {
        return ItemBuilder.from(Material.JUKEBOX).name(Trans.lateToComponent("&eCurrent Pitch: %s", soundEffect.getPitch())).asGuiItem(event -> {
            float newValue = soundEffect.getPitch() * 10;

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
            newValue = (float) (Math.floor(newValue) / 10);
            soundEffect.setPitch(newValue);
            soundEffect.play((Player) event.getWhoClicked(), event.getWhoClicked().getLocation());
            updateItem(event.getSlot(), pitchItem());
        });
    }

    private GuiItem playSoundItem() {
        return ItemBuilder.from(Material.NOTE_BLOCK).name(Trans.lateToComponent("Play Sound")).asGuiItem(e -> {
            soundEffect.play((Player) e.getWhoClicked(), e.getWhoClicked().getLocation());
        });
    }

    private GuiItem saveItem() {
        return ItemBuilder.from(Material.WRITABLE_BOOK).name(Trans.lateToComponent("Save this Sound Effect")).asGuiItem(e -> {
            e.getWhoClicked().closeInventory();
            callback.accept(soundEffect);
        });
    }
}
