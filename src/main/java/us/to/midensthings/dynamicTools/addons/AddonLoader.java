package us.to.midensthings.dynamicTools.addons;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import us.to.midensthings.dynamicTools.DynamicTools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.stream.Stream;

/***
 * Loads the addon jars in plugins/DynamicTools/addons. See DynamicToolsAddon for how to make an addon.
 */
public class AddonLoader {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private record LoadedAddon(String name, DynamicToolsAddon addon, AddonClassLoader classLoader) {}

    // Addon name -> addon, in load order
    private final Map<String, LoadedAddon> addons = new LinkedHashMap<>();

    /***
     * Loads every addon in the addons folder and runs its onLoad. An addon that fails to load is skipped
     * without affecting the others.
     */
    public void loadAddons() {
        Path folder = plugin.getDataFolder().toPath().resolve("addons");
        List<Path> jars;
        try {
            Files.createDirectories(folder);
            try (Stream<Path> files = Files.list(folder)) {
                jars = files.filter(file -> file.getFileName().toString().endsWith(".jar")).sorted().toList();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not read the addons folder", e);
            return;
        }

        for (Path jar : jars) {
            loadAddon(jar);
        }
        if (!addons.isEmpty()) {
            plugin.getLogger().info("Loaded " + addons.size() + " addon(s): " + String.join(", ", addons.keySet()));
        }
    }

    private void loadAddon(Path jar) {
        String fileName = jar.getFileName().toString();
        YamlConfiguration description = readDescription(jar);
        if (description == null) {
            plugin.getLogger().warning("Skipping addon " + fileName + ", it has no addon.yml");
            return;
        }
        String name = description.getString("name");
        String mainClass = description.getString("main");
        if (name == null || mainClass == null) {
            plugin.getLogger().warning("Skipping addon " + fileName + ", its addon.yml needs a name and main");
            return;
        }
        if (addons.containsKey(name)) {
            plugin.getLogger().warning("Skipping addon " + fileName + ", an addon named " + name + " is already loaded");
            return;
        }

        // The class loaders of the plugins the addon can use
        List<ClassLoader> dependencyLoaders = new ArrayList<>();
        for (String dependency : description.getStringList("depend")) {
            Plugin dependencyPlugin = Bukkit.getPluginManager().getPlugin(dependency);
            if (dependencyPlugin == null) {
                plugin.getLogger().warning("Skipping addon " + name + ", it needs the plugin " + dependency + " which isn't installed");
                return;
            }
            dependencyLoaders.add(dependencyPlugin.getClass().getClassLoader());
        }
        for (String dependency : description.getStringList("softdepend")) {
            Plugin dependencyPlugin = Bukkit.getPluginManager().getPlugin(dependency);
            if (dependencyPlugin != null) {
                dependencyLoaders.add(dependencyPlugin.getClass().getClassLoader());
            }
        }

        AddonClassLoader classLoader = null;
        try {
            classLoader = new AddonClassLoader(jar, plugin.getClass().getClassLoader(), dependencyLoaders);
            Class<?> main = Class.forName(mainClass, true, classLoader);
            if (!DynamicToolsAddon.class.isAssignableFrom(main)) {
                plugin.getLogger().warning("Skipping addon " + name + ", " + mainClass + " doesn't implement DynamicToolsAddon");
                classLoader.close();
                return;
            }
            DynamicToolsAddon addon = (DynamicToolsAddon) main.getDeclaredConstructor().newInstance();
            addon.onLoad(plugin);
            addons.put(name, new LoadedAddon(name, addon, classLoader));
            plugin.debug("Loaded addon " + name + " " + description.getString("version", "") + " from " + fileName);
        } catch (Throwable e) {
            // Throwable so a missing class (NoClassDefFoundError) in one addon doesn't stop the plugin from enabling
            plugin.getLogger().log(Level.SEVERE, "Could not load addon " + name + " from " + fileName, e);
            closeQuietly(classLoader);
        }
    }

    private YamlConfiguration readDescription(Path jar) {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            JarEntry entry = jarFile.getJarEntry("addon.yml");
            if (entry == null) {
                return null;
            }
            try (InputStream in = jarFile.getInputStream(entry)) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read " + jar.getFileName(), e);
            return null;
        }
    }

    /***
     * Runs every addon's onDisable and closes their jars.
     */
    public void disableAddons() {
        for (LoadedAddon loaded : addons.values()) {
            try {
                loaded.addon().onDisable();
            } catch (Throwable e) {
                plugin.getLogger().log(Level.SEVERE, "Error while disabling addon " + loaded.name(), e);
            }
            closeQuietly(loaded.classLoader());
        }
        addons.clear();
    }

    private void closeQuietly(AddonClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException ignored) {
        }
    }

    public List<String> getAddonNames() {
        return List.copyOf(addons.keySet());
    }
}
