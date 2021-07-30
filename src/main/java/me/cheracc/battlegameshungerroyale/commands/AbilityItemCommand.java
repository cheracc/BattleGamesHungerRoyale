package me.cheracc.battlegameshungerroyale.commands;
import me.cheracc.battlegameshungerroyale.BghrApi;
import me.cheracc.battlegameshungerroyale.guis.ConfigureAbilityGui;
import me.cheracc.battlegameshungerroyale.tools.Trans;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

public class AbilityItemCommand implements CommandExecutor {
    private final BghrApi api;

    public AbilityItemCommand(BghrApi api) {
        this.api = api;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            int slot = -1;
            ConfigureAbilityGui gui = null;
            String configOption = null;
            MetadataValue value = p.getMetadata("ability_config_gui").get(0);
            if (value.value() instanceof ConfigureAbilityGui)
                gui = (ConfigureAbilityGui) value.value();
            value = p.getMetadata("ability_config_option").get(0);
            if (value.value() instanceof String)
                configOption = value.asString();
            value = p.getMetadata("ability_config_slot").get(0);
            if (value.value() instanceof Integer)
                slot = value.asInt();
            if (slot >= 0 && gui != null && configOption != null) {
                ItemStack item = p.getInventory().getItemInMainHand();
                if (item != null && item.getType().isItem()) {
                    gui.updateConfigOptionForSlot(configOption, slot, item);
                    gui.open(p);
                    p.removeMetadata("ability_config", api.getPlugin());
                } else {
                    p.sendMessage(Trans.lateToComponent("Hold the item you wish to add in your main hand, then try typing &e/abilityitem &fagain"));
                }
            } else {
                api.logr().info(slot + " " + gui.getClass().getSimpleName() + " " + configOption);
                p.sendMessage(Trans.lateToComponent("You don't seem to be editing a kit or ability item"));
            }
            p.removeMetadata("ability_config_gui", api.getPlugin());
            p.removeMetadata("ability_config_option", api.getPlugin());
            p.removeMetadata("ability_config_slot", api.getPlugin());
        }
        return true;
    }
}
