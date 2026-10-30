package us.to.midensthings.dynamicTools.modifiers;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import us.to.midensthings.dynamicTools.DynamicTools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ModifierRegistry {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private Map<String,Modifier> modifierMap = new HashMap<>();

    public Modifier getModifier(String id) {
        return modifierMap.get(id);
    }

    /***
     * Registers a modifier, and its event handlers if it's an EventModifier.
     * Modifiers have to be registered before materials.yml is loaded to be used by materials.
     * @throws IllegalArgumentException if the ID is invalid or already registered
     */
    public void registerModifier(Modifier modifier) {
        if (NamespacedKey.fromString(modifier.getId(), plugin) == null) {
            throw new IllegalArgumentException("Invalid modifier ID " + modifier.getId());
        }
        if (modifierMap.containsKey(modifier.getId())) {
            throw new IllegalArgumentException("A modifier with the ID " + modifier.getId() + " is already registered");
        }

        modifierMap.put(modifier.getId(),modifier);
        if (modifier instanceof EventModifier eventModifier) {
            Bukkit.getPluginManager().registerEvents(eventModifier, plugin);
        }
    }

    public Collection<Modifier> getModifiers() {
        return modifierMap.values();
    }

}
