package us.to.midensthings.dynamicTools.addons;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

/***
 * Loads an addon's classes. Classes are looked up in DynamicTools (which includes the Paper API) first,
 * then in the addon's own jar, then in the plugins the addon depends on.
 * <p>
 * Paper only lets a plugin see the classes of plugins it depends on, so going straight to the dependency plugins'
 * class loaders is what lets addons use plugins that DynamicTools itself doesn't depend on.
 */
public class AddonClassLoader extends URLClassLoader {
    static {
        registerAsParallelCapable();
    }

    private final List<ClassLoader> dependencyLoaders;

    public AddonClassLoader(Path jar, ClassLoader parent, List<ClassLoader> dependencyLoaders) throws IOException {
        super(new URL[]{jar.toUri().toURL()}, parent);
        this.dependencyLoaders = dependencyLoaders;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = findClassInOrder(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    private Class<?> findClassInOrder(String name) throws ClassNotFoundException {
        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        for (ClassLoader dependencyLoader : dependencyLoaders) {
            try {
                return dependencyLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }
}
