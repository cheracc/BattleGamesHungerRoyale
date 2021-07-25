package me.cheracc.battlegameshungerroyale.types.abilities.enums;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum RemoteAbility implements AbilityOptionEnum, ConfigurationSerializable {
    EXPLODE, TELEPORT_SELF;

    @Override
    public AbilityOptionEnum next() {
        int ordinal = this.ordinal() + 1;
        if (ordinal >= values().length)
            ordinal = 0;
        return values()[ordinal];
    }

    public static RemoteAbility deserialize(Map<String, Object> map) {
        return valueOf((String) map.get("name"));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> value = new HashMap<>();
        value.put("name", name());
        return value;
    }
}
