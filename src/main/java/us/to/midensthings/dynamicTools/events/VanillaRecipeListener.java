package us.to.midensthings.dynamicTools.events;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareInventoryResultEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.SmithingTrimRecipe;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.tools.ToolItems;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/***
 * Stops DynamicTools items from being used in vanilla recipes that would treat them as their vanilla base item,
 * and makes the recipes that make sense for tools (repairing, combining) work with DynamicTools tools.
 */
public class VanillaRecipeListener implements Listener {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private static final NamespacedKey REPAIR_ITEM_RECIPE = NamespacedKey.minecraft("repair_item");
    // The smelting slot of furnaces, blast furnaces and smokers
    private static final int FURNACE_INPUT_SLOT = 0;

    // ---- Anvil ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        ItemStack first = e.getInventory().getFirstItem();
        ItemStack second = e.getInventory().getSecondItem();
        boolean firstIsTool = ToolItems.isTool(first);
        boolean secondIsTool = ToolItems.isTool(second);
        if (!firstIsTool && !secondIsTool) {
            return;
        }

        if (second == null || second.isEmpty()) {
            // Renaming. An empty name would remove the tool's name and show the vanilla item's name instead.
            String renameText = e.getView().getRenameText();
            if (first.hasData(DataComponentTypes.CUSTOM_NAME) && (renameText == null || renameText.isBlank())) {
                e.setResult(null);
            }
            return;
        }

        if (!firstIsTool || secondIsTool) {
            // Combining two tools only works if they're the same tool, vanilla handles the rest
            if (!ToolItems.isSameTool(first, second)) {
                e.setResult(null);
            }
            return;
        }

        if (second.getType() == Material.ENCHANTED_BOOK) {
            return;
        }

