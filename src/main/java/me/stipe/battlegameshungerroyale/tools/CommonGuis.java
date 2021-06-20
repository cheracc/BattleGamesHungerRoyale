package me.stipe.battlegameshungerroyale.tools;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.ScrollingGui;
import me.stipe.battlegameshungerroyale.BGHR;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class CommonGuis {
    public static void sendMaterialSelectionGui(HumanEntity p, String title, String commandToDispatchOnSelection, String commandToDispatchOnCancel) {
        ScrollingGui gui = Gui.scrolling().title(Component.text(title)).rows(6).pageSize(45).create();

        gui.setDefaultClickAction(e -> e.setCancelled(true));
        gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Tools.componentalize("&bScroll Up")).asGuiItem(event -> gui.previous()));
        gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Tools.componentalize("&bScroll Down")).asGuiItem(event -> gui.next()));
        gui.setItem(6, 5, ItemBuilder.from(Material.BARRIER).name(Tools.componentalize("&bCancel and Go Back")).asGuiItem(event -> {
            event.getWhoClicked().closeInventory();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(event.getWhoClicked(), commandToDispatchOnCancel + " ");
                }
            }.runTaskLater(BGHR.getPlugin(), 1L);
        }));

        for (Material m : Material.values()) {
            if (m != null && !m.isAir() && m.isItem() && !m.name().contains("LEGACY")) {
                gui.addItem(ItemBuilder.from(m).name(Component.text(ChatColor.WHITE + m.name().toLowerCase())).asGuiItem(e -> {
                    if (e.getWhoClicked() instanceof Player) {
                        if (e.getCurrentItem() != null) {
                            String material = e.getCurrentItem().getType().name().toLowerCase();
                            p.closeInventory();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Bukkit.dispatchCommand(p, commandToDispatchOnSelection + " " + material);
                                    Bukkit.getLogger().info(commandToDispatchOnSelection + " " + material);
                                }
                            }.runTaskLater(BGHR.getPlugin(), 1L);
                        }
                    }
                }));
            }
        }
        gui.open(p);
    }

    public static void sendPotionEffectGui(HumanEntity player, boolean allowInstant, String commandToDispatchOnSave, String commandToDispatchOnCancel) {
        Gui gui = Gui.gui().title(Tools.componentalize("&0Customize Potion Effect")).rows(1).disableAllInteractions().create();
        Player p = (Player) player;

        final PotionEffect savedEffect;
        if (getSavedEffectFromPlayer(p) == null) {
            Bukkit.getLogger().warning(p.getName() + " is trying to modify an effect but doesn't have one saved");
            return;
        }
        else
            savedEffect = getSavedEffectFromPlayer(p);

        gui.addItem(ItemBuilder.from(Material.POTION).name(Tools.componentalize("&eEffect: &f" + savedEffect.getType().getName().toLowerCase()))
            .lore(Tools.componentalize("&bClick to modify this potion effect")).asGuiItem(e -> {
                e.getWhoClicked().closeInventory();
                sendPotionEffectTypeGui(p, allowInstant, commandToDispatchOnSave, commandToDispatchOnCancel);
            }));
        gui.setItem(1, amplifierIcon(p, gui));
        gui.setItem(2, particlesIcon(p, gui));
        gui.setItem(3, iconIcon(p, gui));

        gui.setItem(7, ItemBuilder.from(Material.WRITABLE_BOOK).name(Tools.componentalize("&eSave this effect"))
            .lore(Tools.componentalize(""), Tools.componentalize("&bClick here to save this effect and return to the ability configuration menu"))
            .asGuiItem(e -> {
                if (e.getWhoClicked() instanceof Player) {
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, commandToDispatchOnSave);
                }
            }));
        gui.setItem(8, ItemBuilder.from(Material.BARRIER).name(Tools.componentalize("&eCancel and go back"))
            .lore(Tools.componentalize(""), Tools.componentalize("&bClick here to cancel and return to the ability configuration menu"))
            .asGuiItem(e -> {
                if (e.getWhoClicked() instanceof Player) {
                    player.closeInventory();
                    Tools.removeObjectFromPlayer("potioneffect", p);
                    Bukkit.dispatchCommand(player, commandToDispatchOnCancel);
                }
            }));
        gui.open(p);
    }

    private static PotionEffect getSavedEffectFromPlayer(Player p) {
        Object o = Tools.getObjectFromPlayer("potioneffect", p);
        if (o instanceof PotionEffect)
            return (PotionEffect) o;
        return null;
    }

    private static GuiItem iconIcon(Player p, Gui gui) {
        final PotionEffect savedEffect;
        if (getSavedEffectFromPlayer(p) == null) {
            Bukkit.getLogger().warning(p.getName() + " is trying to modify an effect but doesn't have one saved");
            return null;
        }
        else
            savedEffect = getSavedEffectFromPlayer(p);

        return ItemBuilder.from(Material.BEACON).name(Tools.componentalize("&eShow Effect Icon: &f" + savedEffect.hasIcon()))
                .lore(Tools.componentalize(""),Tools.componentalize("&bClick to toggle icon"))
                .asGuiItem(e -> {
                    PotionEffect newEffect = new PotionEffect(savedEffect.getType(), Integer.MAX_VALUE, savedEffect.getAmplifier(), savedEffect.hasParticles(), savedEffect.hasParticles(), !savedEffect.hasIcon());
                    updateSavedPotionEffect(p, newEffect);
                    updateEffectGui(p, gui);
                });
    }


    private static GuiItem particlesIcon(Player p, Gui gui) {
        final PotionEffect savedEffect;
        if (getSavedEffectFromPlayer(p) == null) {
            Bukkit.getLogger().warning(p.getName() + " is trying to modify an effect but doesn't have one saved");
            return null;
        }
        else
            savedEffect = getSavedEffectFromPlayer(p);

        return ItemBuilder.from(Material.BEACON).name(Tools.componentalize("&eParticles: &f" + savedEffect.hasParticles()))
                .lore(Tools.componentalize(""),Tools.componentalize("&bClick to toggle effect particles"))
                .asGuiItem(e -> {
                    PotionEffect newEffect = new PotionEffect(savedEffect.getType(), Integer.MAX_VALUE, savedEffect.getAmplifier(), !savedEffect.hasParticles(), !savedEffect.hasParticles(), savedEffect.hasIcon());
                    updateSavedPotionEffect(p, newEffect);
                    updateEffectGui(p, gui);
                });
    }

    private static void updateEffectGui(Player p, Gui gui) {
        gui.updateItem(1, amplifierIcon(p, gui));
        gui.updateItem(2, particlesIcon(p, gui));
        gui.updateItem(3, iconIcon(p, gui));
    }

    private static GuiItem amplifierIcon(Player p, Gui gui) {
        final PotionEffect savedEffect;
        if (getSavedEffectFromPlayer(p) == null) {
            Bukkit.getLogger().warning(p.getName() + " is trying to modify an effect but doesn't have one saved");
            return null;
        }
        else
            savedEffect = getSavedEffectFromPlayer(p);

        return ItemBuilder.from(Material.BEACON).name(Tools.componentalize("&eCurrent Amplifier: &f" + savedEffect.getAmplifier()))
            .lore(Tools.componentalize(""),Tools.componentalize("&bClick to increase amplifier"), Tools.componentalize("&bRight Click to decrease amplifier"))
            .asGuiItem(e -> {
                int amplifier = savedEffect.getAmplifier();
                switch (e.getClick()) {
                    case LEFT:
                        if (amplifier < 5)
                            amplifier++;
                        break;
                    case RIGHT:
                        if (amplifier > 0)
                            amplifier--;
                        break;
                }
                PotionEffect newEffect = new PotionEffect(savedEffect.getType(), Integer.MAX_VALUE, amplifier, savedEffect.hasParticles(), savedEffect.hasParticles(), savedEffect.hasIcon());
                updateSavedPotionEffect(p, newEffect);
                updateEffectGui(p, gui);
            });
        }

    public static void sendPotionEffectTypeGui(Player p, boolean allowInstant, String commandToDispatchOnSave, String commandToDispatchOnCancel) {
        ScrollingGui gui = Gui.scrolling().title(Tools.componentalize("&0Select Effect Type")).rows(6).pageSize(45).disableAllInteractions().create();

        final PotionEffect savedEffect;
        if (getSavedEffectFromPlayer(p) == null) {
            Bukkit.getLogger().warning(p.getName() + " is trying to modify an effect but doesn't have one saved");
            return;
        }
        else
            savedEffect = getSavedEffectFromPlayer(p);

        gui.setDefaultClickAction(e -> e.setCancelled(true));
        gui.setItem(6, 3, ItemBuilder.from(Material.PAPER).name(Tools.componentalize("&bScroll Up")).asGuiItem(event -> gui.previous()));
        gui.setItem(6, 7, ItemBuilder.from(Material.PAPER).name(Tools.componentalize("&bScroll Down")).asGuiItem(event -> gui.next()));
        gui.setItem(6, 5, ItemBuilder.from(Material.BARRIER).name(Tools.componentalize("&bCancel and Go Back")).asGuiItem(event -> {
            event.getWhoClicked().closeInventory();
            new BukkitRunnable() {
                @Override
                public void run() {
                    sendPotionEffectGui(p, allowInstant, commandToDispatchOnSave, commandToDispatchOnCancel);
                }
            }.runTaskLater(BGHR.getPlugin(), 5L);
        }));

        for (PotionEffectType type : PotionEffectType.values()) {
            if (type.isInstant() && !allowInstant)
                continue;
            ItemStack potion = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) potion.getItemMeta();
            meta.addCustomEffect(savedEffect.withType(type), true);
            meta.setColor(type.getColor());
            potion.setItemMeta(meta);

            gui.addItem(ItemBuilder.from(potion).lore(Tools.componentalize(""), Tools.componentalize("&bClick to select this effect"))
                    .asGuiItem(e -> {
                        e.getWhoClicked().closeInventory();
                        updateSavedPotionEffect(p, savedEffect.withType(type));
                        sendPotionEffectGui(p, allowInstant, commandToDispatchOnSave, commandToDispatchOnCancel);
            }));
        }
        gui.open(p);
    }

    private static void updateSavedPotionEffect(Player p, PotionEffect effect) {
        Tools.removeObjectFromPlayer("potioneffect", p);
        Tools.saveObjectToPlayer("potioneffect", effect, p);
    }
}
