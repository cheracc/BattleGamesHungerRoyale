package me.cheracc.battlegameshungerroyale.abilities;
import me.cheracc.battlegameshungerroyale.BGHR;
import me.cheracc.battlegameshungerroyale.events.GameStartEvent;
import me.cheracc.battlegameshungerroyale.managers.PlayerManager;
import me.cheracc.battlegameshungerroyale.tools.Tools;
import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.AbilityTrigger;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.EnchantWrapper;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.UpgradeType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class UpgradeableItem extends TriggeredAbility implements Listener {
    private ItemStack baseItem;
    private AbilityTrigger trigger;
    private UpgradeType upgradeType;
    private EnchantWrapper enchantment;
    private int neededToUpgrade;
    private int addMoreAmount;

    public UpgradeableItem() {
        baseItem = new ItemStack(Material.WOODEN_SWORD);
        trigger = AbilityTrigger.KILL_ANYTHING;
        upgradeType = UpgradeType.ENCHANT_ITEM;
        enchantment = EnchantWrapper.SHARPNESS;
        neededToUpgrade = 1;
        addMoreAmount = 1;
        setDescription("Give the player an item at the start of each game that can be upgraded by configurable triggers");
    }

    @Override
    public AbilityTrigger getTrigger() {
        return trigger;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        int count = getCount(player);
        count++;

        if (count >= neededToUpgrade) {
            upgradeItem(player);
            setCount(player, 0);
            setUpgradeLevel(player, getUpgradeLevel(player) + 1);
            return;
        }
        setCount(player, count);
    }

    @EventHandler
    public void giveItemOnGameStart(GameStartEvent event) {
        PlayerManager playerManager = JavaPlugin.getPlugin(BGHR.class).getApi().getPlayerManager();
        for (Player p : event.getPlayers()) {
            if (playerManager.getPlayerData(p).hasKit(getAssignedKit()))
                if (!p.hasMetadata("has_base_item")) {
                    giveBaseItem(p);
                    p.setMetadata("has_base_item", new FixedMetadataValue(plugin, true));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            p.removeMetadata("has_base_item", plugin);
                        }
                    }.runTaskLater(plugin, 1);
                }
        }
    }

    private void upgradeItem(Player player) {
        ItemStack item = getItem(player);
        if (item == null)
            return;

        switch (upgradeType) {
            case ENCHANT_ITEM:
                if (item.getEnchantments().containsKey(enchantment.getEnchantment())) {
                    int enchantLevel = item.getEnchantmentLevel(enchantment.getEnchantment());
                    enchantItem(item, Math.max(enchantLevel + 1, enchantment.getEnchantment().getMaxLevel()));
                }
                enchantItem(item, 1);
                player.updateInventory();
                break;
            case NEXT_BETTER_TYPE:
                upgradeItemType(item);
                player.updateInventory();
                break;
            case ADD_TO_STACK:
                item.setAmount(Math.max(item.getType().getMaxStackSize(), item.getAmount() + addMoreAmount));
                player.updateInventory();
                break;
        }
    }

    private void enchantItem(ItemStack item, int level) {
        item.removeEnchantment(enchantment.getEnchantment());
        item.addUnsafeEnchantment(enchantment.getEnchantment(), level + 1);
    }

    private void upgradeItemType(ItemStack item) {
        String[] toolTypes = new String[] { "WOODEN",  "GOLDEN", "STONE", "IRON", "DIAMOND", "NETHERITE" };
        String[] armorTypes = new String[] { "LEATHER", "GOLDEN", "CHAINMAIL", "IRON", "DIAMOND", "NETHERITE" };
        String[] armors = new String[] { "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS" };

        String[] split = item.getType().name().split("_");
        if (split.length != 2)
            return;

        String currentType = split[0];
        String itemType = split[1];

        boolean isArmor = Arrays.asList(armors).contains(itemType);

        String[] rankLadder = isArmor ? armorTypes : toolTypes;
        int index = Arrays.asList(rankLadder).indexOf(currentType);

        if (index < 0 || index >= rankLadder.length - 1)
            return;

        index++;
        Material newMaterial = Material.valueOf(rankLadder[index] + "_" + itemType);
        item.setType(newMaterial);
    }

    private int getCount(Player player) {
        if (player.hasMetadata("upgradeable_item_trigger_count")) {
            return (int) player.getMetadata("upgradeable_item_trigger_count").get(0).value();
        }
        return 0;
    }

    private void setCount(Player player, int count) {
        player.removeMetadata("upgradeable_item_trigger_count", plugin);
        player.setMetadata("upgradeable_item_trigger_count", new FixedMetadataValue(plugin, count));
    }

    private int getUpgradeLevel(Player player) {
        if (player.hasMetadata("upgradeable_item_level")) {
            return (int) player.getMetadata("upgradeable_item_level").get(0).value();
        }
        return 0;
    }

    private void setUpgradeLevel(Player player, int level) {
        player.removeMetadata("upgradeable_item_level", plugin);
        player.setMetadata("upgradeable_item_level", new FixedMetadataValue(plugin, level));
    }

    private void giveBaseItem(Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "upgradeable_item");
        ItemStack item = baseItem.clone();
        Tools.tagAsPluginItem(new ItemStack(item));
        tagAbilityItem(item);
        item.editMeta(meta -> meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, getId().toString()));
        player.getInventory().addItem(item);
    }

    private ItemStack getItem(Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "upgradeable_item");
        for (ItemStack i : player.getInventory().getContents()) {
            if (i == null || i.getType().isAir() || i.getItemMeta() == null)
                continue;
            ItemMeta meta = i.getItemMeta();
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING))
                return i;
        }
        return null;
    }
}
