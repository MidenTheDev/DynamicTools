package us.to.midensthings.dynamicTools.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.compat.CraftEngineCompat;
import us.to.midensthings.dynamicTools.compat.ItemsAdderCompat;

import java.util.List;
import java.util.Locale;

public class GuiItems {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    /***
     * Creates a decoration item for a GUI with its tooltip hidden.
     * @param materialName the item from config.yml: a vanilla material (e.g. GRAY_STAINED_GLASS_PANE or minecraft:arrow)
     *                     or the namespaced ID of an ItemsAdder or CraftEngine item (e.g. my_items:fancy_pane)
     * @param fallback the material to use if materialName isn't a valid item
     */
    public static ItemStack createDecoration(String materialName, Material fallback) {
        ItemStack item = materialName == null ? null : getItem(materialName.trim());
        if (item == null) {
            plugin.getLogger().warning("Invalid GUI item material " + materialName + ", using " + fallback.name() + " instead");
            item = ItemStack.of(fallback);
        }
        item.setAmount(1);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return item;
    }

    /***
     * @return a vanilla item, or a custom item if the name has a namespace other than minecraft.
     * Null if there's no such item, or the custom item plugin isn't installed or hasn't loaded it.
     */
    private static ItemStack getItem(String name) {
        NamespacedKey key = name.contains(":") ? NamespacedKey.fromString(name.toLowerCase(Locale.ROOT)) : null;
        if (key == null || key.getNamespace().equals(NamespacedKey.MINECRAFT)) {
            Material material = Material.matchMaterial(name);
            return material == null || !material.isItem() || material.isAir() ? null : ItemStack.of(material);
        }

        List<String> compats = plugin.getEnabledCompats();
        if (compats.contains("ItemsAdder")) {
            ItemStack item = ItemsAdderCompat.getItem(key.asString());
            if (item != null) {
                return item;
            }
        }
        if (compats.contains("CraftEngine")) {
            return CraftEngineCompat.getItem(key.asString());
        }
        return null;
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
