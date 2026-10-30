package us.to.midensthings.dynamicTools.util;

import org.bukkit.NamespacedKey;
import us.to.midensthings.dynamicTools.DynamicTools;

public class NamespacedKeys {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);


    public static NamespacedKey partMaterial = new NamespacedKey(plugin,"part-material");
    public static NamespacedKey partType = new NamespacedKey(plugin,"part-type");

    public static NamespacedKey toolType = new NamespacedKey(plugin,"tool-type");
    public static NamespacedKey primaryMaterial = new NamespacedKey(plugin,"primary-material");
    public static NamespacedKey secondaryMaterial = new NamespacedKey(plugin,"secondary-material");
    public static NamespacedKey tertiaryMaterial = new NamespacedKey(plugin,"tertiary-material");

    public static NamespacedKey modifiers = new NamespacedKey(plugin,"modifiers");

}
