package us.to.midensthings.dynamicTools.parts;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.compat.CraftEngineCompat;
import us.to.midensthings.dynamicTools.compat.ItemsAdderCompat;
import us.to.midensthings.dynamicTools.resourcepack.ResourcePackGenerator;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.util.NamespacedKeys;

public class PartManager {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    /***
     * Creates a new part item from the custom item source plugin, tagged with the part type and material.
     * @return the part item, or null if the custom item plugin doesn't have the part item loaded.
     */
    public ItemStack getNewPartStack(PartType partType, ToolMaterial toolMaterial) {
        ItemStack item;
        switch (plugin.getCustomItemSource()) {
            case ITEMSADDER:
                ItemsAdderCompat iaComp = new ItemsAdderCompat();
                item = iaComp.getPartStack(partType);
                break;
            default:
                // default to CraftEngine
                CraftEngineCompat ceComp = new CraftEngineCompat();
                item = ceComp.getPartStack(partType);

        }
        if (item == null) {
            plugin.getLogger().warning("Could not find the custom item for part " + partType.name() + ". Make sure the DynamicTools configs are loaded in " + plugin.getCustomItemSource().name() + ".");
            return null;
        }

        // Set name
        String nameString = toolMaterial.getDisplayName()+" "+partType.getDisplayName();
        item.setData(DataComponentTypes.CUSTOM_NAME, MiniMessage.miniMessage().deserialize(nameString)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        // Use the material's texture instead of the custom item plugin's model
        ResourcePackGenerator.applyPartModel(item, partType, toolMaterial);

        // Set PDC data
        item.editPersistentDataContainer(pdc -> {
            pdc.set(NamespacedKeys.partType, PersistentDataType.STRING,partType.name());
            pdc.set(NamespacedKeys.partMaterial, PersistentDataType.STRING,toolMaterial.getId());
        });

        return item;
    }
}
