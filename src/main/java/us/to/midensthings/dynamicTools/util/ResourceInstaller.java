package us.to.midensthings.dynamicTools.util;

import us.to.midensthings.dynamicTools.DynamicTools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("Bundled resource " + resourcePath + " is missing from the plugin jar");
                return false;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not copy " + resourcePath + " to " + target, e);
            return false;
        }
    }
}
