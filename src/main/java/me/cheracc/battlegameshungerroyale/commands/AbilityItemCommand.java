package me.cheracc.battlegameshungerroyale.commands;

import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.guis.ConfigureAbilityGui;
import me.cheracc.battlegameshungerroyale.tools.Logr;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

public class AbilityItemCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sender instanceof Player) {
            BGHR plugin = BGHR.getPlugin();
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
                    p.removeMetadata("ability_config", plugin);
                } else {
                    p.sendMessage(Tools.componentalize("Hold the item you wish to add in your main hand, then try typing &e/abilityitem &fagain"));
                }
            } else {
                Logr.info(slot + " " + gui.getClass().getSimpleName() + " " + configOption);
                p.sendMessage(Tools.componentalize("You don't seem to be editing a kit or ability item"));
            }
            p.removeMetadata("ability_config_gui", plugin);
            p.removeMetadata("ability_config_option", plugin);
            p.removeMetadata("ability_config_slot", plugin);
        }
        return true;
    }
}
