package us.to.midensthings.dynamicTools.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import us.to.midensthings.dynamicTools.DynamicTools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;

public class ResourceInstaller {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    /***
     * Copies a file bundled in the plugin jar to the given path. Existing files are left untouched so edits made by server owners are kept.
     * @param resourcePath the path of the file inside the jar
     * @param target where to copy the file to
     * @return true if the file was copied, false if it already existed or could not be copied.
     */
    public static boolean copyResourceIfAbsent(String resourcePath, Path target) {
        if (Files.exists(target)) {
            return false;
        }
        return copyResource(resourcePath, target);
    }

    /***
     * Copies a bundled config to the given path if it doesn't exist yet, or if the existing file is an empty placeholder
     * (no items or blocks defined), like the block configs older versions installed. Configs with content are kept.
     * @return true if the file was copied
     */
    public static boolean copyResourceReplacingPlaceholder(String resourcePath, Path target) {
        if (Files.exists(target) && !isPlaceholderConfig(target)) {
            return false;
        }
        return copyResource(resourcePath, target);
    }

    private static boolean isPlaceholderConfig(Path file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        return isEmptySection(config.getConfigurationSection("items")) && isEmptySection(config.getConfigurationSection("blocks"));
    }

    private static boolean isEmptySection(ConfigurationSection section) {
        return section == null || section.getKeys(false).isEmpty();
    }

    private static boolean copyResource(String resourcePath, Path target) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("Bundled resource " + resourcePath + " is missing from the plugin jar");
                return false;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not copy " + resourcePath + " to " + target, e);
            return false;
        }
    }
}
