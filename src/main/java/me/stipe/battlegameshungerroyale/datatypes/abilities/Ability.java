package me.stipe.battlegameshungerroyale.datatypes.abilities;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.Kit;
import me.stipe.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Ability implements Listener {
    public static final NamespacedKey ABILITY_KEY = new NamespacedKey(BGHR.getPlugin(), "ability_key");
    private final String description;
    private final String name;
    private Kit forKit = null;

    public Ability(String name, String description) {
        this.description = description;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Kit getAssignedKit() {
        return forKit;
    }

    public boolean isAssignedToAKit() {
        return forKit != null;
    }

    public void attachNewUuid(ItemMeta meta, String value) {
        meta.getPersistentDataContainer().set(ABILITY_KEY, PersistentDataType.STRING, value);
    }

    public static boolean isThisAnAbilityItem(ItemStack item) {
        return getUuid(item) != null;
    }

    public static @Nullable UUID getUuid(ItemStack item) {
        if (item == null)
            return null;

        if (item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().has(Ability.ABILITY_KEY, PersistentDataType.STRING)) {
            String uuidString = item.getItemMeta().getPersistentDataContainer().get(Ability.ABILITY_KEY, PersistentDataType.STRING);
            if (uuidString == null)
                return null;
            return UUID.fromString(uuidString);
        }
        return null;
    }

    public ItemStack makeItem(Material type, String name, String description, int cooldown) {
        ItemStack abilityItem = new ItemStack(type);
        ItemMeta meta = abilityItem.getItemMeta();
        List<Component> lore = new ArrayList<>();

        if (meta == null)
            return abilityItem;

        meta.displayName(Component.text(ChatColor.WHITE + name));
        attachNewUuid(meta, UUID.randomUUID().toString());

        lore.add(Component.text(""));
        lore.addAll(Tools.toC(Tools.wrapText(description, ChatColor.GRAY)));
        lore.add(Component.text(""));
        if (cooldown > 0)
            lore.add(Tools.toC("&7Cooldown: &f" + Tools.secondsToMinutesAndSeconds(cooldown)));

        meta.lore(lore);
        abilityItem.setItemMeta(meta);
        abilityItem.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        return abilityItem;
    }

    public abstract void load(ConfigurationSection section);

    public void load(ConfigurationSection section, Kit forKit) {
        this.forKit = forKit;
        load(section);
    }


    @EventHandler
    public void registerAllAblities(ServerLoadEvent event) {
        BGHR.getKitManager().registerAbility(this);
    }
}
