package me.cheracc.battlegameshungerroyale.listeners;

import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import me.cheracc.battlegameshungerroyale.types.PlayerData;
import me.cheracc.battlegameshungerroyale.types.abilities.Ability;
import me.cheracc.battlegameshungerroyale.types.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.PassiveAbility;
import me.cheracc.battlegameshungerroyale.types.games.Game;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class GeneralListeners implements Listener {
    private final BghrApi api;

    public GeneralListeners(BghrApi api) {
        this.api = api;
    }

    @EventHandler
    public void handlePlayerQuits(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        PlayerData data = api.getPlayerManager().getPlayerData(p);
        if (data.getKit() != null)
            api.getPlayerManager().disrobePlayer(p, data.getKit());

        if (!api.getGameManager().isInAGame(p))
            data.saveInventory(false);

        Game game = api.getGameManager().getPlayersCurrentGame(p);
        if (game != null)
            game.quit(event.getPlayer());
    }

    // makes players keep their ability items (and not drop them) when they die
    @EventHandler
    public void keepKitItems(PlayerDeathEvent event) {
        Player p = event.getEntity();
        PlayerData data = api.getPlayerManager().getPlayerData(p);

        if (data.getKit() != null) {
            for (ItemStack item : p.getInventory()) {
                if (Tools.isPluginItem(item)) {
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
        Player p = event.getPlayer();
        PlayerData data = api.getPlayerManager().getPlayerData(p);
        ItemStack item = p.getInventory().getItemInMainHand();
        Ability ability = api.getKitManager().getAbilityFromItem(item);

        if (item == null || ability == null) {
            item = p.getInventory().getItemInOffHand();
            ability = api.getKitManager().getAbilityFromItem(item);
        }

        if (p == null || item == null || ability == null)
            return;

        event.setUseInteractedBlock(Event.Result.ALLOW);
        if (item.getType() != Material.SHIELD)
            event.setUseItemInHand(Event.Result.DENY);
        else
            event.setUseItemInHand(Event.Result.ALLOW);

        if (p.hasCooldown(item.getType()))
            return;
        if (!data.hasKit(ability.getAssignedKit()))
            return;

        if (ability instanceof ActiveAbility && event.getAction().name().contains("RIGHT")) {
            ActiveAbility activeAbility = (ActiveAbility) ability;

            if (activeAbility.doAbility(p)) {
                p.setCooldown(item.getType(), activeAbility.getCooldown() * 20);
                data.getStats().addActiveAbilityUsed();
                data.setModified(true);
            }
        } else if (ability instanceof PassiveAbility && event.getAction().name().contains("RIGHT")) {
            PassiveAbility passiveAbility = (PassiveAbility) ability;

            if (passiveAbility.isActive(p))
                passiveAbility.deactivate(p);
            else
                passiveAbility.activate(p);
            p.setCooldown(item.getType(), 20);
        }
    }

    // these listeners are looking for items tied to abilities and ensuring that they stay in the hotbar.
    @EventHandler(ignoreCancelled = true)
    public void abilityItemsStayInHotbar(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();

            if (clicked != null) {
                if (api.getKitManager().isAbilityItem(clicked)) {
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

        if (api.getKitManager().isAbilityItem(item)) {
            event.setCancelled(true);
            p.sendMessage(Component.text("That's kind of important, you should keep it."));
        }
    }

    // attempt to grab a player's data before they actually log in
    @EventHandler
    public void loadDataAtPreLogin(AsyncPlayerPreLoginEvent event) {
        api.getPlayerManager().loadPlayerDataAsync(event.getUniqueId());
    }

    // processes a player when they join
    @EventHandler
    public void loadPlayersOnJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        PlayerManager pm = api.getPlayerManager();

        // change the join message to read "server" instead of "game"
        event.joinMessage(Trans.lateToComponent("&e%s has joined the server", event.getPlayer().getName()));

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
        String gm = api.getPlugin().getConfig().getString("main world.gamemode", "adventure");
        GameMode mode = GameMode.valueOf(gm.toUpperCase());
        p.setGameMode(mode);

        if (pm.isPlayerDataLoaded(p.getUniqueId()))
            pm.restorePlayerFromSavedData(p, pm.getPlayerData(p));

        if (api.getPlugin().getConfig().getBoolean("main world.place players at spawn on join", false))
            p.teleport(api.getMapManager().getLobbyWorld().getSpawnLocation());
    }
}
