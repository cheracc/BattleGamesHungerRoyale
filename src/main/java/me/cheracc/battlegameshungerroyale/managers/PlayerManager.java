package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.datatypes.PlayerData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayerManager {
    private static PlayerManager singletonInstance = null;
    private final List<PlayerData> loadedPlayers = new ArrayList<>();

    private PlayerManager() {
    }

    public @NotNull PlayerData getPlayerData(Player player) {
        for (PlayerData d : loadedPlayers) {
            if (d.getPlayer().equals(player))
                return d;
        }
        PlayerData data = new PlayerData(player);
        loadedPlayers.add(data);
        return data;
    }

    public static PlayerManager getInstance() {
        if (singletonInstance == null)
            singletonInstance = new PlayerManager();
        return singletonInstance;
    }
}
