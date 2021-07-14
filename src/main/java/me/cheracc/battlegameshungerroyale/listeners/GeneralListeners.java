package me.cheracc.battlegameshungerroyale.listeners;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.managers.GameManager;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.Game;
import me.cheracc.battlegameshungerroyale.types.Kit;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.PassiveAbility;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class GeneralListeners implements Listener {

    @EventHandler
    public void handlePlayerQuits(PlayerQuitEvent event) {
        PlayerData data = PlayerManager.getInstance().getPlayerData(event.getPlayer());
        if (data.getKit() != null)
            data.getKit().disrobePlayer(event.getPlayer());
        Game game = GameManager.getInstance().getPlayersCurrentGame(event.getPlayer());
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
    @EventHandler
    public void abilityItemsStayInHotbar(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();

            if (clicked != null) {
                if (!Ability.isAbilityItem(clicked)) {
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
        // change the join message to read "server" instead of "game"
        event.joinMessage(Tools.componentalize("&e" + event.getPlayer().getName() + " has joined the server."));

        // remove any kit/plugin items
        PlayerInventory inv = event.getPlayer().getInventory();
        for (int i = 0; i < inv.getSize() - 1; i++) {
            if (Tools.isPluginItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
        Player p = event.getPlayer();

        // remove any lingering "max duration" potion effects
        for (PotionEffect e : p.getActivePotionEffects()) {
            if (e.getDuration() > Integer.MAX_VALUE / 2)
                p.removePotionEffect(e.getType());
        }

        // set the gamemode based on config
        String gm = BGHR.getPlugin().getConfig().getString("main world.gamemode", "adventure");
        GameMode mode = GameMode.valueOf(gm.toUpperCase());
        p.setGameMode(mode);

        // set the scoreboard (delay this a few seconds to allow the player's settings to be loaded)

        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerData data = PlayerManager.getInstance().getPlayerData(p);
                if (data.getSettings().isShowMainScoreboard())
                    p.setScoreboard(GameManager.getInstance().getMainScoreboard());
                if (data.getSettings().getDefaultKit() != null) {
                    Kit kit = KitManager.getInstance().getKit(data.getSettings().getDefaultKit());
                    if (kit != null)
                        data.registerKit(kit, false);
                }
            }
        }.runTaskLater(BGHR.getPlugin(), 30L);
    }
}
