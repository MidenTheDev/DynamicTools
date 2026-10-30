package us.to.midensthings.dynamicTools.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemManager {


    /***
     * Gives an item to a player, dropping whatever doesn't fit in their inventory at their feet.
     */
    public static void addOrDropItem(ItemStack item, Player p) {
        Map<Integer,ItemStack> itemMap = p.getInventory().addItem(item);
        if (!itemMap.isEmpty()) {
            for (ItemStack leftover : itemMap.values()) {
                p.getWorld().dropItem(p.getLocation(), leftover);
            }
        }
    }
}
