package us.to.midensthings.dynamicTools.util;

import org.bukkit.NamespacedKey;
import us.to.midensthings.dynamicTools.DynamicTools;

public class NamespacedKeys {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);


    public static NamespacedKey partMaterial = new NamespacedKey(plugin,"part-material");
    public static NamespacedKey partType = new NamespacedKey(plugin,"part-type");

}
