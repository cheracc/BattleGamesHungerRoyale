package me.cheracc.battlegameshungerroyale.types.abilities;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ActiveAbility {
    boolean doAbility(Player source);
    ItemStack createAbilityItem();
    int getCooldown();

    default void givePlayerAbilityItem(Player player) {
        player.getInventory().setItem(Tools.getLastEmptyHotbarSlot(player), Tools.tagAsPluginItem(createAbilityItem()));
    }
}
