package us.to.midensthings.dynamicTools.tools;

import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ToolMaterialRegistry {

    // Kept in materials.yml order, so when an item is an input for more than one material the first one listed wins
    private Map<String, ToolMaterial> toolMaterialMap = new LinkedHashMap<>();

    public void registerToolMaterial (ToolMaterial toolMaterial) {
        toolMaterialMap.put(toolMaterial.getId(),toolMaterial);
    }

    public ToolMaterial getToolMaterial (String id) {
        return toolMaterialMap.get(id);

    }

    /***
     * @return the first material in materials.yml the item is an input for, or null if there is none.
     */
    public ToolMaterial getToolMaterial (ItemStack item) {
        for (ToolMaterial material : toolMaterialMap.values()) {
            if (material.matches(item)) {
                return material;
            }
        }
        return null;
    }

    public Collection<ToolMaterial> getMaterials() {
        return toolMaterialMap.values();
    }
}
