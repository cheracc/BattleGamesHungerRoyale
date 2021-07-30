package me.cheracc.battlegameshungerroyale.types.abilities;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface PassiveAbility {
    void activate(Player p);
    void deactivate(Player p);
    boolean hasToggleItem();
    ItemStack makeToggleItem();
    boolean isActive(Player p);

    default void givePlayerAbilityItem(Player player) {
        player.getInventory().setItem(Tools.getLastEmptyHotbarSlot(player), Tools.tagAsPluginItem(makeToggleItem()));
    }

}
