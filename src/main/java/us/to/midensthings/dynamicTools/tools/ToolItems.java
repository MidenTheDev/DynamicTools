package us.to.midensthings.dynamicTools.tools;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
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
     * @return the tool type, materials and bonus modifiers of the tool, e.g. "PICKAXE/iron/wood/gold" or
     * "PICKAXE/iron/wood/gold/frigid:2", or null if the item isn't a tool.
     */
    public static String getToolSignature(ItemStack item) {
        if (!isTool(item)) {
            return null;
        }
        PersistentDataContainerView pdc = item.getPersistentDataContainer();
        StringBuilder signature = new StringBuilder()
                .append(pdc.get(NamespacedKeys.toolType, PersistentDataType.STRING))
                .append("/").append(pdc.get(NamespacedKeys.primaryMaterial, PersistentDataType.STRING))
                .append("/").append(pdc.get(NamespacedKeys.secondaryMaterial, PersistentDataType.STRING))
                .append("/").append(pdc.get(NamespacedKeys.tertiaryMaterial, PersistentDataType.STRING));
        DynamicTools.getPlugin(DynamicTools.class).getModifierManager().getBonusModifiers(item).entrySet().stream()
                .map(entry -> entry.getKey().getId() + ":" + entry.getValue())
                .sorted()
                .forEach(bonus -> signature.append("/").append(bonus));
        return signature.toString();
    }

    /***
     * @return true if both items are tools of the same type made from the same materials in the same parts,
     * with the same bonus modifiers.
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