        // Repairing with a material, which has to be the head's material instead of the vanilla base item's
        ToolMaterial headMaterial = ToolItems.getHeadMaterial(first);
        if (headMaterial == null || !headMaterial.matches(second)) {
            e.setResult(null);
            return;
        }
        repairWithMaterial(e, first, second);
    }

    /***
     * Repairs the tool the same way vanilla repairs with a material: each item restores a quarter of the max durability.
     */
    private void repairWithMaterial(PrepareAnvilEvent e, ItemStack tool, ItemStack material) {
        Integer maxDamage = tool.getData(DataComponentTypes.MAX_DAMAGE);
        int damage = tool.getDataOrDefault(DataComponentTypes.DAMAGE, 0);
        int repairPerItem = maxDamage == null ? 0 : maxDamage / 4;
        if (damage <= 0 || repairPerItem <= 0) {
            e.setResult(null);
            return;
        }

        int itemsUsed = 0;
        while (damage > 0 && itemsUsed < material.getAmount()) {
            damage -= Math.min(damage, repairPerItem);
            itemsUsed++;
        }

        ItemStack result = tool.clone();
        result.setData(DataComponentTypes.DAMAGE, damage);

        int baseCost = tool.getDataOrDefault(DataComponentTypes.REPAIR_COST, 0);
        int cost = baseCost + itemsUsed;

        // Rename as well if the name was changed. The name field starts with the tool's name as plain text,
        // so leave the formatted name alone if it wasn't changed.
        String renameText = e.getView().getRenameText();
        Component currentName = tool.getData(DataComponentTypes.CUSTOM_NAME);
        String currentNameText = currentName == null ? "" : PlainTextComponentSerializer.plainText().serialize(currentName);
        if (renameText != null && !renameText.isBlank() && !renameText.equals(currentNameText)) {
            result.setData(DataComponentTypes.CUSTOM_NAME, Component.text(renameText));
            cost++;
        }

        result.setData(DataComponentTypes.REPAIR_COST, baseCost * 2 + 1);
        e.setResult(result);
        e.getView().setRepairCost(cost);
        e.getView().setRepairItemCountCost(itemsUsed);
    }

    // ---- Crafting ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        ItemStack[] matrix = e.getInventory().getMatrix();
        if (!containsDynamicToolsItem(matrix)) {
            if (!e.isRepair() && isBlockedVanillaItem(e.getInventory().getResult())) {
                plugin.debug("Blocked crafting a vanilla " + e.getInventory().getResult().getType().name());
                e.getInventory().setResult(null);
            }
            return;
        }
        e.getInventory().setResult(e.isRepair() ? repairInCraftingGrid(matrix) : null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent e) {
        if (!(e.getBlock().getState(false) instanceof Container crafter)) {
            return;
        }
        ItemStack[] contents = crafter.getInventory().getContents();
        if (!containsDynamicToolsItem(contents)) {
            if (!isRepairRecipe(e.getRecipe()) && isBlockedVanillaItem(e.getResult())) {
                plugin.debug("Blocked a crafter from crafting a vanilla " + e.getResult().getType().name());
                e.setCancelled(true);
            }
            return;
        }
        ItemStack result = isRepairRecipe(e.getRecipe()) ? repairInCraftingGrid(contents) : null;
        if (result == null) {
            e.setCancelled(true);
        } else {
            e.setResult(result);
        }
    }

    private static boolean isRepairRecipe(Recipe recipe) {
        return recipe instanceof ComplexRecipe complexRecipe && complexRecipe.getKey().equals(REPAIR_ITEM_RECIPE);
    }

    /***
     * Combines two identical tools the same way vanilla's repair recipe does: their durability is added together
     * with a 5% bonus, and only curses are kept.
     * @return the repaired tool, or null if the items aren't two identical tools.
     */
    private ItemStack repairInCraftingGrid(ItemStack[] matrix) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : matrix) {
            if (item != null && !item.isEmpty()) {
                items.add(item);
            }
        }
        if (items.size() != 2 || !ToolItems.isSameTool(items.get(0), items.get(1))) {
            return null;
        }
        ItemStack first = items.get(0);
        ItemStack second = items.get(1);
        Integer maxDamage = first.getData(DataComponentTypes.MAX_DAMAGE);
        if (maxDamage == null) {
            return null;
        }

        int remaining = (maxDamage - first.getDataOrDefault(DataComponentTypes.DAMAGE, 0))
                + (maxDamage - second.getDataOrDefault(DataComponentTypes.DAMAGE, 0))
                + maxDamage * 5 / 100;

        ItemStack result = first.clone();
        result.setAmount(1);
        result.setData(DataComponentTypes.DAMAGE, Math.max(0, maxDamage - remaining));
        result.resetData(DataComponentTypes.REPAIR_COST);

        for (Enchantment enchantment : new ArrayList<>(result.getEnchantments().keySet())) {
            if (!enchantment.isCursed()) {
                result.removeEnchantment(enchantment);
            }
        }
        for (Map.Entry<Enchantment, Integer> enchantment : second.getEnchantments().entrySet()) {
            if (enchantment.getKey().isCursed() && result.getEnchantmentLevel(enchantment.getKey()) < enchantment.getValue()) {
                result.addUnsafeEnchantment(enchantment.getKey(), enchantment.getValue());
            }
        }
        return result;
    }

    // ---- Grindstone, smithing table and other workstations ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareGrindstone(PrepareGrindstoneEvent e) {
        ItemStack upper = e.getInventory().getUpperItem();
        ItemStack lower = e.getInventory().getLowerItem();
        if (!ToolItems.isDynamicToolsItem(upper) && !ToolItems.isDynamicToolsItem(lower)) {
            return;
        }
        // Removing enchantments from one tool is fine, combining two only works if they're the same tool
        boolean combining = upper != null && !upper.isEmpty() && lower != null && !lower.isEmpty();
        if (combining && !ToolItems.isSameTool(upper, lower)) {
            e.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent e) {
        // Upgrades would turn the tool into a different vanilla item, e.g. a diamond based tool into a netherite pickaxe.
        // Armor trims only add the trim to the armor, so they're allowed.
        if (containsDynamicToolsItem(e.getInventory().getContents())) {
            if (!(e.getInventory().getRecipe() instanceof SmithingTrimRecipe)) {
                e.setResult(null);
            }
            return;
        }
        // Netherite upgrades make new vanilla tools and armor, armor trims don't
        if (e.getInventory().getRecipe() instanceof SmithingTransformRecipe && isBlockedVanillaItem(e.getResult())) {
            plugin.debug("Blocked upgrading to a vanilla " + e.getResult().getType().name());
            e.setResult(null);
        }
    }

    /***
     * @return true if the item is a vanilla tool or armor piece and crafting it is blocked in config.yml
     */
    private boolean isBlockedVanillaItem(ItemStack item) {
        if (item == null || item.isEmpty() || ToolItems.isDynamicToolsItem(item)) {
            return false;
        }
        String name = item.getType().name();
        boolean tool = item.getType() == Material.MACE || name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_SWORD") || name.endsWith("_SPEAR");
        boolean armor = name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
        return (tool && plugin.getConfig().getBoolean("block-vanilla-tools", false))
                || (armor && plugin.getConfig().getBoolean("block-vanilla-armor", false));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareOtherResult(PrepareInventoryResultEvent e) {
        InventoryType type = e.getInventory().getType();
        // Parts are paper based, which cartography tables accept
        if ((type == InventoryType.CARTOGRAPHY || type == InventoryType.LOOM) && containsDynamicToolsItem(e.getInventory().getContents())) {
            e.setResult(null);
        }
    }

    // ---- Furnaces ----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory() instanceof FurnaceInventory furnace)) {
            return;
        }

        if (e.getClickedInventory() == furnace && e.getSlot() == FURNACE_INPUT_SLOT) {
            ItemStack placed = switch (e.getAction()) {
                case HOTBAR_SWAP -> getSwappedItem(e.getWhoClicked(), e.getHotbarButton());
                default -> e.getCursor();
            };
            if (ToolItems.isTool(placed)) {
                e.setCancelled(true);
            }
        } else if (e.getClickedInventory() != furnace && e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && ToolItems.isTool(e.getCurrentItem())) {
            e.setCancelled(true);
        }
    }

    private static ItemStack getSwappedItem(HumanEntity player, int hotbarButton) {
        // -1 means the offhand swap key was used
        return hotbarButton < 0 ? player.getInventory().getItemInOffHand() : player.getInventory().getItem(hotbarButton);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory() instanceof FurnaceInventory
                && e.getRawSlots().contains(FURNACE_INPUT_SLOT) && ToolItems.isTool(e.getOldCursor())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent e) {
        if (e.getDestination() instanceof FurnaceInventory && ToolItems.isTool(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        // In case a tool got into a furnace some other way
        if (ToolItems.isTool(e.getSource())) {
            e.setCancelled(true);
        }
    }

    private static boolean containsDynamicToolsItem(ItemStack[] items) {
        for (ItemStack item : items) {
            if (ToolItems.isDynamicToolsItem(item)) {
                return true;
            }
        }
        return false;
    }
}
