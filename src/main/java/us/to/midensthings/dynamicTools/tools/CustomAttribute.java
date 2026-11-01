package us.to.midensthings.dynamicTools.tools;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import us.to.midensthings.dynamicTools.DynamicTools;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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
 */
public record CustomAttribute(String id, NamespacedKey key, boolean displayOnTools, String displayName,
                              double defaultValue, double defaultMultiplier) {
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
                    config.getDouble(id + ".default-multiplier", 1.0)));
        }
        return attributes;
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
