package us.to.midensthings.dynamicTools.compat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import us.to.midensthings.dynamicTools.util.ResourceInstaller;

import java.nio.file.Path;
import java.util.List;

public class ItemsAdderCompat {
    public static final String NAMESPACE = "dynamictools";

    private static final String RESOURCE_FOLDER = "ItemsAdderConfigs/";
    private static final List<String> CONFIG_FILES = List.of(
            "dynamicToolsParts.yml",
            "dynamicToolsPartCrafter.yml",
            "dynamicToolsPartAssembler.yml"
    );

    /***
     * Copies the DynamicTools item and block configs into plugins/ItemsAdder/contents/dynamictools/configs.
     * Files that already exist are not overwritten.
     * @return true if any files were added, meaning ItemsAdder needs to be reloaded to pick them up.
     */
    public static boolean installConfigs() {
        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null) {
            return false;
        }

        Path configsFolder = itemsAdder.getDataFolder().toPath().resolve("contents").resolve(NAMESPACE).resolve("configs");
        boolean added = false;
        for (String file : CONFIG_FILES) {
            added |= ResourceInstaller.copyResourceIfAbsent(RESOURCE_FOLDER + file, configsFolder.resolve(file));
        }
        return added;
    }
}
