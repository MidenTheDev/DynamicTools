package us.to.midensthings.dynamicTools.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;

public class GuiItems {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    /***
     * Creates a decoration item for a GUI with its tooltip hidden.
     * @param materialName the item's material from config.yml
     * @param fallback the material to use if materialName isn't a valid item
     */
    public static ItemStack createDecoration(String materialName, Material fallback) {
        Material material = materialName == null ? null : Material.matchMaterial(materialName);
        if (material == null || !material.isItem() || material.isAir()) {
            plugin.getLogger().warning("Invalid GUI item material " + materialName + ", using " + fallback.name() + " instead");
            material = fallback;
        }
        ItemStack item = ItemStack.of(material);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return item;
    }

    /***
     * Moves as much of an item as fits into a slot.
     * @return what is left of the item after moving it, or null if all of it was moved.
     */
    public static ItemStack insert(Inventory inv, int slot, ItemStack item) {
        ItemStack current = inv.getItem(slot);
        if (current == null || current.isEmpty()) {
            inv.setItem(slot, item.clone());
            return null;
        }
        if (!current.isSimilar(item)) {
            return item;
        }

        int moved = Math.min(current.getMaxStackSize() - current.getAmount(), item.getAmount());
        if (moved <= 0) {
            return item;
        }
        current.setAmount(current.getAmount() + moved);
        inv.setItem(slot, current);

        ItemStack leftover = item.clone();
        leftover.setAmount(item.getAmount() - moved);
        return leftover.getAmount() > 0 ? leftover : null;
    }

    /***
     * Removes an amount of items from a slot, clearing the slot if none are left.
     */
    public static void subtract(Inventory inv, int slot, int amount) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.isEmpty()) {
            return;
        }
        item.setAmount(item.getAmount() - amount);
        inv.setItem(slot, item.getAmount() > 0 ? item : null);
    }
}
