package us.to.midensthings.dynamicTools.tools;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.util.NamespacedKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/***
 * A value from a material's custom-attributes in materials.yml, added to the persistent data container of the tools
 * made with that material, e.g. for other plugins to read.
 */
public record CustomItemData(NamespacedKey key, PersistentDataType<?, ?> type, Object value) {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    // DynamicTools' own keys, which custom data can't overwrite
    private static final Set<NamespacedKey> RESERVED_KEYS = Set.of(
            NamespacedKeys.partMaterial, NamespacedKeys.partType, NamespacedKeys.toolType,
            NamespacedKeys.primaryMaterial, NamespacedKeys.secondaryMaterial, NamespacedKeys.tertiaryMaterial,
            NamespacedKeys.modifiers, NamespacedKeys.bonusModifiers);

    @SuppressWarnings("unchecked")
    public void apply(PersistentDataContainer pdc) {
        pdc.set(key, (PersistentDataType<?, Object>) type, value);
    }

    /***
     * Reads a material's custom-attributes section. Invalid entries are skipped with a warning.
     * <pre>
     * custom-attributes:
     *   any_name:
     *     namespaced-key: "myplugin:some_key"   # a key without a namespace uses dynamictools
     *     data-type: DOUBLE                     # BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN or STRING
     *     value: 1.5
     * </pre>
     */
    public static List<CustomItemData> load(ConfigurationSection section, String material) {
        List<CustomItemData> data = new ArrayList<>();
        if (section == null) {
            return data;
        }
        for (String name : section.getKeys(false)) {
            String where = "custom-attributes." + name + " of material " + material;
            String keyString = section.getString(name + ".namespaced-key", "");
            NamespacedKey key = NamespacedKey.fromString(keyString, plugin);
            if (key == null) {
                plugin.getLogger().warning("Invalid namespaced-key \"" + keyString + "\" in " + where
                        + ". Keys can only use lowercase letters, numbers, _ - . and /, like myplugin:some_key");
                continue;
            }
            if (RESERVED_KEYS.contains(key)) {
                plugin.getLogger().warning("The namespaced-key " + key + " in " + where + " is used by DynamicTools itself and can't be set");
                continue;
            }

            String typeName = section.getString(name + ".data-type", "").toUpperCase(Locale.ROOT);
            Object rawValue = section.get(name + ".value");
            if (rawValue == null) {
                plugin.getLogger().warning("Missing value in " + where);
                continue;
            }
            CustomItemData entry = switch (typeName) {
                case "BYTE" -> rawValue instanceof Number n ? new CustomItemData(key, PersistentDataType.BYTE, n.byteValue()) : null;
                case "SHORT" -> rawValue instanceof Number n ? new CustomItemData(key, PersistentDataType.SHORT, n.shortValue()) : null;
                case "INTEGER" -> rawValue instanceof Number n ? new CustomItemData(key, PersistentDataType.INTEGER, n.intValue()) : null;
                case "LONG" -> rawValue instanceof Number n ? new CustomItemData(key, PersistentDataType.LONG, n.longValue()) : null;
                case "FLOAT" -> rawValue instanceof Number n ? new CustomItemData(key, PersistentDataType.FLOAT, n.floatValue()) : null;
                case "DOUBLE" -> rawValue instanceof Number n ? new CustomItemData(key, PersistentDataType.DOUBLE, n.doubleValue()) : null;
                case "BOOLEAN" -> rawValue instanceof Boolean b ? new CustomItemData(key, PersistentDataType.BOOLEAN, b) : null;
                case "STRING" -> new CustomItemData(key, PersistentDataType.STRING, String.valueOf(rawValue));
                default -> {
                    plugin.getLogger().warning("Unknown data-type \"" + typeName + "\" in " + where
                            + ". Use BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN or STRING");
                    yield null;
                }
            };
            if (entry == null) {
                if (!typeName.isEmpty()) {
                    plugin.getLogger().warning("The value " + rawValue + " in " + where + " isn't a valid " + typeName);
                }
                continue;
            }
            data.add(entry);
        }
        return data;
    }
}
