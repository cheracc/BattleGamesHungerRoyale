package me.stipe.battlegameshungerroyale.datatypes;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SoundEffect implements ConfigurationSerializable {
    Sound sound;
    float pitch;
    float volume;

    public SoundEffect() {
        sound = Sound.ENTITY_PLAYER_BURP;
        pitch = 0;
        volume = 0;
    }

    public SoundEffect(Map<String, Object> serialized) {
        this.sound = Sound.valueOf(((String) serialized.get("sound")).toUpperCase());
        this.pitch = (float)(double) serialized.get("pitch");
        this.volume = (float)(double) serialized.get("volume");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> serializedSound = new HashMap<>();
        serializedSound.put("sound", sound.name().toLowerCase());
        serializedSound.put("pitch", pitch);
        serializedSound.put("volume", volume);
        return serializedSound;
    }

    public void play(Player p, Location l) {
        p.playSound(l, sound, volume, pitch);
    }

    public void play(Location l) {
        l.getWorld().playSound(l, sound, volume, pitch);
    }

    public Sound getSound() {
        return sound;
    }

    public void setSound(Sound sound) {
        this.sound = sound;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

}
