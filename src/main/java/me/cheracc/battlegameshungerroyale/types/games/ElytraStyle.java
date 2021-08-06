package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.events.GameChangedPhaseEvent;
import me.cheracc.battlegameshungerroyale.types.Metadata;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

public class ElytraStyle extends Game implements InvincibilityPhase, BorderPhase {
    public ElytraStyle(GameOptions options) {
        super(options);
    }

    public ElytraStyle() {
        super();
    }

    @Override
    public String getGameTypeName() {
        return "Battle Royale: Elytra Style!";
    }

    @Override
    public String getGameDescription() {
        return "At the start of the game, players are given an elytra and launched into the air to glide back down to their desired location. The elytra is removed at the end of the invincibility phase, so make sure it is long enough!";
    }

    @Override
    public Material getGameIcon() {
        return Material.ELYTRA;
    }

    @Override
    protected void onTick() {
        forEachActivePlayer(p -> {
            if (!p.isGliding() && p.getLocation().getBlock().getRelative(BlockFace.DOWN).isSolid())
                removeElytra(p);
        });
    }

    private void doElytraSpawn(Consumer<Boolean> callback) {
        closeGameToPlayers();
        Vector boost = new Vector(0, 1, 0);

        getWorld().setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, true);

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                for (Player p : getActivePlayers()) {
                    if (!isPlaying(p))
                        return;

                    if (count == 0) {
                        p.setVelocity(boost);
                    } else {
                        p.setVelocity(p.getVelocity().add(boost));
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1F, 1F + (0.1F * 2 * count));

                    if (count == 2) {
                        ItemStack elytra = new ItemStack(Material.ELYTRA);
                        elytra.addEnchantment(Enchantment.BINDING_CURSE, 1);
                        ItemStack current = p.getInventory().getChestplate();
                        if (current != null && !current.getType().isAir())
                            p.setMetadata(Metadata.PRE_ELYTRA.key(), new FixedMetadataValue(api.getPlugin(), current));
                        p.getInventory().setChestplate(elytra);
                        p.setGliding(true);
                    }
                    if (count >= 4) {
                        cancel();
                        callback.accept(true);
                    }
                }
                count++;
            }
        }.runTaskTimer(api.getPlugin(), 40L, 4L);
    }

    private void removeElytra(Player player) {
        if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType().equals(Material.ELYTRA)) {
            if (player.hasMetadata(BghrApi.PRE_ELYTRA) && player.getMetadata(BghrApi.PRE_ELYTRA).get(0).value() instanceof ItemStack) {
                player.getInventory().setChestplate((ItemStack) player.getMetadata(BghrApi.PRE_ELYTRA).get(0).value());
                player.removeMetadata(Metadata.PRE_ELYTRA.key(), api.getPlugin());
            } else
                player.getInventory().setChestplate(null);
            player.setGliding(false);
            player.setAllowFlight(false);
        }
    }

    @Override
    public void onGameStart() {
        doElytraSpawn(success -> {
            setPhase(GamePhase.INVINCIBILITY);
            getWorld().setGameRule(GameRule.DISABLE_ELYTRA_MOVEMENT_CHECK, false);
            new GameChangedPhaseEvent(this, "invincibility").callEvent();
        });
    }

    @Override
    public void atEndOfInvincibilityPhase() {
        for (Player p : getActivePlayers()) {
            removeElytra(p);
        }
    }
}
