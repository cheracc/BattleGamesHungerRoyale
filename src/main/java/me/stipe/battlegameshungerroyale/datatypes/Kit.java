package me.stipe.battlegameshungerroyale.datatypes;

import me.stipe.battlegameshungerroyale.BGHR;
import me.stipe.battlegameshungerroyale.datatypes.abilities.Ability;
import me.stipe.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.stipe.battlegameshungerroyale.managers.KitManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Kit {
    String name;
    String description;
    Material icon;
    List<Ability> abilities = new ArrayList<>();

    public Kit(FileConfiguration config) {
        name = config.getString("name", "");
        description = config.getString("description", "");
        icon = Material.valueOf(config.getString("icon", "chest").toLowerCase());
        if (config.contains("abilities"))
            loadAbilities(Objects.requireNonNull(config.getConfigurationSection("abilities")));
    }

    public void outfitPlayer(Player p, PlayerData data) {
        for (Ability a : abilities) {
            if (a instanceof ActiveAbility) {
                ItemStack abilityItem = ((ActiveAbility) a).createAbilityItem();
                p.getInventory().setItem(getLastEmptyHotbarSlot(p), abilityItem);
                data.registerAbilityItem(this, abilityItem);
            }
        }
    }

    private int getLastEmptyHotbarSlot(Player p) {
        for (int i = 8; i >= 0; i--) {
            ItemStack item = p.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR)
                return i;
        }
        return -1;
    }

    private void loadAbilities(ConfigurationSection section) {
        KitManager kits = BGHR.getPlugin().getKitManager();
        Set<String> keys = section.getKeys(false);

        for (Ability a : kits.getGenericAbilities()) {
            if (keys.contains(a.getName())) {
                keys.remove(a.getName());
                a.load(section.getConfigurationSection(a.getName()));
            }
        }
    }

    public String getName() {
        return name;
    }

}
