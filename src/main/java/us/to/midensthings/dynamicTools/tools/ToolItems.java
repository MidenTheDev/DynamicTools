package us.to.midensthings.dynamicTools.tools;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import us.to.midensthings.dynamicTools.util.NamespacedKeys;

/***
 * Identifies DynamicTools items from their stored data.
 */
public final class ToolItems {

    private ToolItems() {
    }

    public static boolean isTool(ItemStack item) {
        return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(NamespacedKeys.toolType);
    }

    public static boolean isPart(ItemStack item) {
        return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(NamespacedKeys.partType);
    }

    /***
     * @return true if the item is a DynamicTools tool or part
     */
    public static boolean isDynamicToolsItem(ItemStack item) {
        return isTool(item) || isPart(item);
    }

    /***
     * @return the tool type and materials of the tool, e.g. "PICKAXE/iron/wood/gold", or null if the item isn't a tool.
     */
    public static String getToolSignature(ItemStack item) {
        if (!isTool(item)) {
            return null;
        }
        PersistentDataContainerView pdc = item.getPersistentDataContainer();
        return pdc.get(NamespacedKeys.toolType, PersistentDataType.STRING)
                + "/" + pdc.get(NamespacedKeys.primaryMaterial, PersistentDataType.STRING)
                + "/" + pdc.get(NamespacedKeys.secondaryMaterial, PersistentDataType.STRING)
                + "/" + pdc.get(NamespacedKeys.tertiaryMaterial, PersistentDataType.STRING);
    }

    /***
     * @return true if both items are tools of the same type made from the same materials in the same parts.
     */
    public static boolean isSameTool(ItemStack first, ItemStack second) {
        String signature = getToolSignature(first);
        return signature != null && signature.equals(getToolSignature(second));
    }

    /***
     * @return the material of the tool's head, or null if the item isn't a tool or the material no longer exists.
     */
    public static ToolMaterial getHeadMaterial(ItemStack item) {
        if (!isTool(item)) {
            return null;
        }
        String id = item.getPersistentDataContainer().get(NamespacedKeys.primaryMaterial, PersistentDataType.STRING);
        return id == null ? null : ToolMaterial.fromId(id);
    }
}
