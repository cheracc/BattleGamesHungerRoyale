package me.cheracc.battlegameshungerroyale.listeners;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.PlayerData;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.PassiveAbility;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

public class GeneralListeners implements Listener {
    private static GeneralListeners singletonInstance = null;

    private GeneralListeners() {
        Bukkit.getPluginManager().registerEvents(this, BGHR.getPlugin());
    }

    public static void start() {
        if (singletonInstance == null)
            singletonInstance = new GeneralListeners();
    }

    @EventHandler
    public void handlePlayersChangingWorlds(PlayerTeleportEvent event) {
        // don't care if the teleport isn't changing worlds
        if (event.getFrom().getWorld().equals(event.getTo().getWorld()))
            return;

        Player p = event.getPlayer();
        PlayerData pData = PlayerManager.getInstance().getPlayerData(p);

        // check if player is leaving a game or loaded map and handle it - this should only happen if admins are using /tp commands.
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN && MapManager.getInstance().isThisAGameWorld(event.getFrom().getWorld())) {
            Bukkit.dispatchCommand(p, "quit");
        }

        // check if player is transferring TO a main world FROM a game world
        if (!MapManager.getInstance().isThisAGameWorld(p.getWorld()) && MapManager.getInstance().isThisAGameWorld(event.getFrom().getWorld())) {
            GameMode defaultGameMode = GameMode.valueOf(BGHR.getPlugin().getConfig().getString("main world.gamemode", "adventure").toUpperCase());
            p.setGameMode(defaultGameMode);
            pData.resetInventory();
            if (BGHR.getPlugin().getConfig().getBoolean("main world.place players at spawn on join", false) ||
                    pData.getLastLocation() == null || !pData.getLastLocation().getWorld().equals(event.getTo().getWorld())) {
                Bukkit.getLogger().info("teleporting to lobby spawn");
                p.teleport(p.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            } else {
                Bukkit.getLogger().info("returning to previous location");
                p.teleport(pData.getLastLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                pData.setLastLocation(null);
            }
        }

        // here they are going FROM a main world TO a game world
        else if (!MapManager.getInstance().isThisAGameWorld(event.getFrom().getWorld()) &&
                  MapManager.getInstance().isThisAGameWorld(event.getTo().getWorld())) {
            pData.saveInventory();
            pData.setLastLocation(p.getLocation());
            p.getInventory().clear();
            for (ItemStack item : p.getInventory().getArmorContents())
                if (item != null)
                    item.setType(Material.AIR);
            if (pData.getKit() != null)
                pData.getKit().outfitPlayer(p);
        }
    }

    // makes players keep their ability items (and not drop them) when they die
    @EventHandler
    public void keepKitItems(PlayerDeathEvent event) {
        Player p = event.getEntity();
        PlayerData data = PlayerManager.getInstance().getPlayerData(p);

        if (data.getKit() != null) {
            for (ItemStack item : p.getInventory()) {
                if (Tools.isPluginItem(item)) {
                    event.getItemsToKeep().add(item);
                    event.getDrops().remove(item);
                    if (p.hasCooldown(item.getType()))
                        p.setCooldown(item.getType(), 0);
                }
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

            PlayerData data = PlayerManager.getInstance().getPlayerData(p);
            Ability ability = Ability.getFromItem(activeItem);
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

    // attempts to catch any stray plugin items and removes them
    @EventHandler
    public void clearAnyPluginItemsOnJoin(PlayerJoinEvent event) {
        PlayerInventory inv = event.getPlayer().getInventory();
        for (int i = 0; i < inv.getSize() - 1; i++) {
            if (Tools.isPluginItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
        Player p = event.getPlayer();
        for (PotionEffect e : p.getActivePotionEffects()) {
            if (e.getDuration() > Integer.MAX_VALUE / 2)
                p.removePotionEffect(e.getType());
        }
        String gm = BGHR.getPlugin().getConfig().getString("main world.gamemode", "adventure");
        GameMode mode = GameMode.valueOf(gm.toUpperCase());
        p.setGameMode(mode);
    }
}
