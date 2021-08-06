package me.cheracc.battlegameshungerroyale.types.games;

import me.cheracc.battlegameshungerroyale.tools.Trans;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class Hungergames extends Game implements InvincibilityPhase, BorderPhase {
    public Hungergames(GameOptions options) {
        super(options);
    }

    public Hungergames() {
        super();
    }

    @Override
    public String getGameTypeName() {
        return Trans.late("Battle Royale: Hungergames!");
    }

    @Override
    public String getGameDescription() {
        return "Players start equally distributed around the spawn location. After an optional invincibility phase, the game is a free-for-all until there is one player remaining.";
    }

    @Override
    public Material getGameIcon() {
        return Material.CHEST;
    }

    private void doHungergamesSpawn(Consumer<Boolean> callback) {
        closeGameToPlayers();
        List<Location> spawns = getXSpawnPoints(getActivePlayers().size());

        if (getLootManager() != null)
            getLootManager().placeLootChests((int) (getActivePlayers().size() * 5 * Math.sqrt(getMap().getBorderRadius())));
        if (spawns.size() < getActivePlayers().size()) {
            api.logr().warn("not enough spawns");
            getXSpawnPoints(getActivePlayers().size());
        }

        Collections.shuffle(spawns);
        int count = 0;

        PotionEffect noMove = new PotionEffect(PotionEffectType.SLOW, 50, 99999, false, false, false);
        PotionEffect noJump = new PotionEffect(PotionEffectType.JUMP, 50, -99999, false, false, false);
        for (Player p : getActivePlayers()) {
            p.teleport(spawns.get(count), PlayerTeleportEvent.TeleportCause.PLUGIN);
            p.addPotionEffect(noMove);
            p.addPotionEffect(noJump);
            count++;
        }
        callback.accept(true);
    }

    @Override
    public void onGameStart() {
        doHungergamesSpawn(success -> {
        });
    }
}
