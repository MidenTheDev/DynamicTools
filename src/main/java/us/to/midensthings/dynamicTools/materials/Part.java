package us.to.midensthings.dynamicTools.materials;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.util.NamespacedKeys;

public class Part {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private final PartType partType;
    private final ToolMaterial toolMaterial;
    private final ItemStack item;

    public Part(PartType partType, ToolMaterial toolMaterial) {
        this.partType = partType;
        this.toolMaterial = toolMaterial;
        this.item = plugin.getPartManager().getNewPartStack(partType, toolMaterial);
    }

    public Part(PartType partType, ToolMaterial toolMaterial, ItemStack sourceitem) {
        this.partType = partType;
        this.toolMaterial = toolMaterial;
        this.item = sourceitem;
    }

    /***
     * Generates a Part class containing all needed data from the given ItemStack
     * @param item
     * @return the Part or null if the item is not a part or is missing necessary data.
     */
    public static Part fromItemStack(ItemStack item) {
        String matId = item.getPersistentDataContainer().getOrDefault(NamespacedKeys.partMaterial, PersistentDataType.STRING, "");
        if (matId.isEmpty()) {
            return null;
        }
        ToolMaterial toolMat = plugin.getToolMaterialRegistry().getToolMaterial(matId);
        if (toolMat == null) {
            return null;
        }


        String partId = item.getPersistentDataContainer().getOrDefault(NamespacedKeys.partType, PersistentDataType.STRING, "");
        if (partId.isEmpty()) {
            return null;
        }
        PartType partType;
        try {
            partType = PartType.valueOf(partId);
            return new Part(partType,toolMat);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
