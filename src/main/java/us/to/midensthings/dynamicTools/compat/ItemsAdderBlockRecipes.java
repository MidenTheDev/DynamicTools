package us.to.midensthings.dynamicTools.compat;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.util.Schedulers;

/***
 * Registers the crafting recipes of the custom blocks on ItemsAdder. ItemsAdder's own recipes can't use item tags
 * (e.g. any planks), so DynamicTools registers them itself once ItemsAdder has loaded the block items.
 * CraftEngine supports tags, so its recipes are in its block configs instead.
 */
public class ItemsAdderBlockRecipes implements Listener {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    // ItemsAdder fires this after loading its items, and again after every /iazip
    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent e) {
        Schedulers.runGlobal(this::registerRecipes);
    }

    private void registerRecipes() {
        for (CustomBlocks.CustomBlock block : CustomBlocks.ALL) {
            Bukkit.removeRecipe(new NamespacedKey(plugin, block.id()), false);
            if (!block.isEnabled()) {
                continue;
            }

            ItemStack result = ItemsAdderCompat.getBlockItem(block);
            if (result == null) {
                plugin.getLogger().warning("ItemsAdder doesn't have the dynamictools:" + block.id() + " block loaded, so its recipe wasn't added. Run /iazip to load it.");
                continue;
            }
            Bukkit.addRecipe(CustomBlocks.createRecipe(block, result), true);
            plugin.debug("Registered the " + block.id() + " recipe");
        }
    }

    /***
     * Removes the recipes when DynamicTools is disabled.
     */
    public static void removeRecipes() {
        DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);
        for (CustomBlocks.CustomBlock block : CustomBlocks.ALL) {
            Bukkit.removeRecipe(new NamespacedKey(plugin, block.id()), false);
        }
    }
}
