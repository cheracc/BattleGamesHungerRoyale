package me.cheracc.battlegameshungerroyale.datatypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.cheracc.battlegameshungerroyale.BGHR;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CustomLootTable implements LootTable {
    String name;
    JsonNode json = null;
    LootItemPool pool;

    public CustomLootTable(File jsonFile) {
        try {
            json = new ObjectMapper().readTree(jsonFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        pool = new LootItemPool(json);
        name = jsonFile.getName().split("\\.")[0];
    }


    @Override
    public @NotNull Collection<ItemStack> populateLoot(@NotNull Random random, @NotNull LootContext context) {
        return pool.generateLoot(random);
    }

    @Override
    public void fillInventory(@NotNull Inventory inventory, @NotNull Random random, @NotNull LootContext context) {
        Collection<ItemStack> items = pool.generateLoot(random);

        for (ItemStack item : items) {
            boolean placed = false;
            int size = inventory.getSize();
            while (!placed && size > 0) {
                size--;
                int slot = random.nextInt(inventory.getSize());
                if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.AIR)
                    continue;
                inventory.setItem(slot, item);
                placed = true;
            }
        }
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return new NamespacedKey(BGHR.getPlugin(), name);
    }

    public class LootItemPool {
        int minItems;
        int maxItems;
        int minBonusItemsPerLuck;
        int maxBonusItemsPerLuck;
        List<LootTableItem> items = new ArrayList<>();

        public LootItemPool(JsonNode json) {
            JsonNode pool = json.path("pools");
            minItems = pool.path("rolls").path("min").asInt(0);
            maxItems = pool.path("rolls").path("max").asInt(1);
            minBonusItemsPerLuck = pool.path("bonus_rolls").path("min").asInt(0);
            maxBonusItemsPerLuck = pool.path("bonus_rolls").path("max").asInt(0);

            JsonNode items = pool.path("entries");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    this.items.add(new LootTableItem(item));
                }
            }
        }

        Collection<ItemStack> generateLoot(Random rand) {
            Set<ItemStack> items = new HashSet<>();
            int size = rand.nextInt(maxItems-minItems) + minItems;

            while (items.size() < size) {
                Collections.shuffle(this.items);
                items.add(this.items.get(rand.nextInt(this.items.size() - 1)).createItem());
            }
            return items;
        }
    }

    public class LootTableItem {
        Material type;
        int weight;
        int quality;
        List<LootFunction> functions = new ArrayList<>();

        public LootTableItem(JsonNode itemNode) {
            this.type = Material.getMaterial(itemNode.path("name").asText());
            this.weight = itemNode.path("weight").asInt(1);
            this.quality = itemNode.path("quality").asInt(1);
            JsonNode functions = itemNode.path("functions");

            if (functions.isArray()) {
                for (JsonNode function: functions) {
                    this.functions.add(new LootFunction(function));
                }
            }
        }

        ItemStack createItem() {
            return null;
        }
    }

    public class LootFunction {
        FunctionType type;
        List<Enchantment> enchantList = new ArrayList<>();
        boolean hasChance;
        double chance;
        int min;
        int max;

        public LootFunction(JsonNode function) {
            this.type = FunctionType.valueOf(function.path("function").toString().toUpperCase());
            this.hasChance = function.path("conditions").path("condition").toString().equalsIgnoreCase("random_chance");
            this.chance = function.path("conditions").path("chance").asInt(0);

            switch (type) {
                case SET_COUNT:
                    min = function.path("count").path("min").asInt(1);
                    max = function.path("count").path("max").asInt(1);
                    break;
                case ENCHANT_WITH_LEVELS:
                    min = function.path("levels").path("min").asInt(0);
                    max = function.path("levels").path("max").asInt(10);
                    break;
                case ENCHANT_RANDOMLY:
                    if (function.path("enchantments").isArray()) {
                        for (String s : function.findValuesAsText("enchantments"))
                            enchantList.add(Enchantment.getByKey(NamespacedKey.minecraft(s)));
                    }
                    break;
                case SET_DAMAGE:
                    min = function.path("damage").path("min").asInt(0);
                    max = function.path("damage").path("max").asInt(100);
                    break;
            }
        }

        void perform() {

        }
    }

    private enum FunctionType { ENCHANT_WITH_LEVELS, ENCHANT_RANDOMLY, SET_DAMAGE, SET_COUNT }
}
