package us.to.midensthings.dynamicTools.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ToolMaterialRegistry {

    private Map<String, ToolMaterial> toolMaterialMap = new HashMap<>();

    public void registerToolMaterial (ToolMaterial toolMaterial) {
        toolMaterialMap.put(toolMaterial.getId(),toolMaterial);
    }

    public ToolMaterial getToolMaterial (String id) {
        return toolMaterialMap.get(id);

    }

    public Collection<ToolMaterial> getMaterials() {
        return toolMaterialMap.values();
    }
}
