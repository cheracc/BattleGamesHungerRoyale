package me.cheracc.battlegameshungerroyale.abilities;

import me.cheracc.battlegameshungerroyale.types.abilities.TriggeredAbility;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.AbilityTrigger;
import me.cheracc.battlegameshungerroyale.types.abilities.enums.DualWieldBonusType;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class DualWield extends TriggeredAbility implements Listener {
    private boolean bonusWhenUsingSwords;
    private boolean bonusWhenUsingAxes;
    private double attackSpeedBonus;
    private int moveSpeedBonusDuration;
    private DualWieldBonusType dualWieldBonus;

    public DualWield() {
        bonusWhenUsingSwords = false;
        bonusWhenUsingAxes = true;
        dualWieldBonus = DualWieldBonusType.ATTACK_SPEED_BONUS;
        attackSpeedBonus = 0.25;
        moveSpeedBonusDuration = 8;
        setDescription("Provides a bonus when the player is dual wielding a sword and/or axe");
    }

    private boolean isDualWielding(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        if (main != null && off != null) {
            if (bonusWhenUsingSwords)
                return main.getType().name().contains("SWORD") && off.getType().name().contains("SWORD");
            if (bonusWhenUsingAxes)
                return main.getType().name().contains("AXE") && off.getType().name().contains("AXE");
        }
        return false;
    }

    private void setAttackSpeedModifier(Player player, ItemStack item) {
        if (item != null && item.getItemMeta() != null) {
            NamespacedKey key = new NamespacedKey(plugin, "dual_wield_modified");
            ItemMeta meta = item.getItemMeta();

            // doesn't have the ability - remove the attribute modifier
            if (player == null || meta.getPersistentDataContainer().has(key, PersistentDataType.STRING) && !hasMyAbility(player)) {
                meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
                meta.getPersistentDataContainer().remove(key);
                item.setItemMeta(meta);
                return;
            }

            // has ability but weapon does not have modifier
            if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING) && hasMyAbility(player) && isDualWielding(player)) {
                AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "dual_wield_attack_speed_modifier", attackSpeedBonus,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlot.HAND);
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, modifier);
                meta.getPersistentDataContainer().remove(key);
                item.setItemMeta(meta);
            }
        }
    }

    private void removeModifierAfterOneTick(ItemStack item) {
        new BukkitRunnable() {
            @Override
            public void run() {
                item.editMeta(meta -> meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED));
            }
        }.runTaskLater(plugin, 1L);
    }

    @Override
    public AbilityTrigger getTrigger() {
        return AbilityTrigger.DEAL_MELEE_HIT;
    }

    @Override
    public void onTrigger(Player player, Event event) {
        if (isDualWielding(player) && hasMyAbility(player)) {
            if (!(event instanceof EntityDamageByEntityEvent))
                return;
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
            switch (dualWieldBonus) {
                case DAMAGE_BONUS:
                    e.setDamage(e.getDamage() * (1 + attackSpeedBonus));
                    break;
                case ATTACK_SPEED_BONUS:
                    ItemStack weapon = player.getInventory().getItemInMainHand();
                    if (weapon == null || weapon.getItemMeta() == null)
                        break;
                    setAttackSpeedModifier(player, weapon);
                    removeModifierAfterOneTick(weapon);
                    break;
                case MOVEMENT_SPEED_BONUS:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, moveSpeedBonusDuration * 20, 0, false, false, false));
                    break;
            }
        }
    }

}
