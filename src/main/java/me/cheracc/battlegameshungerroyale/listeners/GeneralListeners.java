package me.cheracc.battlegameshungerroyale.listeners;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.PassiveAbility;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class GeneralListeners implements Listener {
    private final BGHR plugin;
    public GeneralListeners(BGHR plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void handlePlayerQuits(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        PlayerData data = PlayerManager.getInstance().getPlayerData(p);
        if (data.getKit() != null)
            data.getKit().disrobePlayer(p);

        if (!GameManager.getInstance().isInAGame(p))
            data.saveInventory(false);

        Game game = GameManager.getInstance().getPlayersCurrentGame(p);
        if (game != null)
            game.quit(event.getPlayer());
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
            GameMode defaultGameMode = GameMode.valueOf(plugin.getConfig().getString("main world.gamemode", "adventure").toUpperCase());
            p.setGameMode(defaultGameMode);
            pData.setLastLocation(event.getFrom());
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
            if (activeItem == null)
                return;
            if (!Ability.isAbilityItem(activeItem))
                activeItem = p.getInventory().getItemInMainHand();
            if (!Ability.isAbilityItem(activeItem))
                activeItem = p.getInventory().getItemInOffHand();
            if (!Ability.isAbilityItem(activeItem))
                return;

            PlayerData data = PlayerManager.getInstance().getPlayerData(p);
            Ability ability = Ability.getFromItem(activeItem);

            if (ability == null)
                return;

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
    @EventHandler (ignoreCancelled = true)
    public void abilityItemsStayInHotbar(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();

            if (clicked != null) {
                if (Ability.isAbilityItem(clicked)) {
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

        if (Ability.isAbilityItem(item)) {
            event.setCancelled(true);
            p.sendMessage(Component.text("That's kind of important, you should keep it."));
        }
    }

    // attempt to grab a player's data before they actually log in
    @EventHandler
    public void loadDataAtPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerManager.getInstance().getPlayerData(event.getUniqueId());
    }

    // processes a player when they join
    @EventHandler
    public void loadPlayersOnJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        // change the join message to read "server" instead of "game"
        event.joinMessage(Tools.componentalize("&e" + event.getPlayer().getName() + " has joined the server"));

        // clear the player's inventory (we will reload it from what was saved)
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.getEnderChest().clear();

        // remove any lingering "max duration" potion effects
        for (PotionEffect e : p.getActivePotionEffects()) {
            if (e.getDuration() > Integer.MAX_VALUE / 2)
                p.removePotionEffect(e.getType());
        }

        // set the gamemode based on config
        String gm = plugin.getConfig().getString("main world.gamemode", "adventure");
        GameMode mode = GameMode.valueOf(gm.toUpperCase());
        p.setGameMode(mode);

        if (PlayerManager.getInstance().isPlayerDataLoaded(p))
            PlayerManager.getInstance().getPlayerData(p).restorePlayer();

        if (plugin.getConfig().getBoolean("main world.place players at spawn on join", false))
            p.teleport(MapManager.getInstance().getLobbyWorld().getSpawnLocation());
    }

    // Inventory handling listener
    @EventHandler
    public void saveOrReloadInventoryWhenChangingWorlds(PlayerChangedWorldEvent event) {
        final World from = event.getFrom();
        final World to = event.getPlayer().getWorld();

        final boolean enteringGameFromMainWorlds = MapManager.getInstance().isThisAGameWorld(to) && !MapManager.getInstance().isThisAGameWorld(from);
        final boolean leavingGameToMainWorlds = MapManager.getInstance().isThisAGameWorld(from) && !MapManager.getInstance().isThisAGameWorld(to);

        if (enteringGameFromMainWorlds) {
            Player p = event.getPlayer();
            PlayerData data = PlayerManager.getInstance().getPlayerData(p);

            data.saveInventory(true);
        }

        if (leavingGameToMainWorlds) {
            Player p = event.getPlayer();
            PlayerData data = PlayerManager.getInstance().getPlayerData(p);

            data.restorePlayer();
        }
    }

}
