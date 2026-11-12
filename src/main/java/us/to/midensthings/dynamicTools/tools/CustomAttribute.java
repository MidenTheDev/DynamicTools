package us.to.midensthings.dynamicTools.tools;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.compat.ReliqueCompat;
import us.to.midensthings.dynamicTools.util.NamespacedKeys;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/***
 * A custom stat from custom-attributes.yml, e.g. for other plugins to read. Materials give it a value and multiplier,
 * which are weighted onto tools like attack damage, and the result is stored in the tool's persistent data container
 * as a DOUBLE under namespace:id.
 * @param id the attribute's ID, also the key in materials.yml
 * @param key where the final value is stored on tools
 * @param displayOnTools show the value in the tool's lore
 * @param displayName the lore line (MiniMessage), with %final_value% replaced by the value
 * @param defaultValue the value for materials that don't set one
 * @param defaultMultiplier the multiplier for materials that don't set one
 * @param validToolTypes the tool types that get this attribute, all of them if valid-tool-types isn't set
 */
public record CustomAttribute(String id, NamespacedKey key, boolean displayOnTools, String displayName,
                              double defaultValue, double defaultMultiplier, Set<ToolType> validToolTypes) {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    /***
     * A material's value and multiplier for an attribute.
     */
    public record MaterialValue(double value, double multiplier) {}

    /***
     * Reads every attribute from custom-attributes.yml. Invalid ones are skipped with a warning.
     */
    public static Map<String, CustomAttribute> loadAll(ConfigurationSection config) {
        Map<String, CustomAttribute> attributes = new LinkedHashMap<>();
        for (String id : config.getKeys(false)) {
            String namespace = config.getString(id + ".namespace", "dynamictools").toLowerCase(Locale.ROOT);
            NamespacedKey key = NamespacedKey.fromString(namespace + ":" + id);
            if (key == null) {
                plugin.getLogger().warning("Invalid custom attribute " + id + " (namespace " + namespace + ") in custom-attributes.yml."
                        + " IDs and namespaces can only use lowercase letters, numbers, _ - . and /");
                continue;
            }
            attributes.put(id, new CustomAttribute(
                    id,
                    key,
                    config.getBoolean(id + ".display-on-tools", false),
                    config.getString(id + ".display-name", "%final_value% " + id),
                    config.getDouble(id + ".default-value", 0.0),
                    config.getDouble(id + ".default-multiplier", 1.0),
                    loadToolTypes(config.getStringList(id + ".valid-tool-types"), id)));
        }
        return attributes;
    }

    /***
     * @return the tool types from valid-tool-types, or every tool type if the list is empty or missing.
     * Unknown tool types are skipped with a warning.
     */
    private static Set<ToolType> loadToolTypes(List<String> names, String id) {
        if (names.isEmpty()) {
            return EnumSet.allOf(ToolType.class);
        }
        Set<ToolType> toolTypes = EnumSet.noneOf(ToolType.class);
        for (String name : names) {
            try {
                toolTypes.add(ToolType.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown tool type " + name + " in valid-tool-types of custom attribute " + id
                        + ". Use one of " + Arrays.toString(ToolType.values()));
            }
        }
        if (toolTypes.isEmpty()) {
            plugin.getLogger().warning("Custom attribute " + id + " has no valid tool types, so no tools will get it");
        }
        return toolTypes;
    }

    /***
     * @return true if tools of this type get the attribute
     */
    public boolean appliesTo(ToolType toolType) {
        return validToolTypes.contains(toolType);
    }

    /***
     * Gets the total value of this attribute across what an entity is using: the tools it holds in its main hand,
     * the armor it's wearing and, if Relique is installed, its equipped relics. Like vanilla attributes, a tool only
     * counts in the slot it gives its stats in, so a helmet held in the main hand or a sword in an armor slot doesn't
     * add anything. Any relic with the attribute counts, whether or not it's a DynamicTools item.
     * On Folia, call this on the entity's thread.
     * @param entity a player or any other entity that can hold and wear items
     * @return the sum of the attribute's values, or 0 if none of the items have it
     */
    public double getValue(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return 0;
        }
        double total = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!entity.canUseEquipmentSlot(slot)) {
                continue;
            }
            ItemStack item = equipment.getItem(slot);
            ToolType toolType = getToolType(item);
            if (toolType != null && toolType.getAttributeSlot().test(slot)) {
                total += getValue(item);
            }
        }
        if (plugin.isReliqueEnabled()) {
            for (ItemStack relic : ReliqueCompat.getEquippedRelics(entity)) {
                total += getValue(relic);
            }
        }
        return total;
    }

    /***
     * @return the value of this attribute stored on the item, or 0 if it doesn't have one
     */
    public double getValue(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return 0;
        }
        Double value = item.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        return value == null ? 0 : value;
    }

    /***
     * Gets the total value of a custom attribute across the tools an entity holds in its main hand and the armor it's
     * wearing. See {@link #getValue(LivingEntity)}.
     * @param id the attribute's ID in custom-attributes.yml
     * @return the sum of the attribute's values, or 0 if none of the items have it or there's no attribute with that ID
     */
    public static double getValue(LivingEntity entity, String id) {
        CustomAttribute attribute = plugin.getCustomAttributes().get(id);
        return attribute == null ? 0 : attribute.getValue(entity);
    }

    /***
     * @return the tool type of a DynamicTools tool, or null if the item isn't one
     */
    private static ToolType getToolType(ItemStack item) {
        if (!ToolItems.isTool(item)) {
            return null;
        }
        String name = item.getPersistentDataContainer().get(NamespacedKeys.toolType, PersistentDataType.STRING);
        try {
            return name == null ? null : ToolType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /***
     * Reads a material's custom-attributes section. Attributes that aren't in custom-attributes.yml are skipped with a warning,
     * and a missing value or multiplier uses the attribute's default.
     */
    public static Map<String, MaterialValue> loadMaterialValues(ConfigurationSection section, Map<String, CustomAttribute> attributes, String material) {
        Map<String, MaterialValue> values = new LinkedHashMap<>();
        if (section == null) {
            return values;
        }
        for (String id : section.getKeys(false)) {
            CustomAttribute attribute = attributes.get(id);
            if (attribute == null) {
                plugin.getLogger().warning("Unknown custom attribute " + id + " in material " + material + ". Add it to custom-attributes.yml first.");
                continue;
            }
            values.put(id, new MaterialValue(
                    section.getDouble(id + ".value", attribute.defaultValue()),
                    section.getDouble(id + ".multiplier", attribute.defaultMultiplier())));
        }
        return values;
    }

    /***
     * Weights the attribute onto a tool the same way as attack damage:
     * (Head value + 1/2 handle value + 1/2 binding value) X (Mean multiplier across all parts).
     * Materials that don't set the attribute use its default value and multiplier.
     * @return the tool's value, or null if none of the materials set the attribute
     */
    public Double calculate(ToolMaterial primary, ToolMaterial secondary, ToolMaterial tertiary) {
        MaterialValue p = primary.getCustomAttributes().get(id);
        MaterialValue s = secondary.getCustomAttributes().get(id);
        MaterialValue t = tertiary.getCustomAttributes().get(id);
        if (p == null && s == null && t == null) {
            return null;
        }
        MaterialValue fallback = new MaterialValue(defaultValue, defaultMultiplier);
        p = p == null ? fallback : p;
        s = s == null ? fallback : s;
        t = t == null ? fallback : t;
        return (p.value() + (0.5 * s.value()) + (0.5 * t.value())) * ((p.multiplier() + s.multiplier() + t.multiplier()) / 3);
    }
}
