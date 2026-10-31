package us.to.midensthings.dynamicTools.compat;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import us.to.midensthings.dynamicTools.parts.PartType;
import us.to.midensthings.dynamicTools.util.ResourceInstaller;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class ItemsAdderCompat {
    public static final String NAMESPACE = "dynamictools";

    private static final String RESOURCE_FOLDER = "ItemsAdderConfigs/";
    private static final List<String> CONFIG_FILES = List.of(
            "dynamicToolsParts.yml"
    );

    /***
     * Copies the DynamicTools part configs into plugins/ItemsAdder/contents/dynamictools/configs, and the configs and
     * textures of the custom blocks enabled in config.yml. Files that already exist are not overwritten.
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
        added |= CustomBlocks.install(RESOURCE_FOLDER, configsFolder, getAssetsFolder());
        return added;
    }

    /***
     * @return a new instance of the custom block's item, or null if ItemsAdder doesn't have it loaded.
     */
    public static ItemStack getBlockItem(CustomBlocks.CustomBlock block) {
        CustomStack stack = CustomStack.getInstance(NAMESPACE + ":" + block.id());
        return stack == null ? null : stack.getItemStack();
    }

    /***
     * @return the folder ItemsAdder merges into its resource pack as assets/dynamictools,
     * plugins/ItemsAdder/contents/dynamictools/resourcepack/assets/dynamictools. Null if ItemsAdder isn't installed.
     */
    public static Path getAssetsFolder() {
        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null) {
            return null;
        }
        return itemsAdder.getDataFolder().toPath().resolve("contents").resolve(NAMESPACE)
                .resolve("resourcepack").resolve("assets").resolve(NAMESPACE);
    }

    /***
     * @return a new instance of the part's custom item, or null if ItemsAdder doesn't have it loaded.
     */
    public ItemStack getPartStack(PartType partType) {
        CustomStack stack = CustomStack.getInstance(NAMESPACE+":"+partType.name().toLowerCase(Locale.ROOT));
        return stack == null ? null : stack.getItemStack();
    }

    /***
     * @return the namespaced ID of the ItemsAdder item (e.g. "my_items:ruby"), or null if it isn't an ItemsAdder item.
     */
    public static String getCustomItemId(ItemStack item) {
        CustomStack stack = CustomStack.byItemStack(item);
        return stack == null ? null : stack.getNamespacedID();
    }
}
