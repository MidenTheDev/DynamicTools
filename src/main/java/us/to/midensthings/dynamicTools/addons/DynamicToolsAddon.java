package us.to.midensthings.dynamicTools.addons;

import us.to.midensthings.dynamicTools.DynamicTools;

/***
 * The main class of a DynamicTools addon. Addons are jars in plugins/DynamicTools/addons with an addon.yml at the root:
 * <pre>
 * name: MyAddon
 * main: com.example.MyAddon          # the class implementing DynamicToolsAddon, needs a no-argument constructor
 * version: 1.0
 * depend: [ProjectKorra]             # (optional) plugins the addon needs, it isn't loaded if one is missing
 * softdepend: [WorldGuard]           # (optional) plugins the addon can use if they're installed
 * </pre>
 * Addons can use the classes of DynamicTools, the Paper API, and the plugins listed in depend and softdepend,
 * without DynamicTools depending on those plugins.
 * <p>
 * DynamicTools supports Folia, where the Bukkit scheduler doesn't work. Addons should schedule tasks with
 * us.to.midensthings.dynamicTools.util.Schedulers (or Paper's entity, global region and async schedulers) instead.
 */
public interface DynamicToolsAddon {

    /***
     * Called while DynamicTools is enabling, before materials.yml is loaded, so modifiers registered here
     * (with plugin.getModifierRegistry().registerModifier) can be used by materials. Other plugins may not
     * be enabled yet, but their classes can be used.
     */
    void onLoad(DynamicTools plugin);

    /***
     * Called when DynamicTools is disabled.
     */
    default void onDisable() {
    }
}
