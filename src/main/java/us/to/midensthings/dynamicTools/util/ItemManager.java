package us.to.midensthings.dynamicTools.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemManager {


    public static void addOrDropItem(ItemStack item, Player p) {
        Map<Integer,ItemStack> itemMap = p.getInventory().addItem(item);
        if (!itemMap.isEmpty()) {

        }
    }
}
