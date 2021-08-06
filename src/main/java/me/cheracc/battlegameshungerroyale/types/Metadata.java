package me.cheracc.battlegameshungerroyale.types;

import me.cheracc.battlegameshungerroyale.BGHR;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;

public enum Metadata {
    HOLOGRAM_TAG,
    HOLOGRAM_ID_TAG,
    HOLOGRAM_CLICKABLE,
    KILLER,
    PLACED_BY_PLAYER,
    PRE_ELYTRA,
    PREGAME_HEALTH,
    PREGAME_FOOD_LEVEL,
    PREGAME_SCOREBOARD;

    public static void removeAll(Metadatable thing) {
        for (Metadata key : values())
            thing.removeMetadata(key.key(), JavaPlugin.getPlugin(BGHR.class));
    }

    public String key() {
        return name();
    }
}
