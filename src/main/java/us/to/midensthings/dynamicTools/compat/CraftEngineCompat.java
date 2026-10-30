package us.to.midensthings.dynamicTools.compat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import us.to.midensthings.dynamicTools.util.ResourceInstaller;

import java.nio.file.Path;
import java.util.List;

public class CraftEngineCompat {
    public static final String NAMESPACE = "dynamictools";

    private static final String RESOURCE_FOLDER = "CraftEngineConfigs/";
    private static final List<String> CONFIG_FILES = List.of(
            "dynamicToolsParts.yml",
            "dynamicToolsPartCrafter.yml",
            "dynamicToolsPartAssembler.yml"
    );

    /***
     * Copies the DynamicTools pack into plugins/CraftEngine/resources/dynamictools, with pack.yml at its root
     * and the item and block configs in its configuration folder. Files that already exist are not overwritten.
     * @return true if any files were added, meaning CraftEngine needs to be reloaded to pick them up.
     */
    public static boolean installConfigs() {
        Plugin craftEngine = Bukkit.getPluginManager().getPlugin("CraftEngine");
        if (craftEngine == null) {
            return false;
        }

        Path packFolder = craftEngine.getDataFolder().toPath().resolve("resources").resolve(NAMESPACE);
        Path configurationFolder = packFolder.resolve("configuration");

        boolean added = ResourceInstaller.copyResourceIfAbsent(RESOURCE_FOLDER + "pack.yml", packFolder.resolve("pack.yml"));
        for (String file : CONFIG_FILES) {
            added |= ResourceInstaller.copyResourceIfAbsent(RESOURCE_FOLDER + file, configurationFolder.resolve(file));
        }
        return added;
    }
}
