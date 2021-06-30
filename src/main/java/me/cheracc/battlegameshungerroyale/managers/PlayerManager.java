package me.cheracc.battlegameshungerroyale.managers;

import me.cheracc.battlegameshungerroyale.datatypes.PlayerData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayerManager {
    List<PlayerData> loadedPlayers = new ArrayList<>();

    public @NotNull PlayerData getPlayerData(Player player) {
        for (PlayerData d : loadedPlayers) {
            if (d.getPlayer().equals(player))
                return d;
        }
        PlayerData data = new PlayerData(player);
        loadedPlayers.add(data);
        return data;
    }

}
