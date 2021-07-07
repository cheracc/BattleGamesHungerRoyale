package me.cheracc.battlegameshungerroyale.guis;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.InteractionModifier;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.datatypes.Kit;
import me.cheracc.battlegameshungerroyale.datatypes.PlayerData;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.Ability;
import me.cheracc.battlegameshungerroyale.datatypes.abilities.ActiveAbility;
import me.cheracc.battlegameshungerroyale.managers.KitManager;
import me.cheracc.battlegameshungerroyale.managers.MapManager;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SelectKitGui extends Gui {
    public SelectKitGui(HumanEntity player) {
        super(KitManager.getInstance().getLoadedKits().size() / 9 + 1, "Select a Kit", InteractionModifier.VALUES);
        setOutsideClickAction(e -> e.getWhoClicked().closeInventory());

        fillGui();

        open(player);
    }

    private void fillGui() {
        int slot = 0;
        for (Kit kit : KitManager.getInstance().getLoadedKits()) {
            setItem(slot, kitIcon(slot, kit));
            slot++;
        }

    }

    private GuiItem kitIcon(int slot, Kit kit) {
        ItemBuilder icon = ItemBuilder.from(kit.getIcon()).name(Tools.componentalize("&e" + kit.getName()));

        icon.flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_PLACED_ON);
        List<String> lore = new ArrayList<>(Tools.wrapText("&7" + kit.getDescription(), ChatColor.GRAY));
        if (!kit.getAbilities().isEmpty()) {
            lore.add("&eSpecial Abilities:");
            for (Ability a : kit.getAbilities()) {
                String color = (a instanceof ActiveAbility) ? "&a" : "&6";
                String abilityString = String.format("%s%s &f- &7%s", color, a.getCustomName() != null ? a.getCustomName() : a.getName(),
                        a.getDescription());
                lore.addAll(Tools.wrapText(abilityString, ChatColor.GRAY));
            }
            lore.add(" ");
        }
        if (!kit.getEquipment().isEmpty()) {
            lore.add("&eStarting Equipment:");
            for (ItemStack item : kit.getEquipment().getAllItems()) {
                lore.add(LegacyComponentSerializer.legacyAmpersand().serialize(item.displayName()));
            }
        }
        lore.add("");
        lore.add("&bClick to select this kit");
        icon.lore(Tools.componentalize(lore));
        return icon.asGuiItem(e -> {
            Player p = (Player) e.getWhoClicked();
            PlayerData data = PlayerManager.getInstance().getPlayerData(p);
            data.registerKit(kit, false);
            if (MapManager.getInstance().isThisAGameWorld(e.getWhoClicked().getWorld()) ||
                    BGHR.getPlugin().getConfig().getBoolean("main world.kits useable in main world", false)) {
                kit.outfitPlayer(p);
            }
        });
    }
}