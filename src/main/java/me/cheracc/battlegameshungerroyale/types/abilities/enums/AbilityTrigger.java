package me.cheracc.battlegameshungerroyale.types.abilities.enums;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum AbilityTrigger implements AbilityOptionEnum, ConfigurationSerializable {
    DEAL_ANY_DAMAGE,
    DEAL_MELEE_HIT,
    DEAL_PROJECTILE_HIT,
    TAKE_ANY_DAMAGE,
    TAKE_MELEE_HIT,
    TAKE_PROJECTILE_HIT,
    KILL_ANYTHING,
    KILL_PLAYER,
    KILL_MONSTER,
    KILL_ANIMAL,
    DEATH_BY_ANY,
    DEATH_BY_PLAYER,
    OPEN_LOOT_CHEST;

    @Override
    public AbilityOptionEnum next() {
        int ordinal = this.ordinal() + 1;
        if (ordinal >= values().length)
            ordinal = 0;
        return values()[ordinal];
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name());
        return map;
    }

    public static AbilityTrigger deserialize(Map<String, Object> map) {
        return valueOf((String) map.get("name"));
    }
}
