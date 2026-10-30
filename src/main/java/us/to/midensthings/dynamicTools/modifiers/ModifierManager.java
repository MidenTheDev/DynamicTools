package us.to.midensthings.dynamicTools.modifiers;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.util.NamespacedKeys;

import java.util.LinkedHashMap;
import java.util.Map;

/***
 * Reads and writes the modifiers stored on tool items. Modifiers are stored as a container of modifier ID -> level.
 */
public class ModifierManager {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    public void setModifiers(ItemStack item, Map<Modifier,Integer> modifiers) {
        item.editPersistentDataContainer(pdc -> {
            PersistentDataContainer container = pdc.getAdapterContext().newPersistentDataContainer();
            modifiers.forEach((modifier, level) -> container.set(getKey(modifier), PersistentDataType.INTEGER, level));
            pdc.set(NamespacedKeys.modifiers, PersistentDataType.TAG_CONTAINER, container);
        });
    }

    /***
     * @return the modifiers on the item and their levels. Modifiers that are no longer registered are left out.
     */
    public Map<Modifier,Integer> getModifiers(ItemStack item) {
        Map<Modifier,Integer> modifiers = new LinkedHashMap<>();
        PersistentDataContainer container = getContainer(item);
        if (container == null) {
            return modifiers;
        }
        for (Modifier modifier : plugin.getModifierRegistry().getModifiers()) {
            Integer level = container.get(getKey(modifier), PersistentDataType.INTEGER);
            if (level != null) {
                modifiers.put(modifier, level);
            }
        }
        return modifiers;
    }

    /***
     * @return the level of the modifier on the item, or 0 if the item doesn't have it.
     */
    public int getLevel(ItemStack item, Modifier modifier) {
        PersistentDataContainer container = getContainer(item);
        if (container == null) {
            return 0;
        }
        return container.getOrDefault(getKey(modifier), PersistentDataType.INTEGER, 0);
    }

    private PersistentDataContainer getContainer(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        return item.getPersistentDataContainer().get(NamespacedKeys.modifiers, PersistentDataType.TAG_CONTAINER);
    }

    private NamespacedKey getKey(Modifier modifier) {
        return NamespacedKey.fromString(modifier.getId(), plugin);
    }
}
