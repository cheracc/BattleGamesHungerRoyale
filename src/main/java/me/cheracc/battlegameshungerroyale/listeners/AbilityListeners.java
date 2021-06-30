package me.cheracc.battlegameshungerroyale.listeners;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.PlayerData;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.PassiveAbility;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class AbilityListeners implements Listener {
    PlayerManager playerManager = BGHR.getPlayerManager();

    // makes players keep their ability items (and not drop them) when they die
    @EventHandler
    public void KeepKitItems(PlayerDeathEvent event) {
        Player p = event.getEntity();
        PlayerData data = playerManager.getPlayerData(p);

        if (data.getKit() != null) {
            for (ItemStack item : data.getAbilityItems()) {
                event.getItemsToKeep().add(item);
                event.getDrops().remove(item);
                if (p.hasCooldown(item.getType()))
                    p.setCooldown(item.getType(), 0);
            }
        }
    }

    // detects when a player uses an ability item and executes the ability
    @EventHandler
    public void onUseActiveAbility(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT")) {
            Player p = event.getPlayer();
            ItemStack activeItem = p.getActiveItem();
            if (Tools.getUuidFromItem(activeItem) == null)
                activeItem = p.getInventory().getItemInMainHand();
            if (Tools.getUuidFromItem(activeItem) == null)
                activeItem = p.getInventory().getItemInOffHand();
            if (Tools.getUuidFromItem(activeItem) == null)
                return;

            PlayerData data = playerManager.getPlayerData(p);
            Ability ability = data.getAbilityFromItem(activeItem);
            event.setCancelled(true);

            if (!data.hasKit(ability.getAssignedKit())) {
                Bukkit.getLogger().warning(p.getName() + " has an item for kit " + ability.getAssignedKit().getName() + " but has kit " + data.getKit().getName());
                return;
            }

            if (ability instanceof ActiveAbility && activeItem != null && !p.hasCooldown(activeItem.getType())) {
                ActiveAbility activeAbility = (ActiveAbility) ability;
                if (activeAbility.doAbility(p))
                    p.setCooldown(activeItem.getType(), activeAbility.getCooldown() * 20);
            }

            else if (ability instanceof PassiveAbility && activeItem != null) {
                PassiveAbility passiveAbility = (PassiveAbility) ability;
                if (passiveAbility.isActive(p))
                    passiveAbility.deactivate(p);
                else
                    passiveAbility.activate(p);
                p.setCooldown(activeItem.getType(), 10);
            }
        }
    }

    // these listeners are looking for items tied to abilities and ensuring that they stay in the hotbar.
    @EventHandler
    public void abilityItemsStayInHotbar(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();

            if (clicked != null) {
                if (Tools.getUuidFromItem(clicked) != null) {
                    p.sendMessage(Component.text("Ability items must remain in your hotbar or offhand. You can use the 'swap hands' (default 'F') button to move them around"));
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void doNotDropAbilityItems(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (Tools.getUuidFromItem(item) != null) {
            event.setCancelled(true);
            p.sendMessage(Component.text("That's kind of important, you should keep it."));
        }
    }
}
