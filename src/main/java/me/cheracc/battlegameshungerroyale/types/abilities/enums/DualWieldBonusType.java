package me.cheracc.battlegameshungerroyale.types.abilities.enums;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum DualWieldBonusType implements AbilityOptionEnum, ConfigurationSerializable {
    DAMAGE_BONUS, ATTACK_SPEED_BONUS, MOVEMENT_SPEED_BONUS;

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

    public DualWieldBonusType deserialize(Map<String, Object> map) {
        return valueOf((String) map.get("name"));
    }
}
