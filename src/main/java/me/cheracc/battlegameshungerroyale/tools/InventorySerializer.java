package me.cheracc.battlegameshungerroyale.tools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// credit: graywolf336 copied from: https://gist.githubusercontent.com/graywolf336/8153678/raw/1321df5a0511960538b3f60cd161baa2c0961d45/BukkitSerialization.java
public class InventorySerializer {
    /**
     * Converts the player inventory to a String array of Base64 strings. First string is the content and second string is the armor.
     *
     * @param player player whose inventory to serialize.
     * @return Array of strings: [ main content, armor content, ender chest content ]
     */
    public static String[] playerInventoryToBase64(Player player) throws IllegalStateException {
        PlayerInventory playerInventory = player.getInventory();
        //get the main content part, this doesn't return the armor
        String content = toBase64(playerInventory);
        String armor = itemStackArrayToBase64(playerInventory.getArmorContents());
        String enderChest = toBase64(player.getEnderChest());

        return new String[] { content, armor, enderChest };
    }

    /**
     *
     * A method to serialize an {@link ItemStack} array to Base64 String.
     *
     * <p />
     *
     * Based off of {@link #toBase64(Inventory)}.
     *
     * @param items to turn into a Base64 String.
     * @return Base64 string of the items.
     */
    public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Write the size of the inventory
            dataOutput.writeInt(items.length);

            // Save every element in the list, ignoring items issued by the plugin itself
            for (ItemStack item : items) {
                if (!Tools.isPluginItem(item))
                    dataOutput.writeObject(item);
                else
                    dataOutput.writeObject(null);
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    /**
     * A method to serialize an inventory to Base64 string.
     *
     * <p />
     *
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     *
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     *
     * @param inventory to serialize
     * @return Base64 string of the provided inventory
     */
    public static String toBase64(Inventory inventory) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            int inventorySize = inventory.getSize();

            // Write the size of the inventory
            dataOutput.writeInt(inventorySize);

            // Save every element in the list
            for (int i = 0; i < inventorySize; i++) {
                ItemStack item = inventory.getItem(i);
                if (Tools.isPluginItem(item))
                    item = null;

                dataOutput.writeObject(item);
            }

            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    /**
     *
     * A method to get an {@link Inventory} from an encoded, Base64, string.
     *
     * <p />
     *
     * Special thanks to Comphenix in the Bukkit forums or also known
     * as aadnk on GitHub.
     *
     * <a href="https://gist.github.com/aadnk/8138186">Original Source</a>
     *
     * @param data Base64 string of data containing an inventory.
     * @return Inventory created from the Base64 string.
     */
    public static Inventory fromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int inventorySize = dataInput.readInt();
            Inventory inventory;

            if (inventorySize == 41) {
                inventory = Bukkit.getServer().createInventory(null, InventoryType.PLAYER);
            } else
                inventory = Bukkit.getServer().createInventory(null, inventorySize);

            // Read the serialized inventory
            for (int i = 0; i < inventorySize; i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            dataInput.close();
            return inventory;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    /**
     * Gets an array of ItemStacks from Base64 string.
     *
     * <p />
     *
     * Base off of {@link #fromBase64(String)}.
     *
     * @param data Base64 string to convert to ItemStack array.
     * @return ItemStack array created from the Base64 string.
     */
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static void resetPlayerInventoryFromBase64(Player player, String[] base64EncodedPlayerInventory) throws IOException {
        if (base64EncodedPlayerInventory[0] == null || base64EncodedPlayerInventory[0].length() <= 1)
            return;

        ItemStack[] primary = fromBase64(base64EncodedPlayerInventory[0]).getContents();
        ItemStack[] armor = itemStackArrayFromBase64(base64EncodedPlayerInventory[1]);

        if (primary != null && primary.length >= 0)
            player.getInventory().setContents(primary);

        player.getInventory().setArmorContents(armor);

        if (base64EncodedPlayerInventory[2] == null)
            return;
        if (base64EncodedPlayerInventory.length >= 3) {
            Inventory enderChest = fromBase64(base64EncodedPlayerInventory[2]);
            ItemStack[] enderContents = enderChest.getContents();

            if (enderContents != null && enderContents.length >= 0)
                player.getEnderChest().setContents(enderContents);
        }

    }


}
