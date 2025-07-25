package io.github.ozkanpakdil.grepwise.service.impl;

import io.github.ozkanpakdil.grepwise.plugin.AbstractPlugin;
import io.github.ozkanpakdil.grepwise.plugin.Plugin;
import io.github.ozkanpakdil.grepwise.plugin.PluginRegistry;
import io.github.ozkanpakdil.grepwise.service.PluginManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the PluginManagerService interface.
 * This service manages the lifecycle of plugins in the GrepWise application.
 */
@Service
public class PluginManagerServiceImpl implements PluginManagerService {

    private static final Logger logger = LoggerFactory.getLogger(PluginManagerServiceImpl.class);

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${grepwise.plugins.directory:plugins}")
    private String pluginsDirectory;

    @Value("${grepwise.plugins.scan-classpath:true}")
    private boolean scanClasspath;

    /**
     * Initializes the plugin manager and loads all available plugins.
     * This method is called automatically after the bean is constructed.
     */
    @PostConstruct
    @Override
    public void initialize() {
        logger.info("Initializing PluginManagerService");

        // Create plugins directory if it doesn't exist
        Path pluginsPath = Paths.get(pluginsDirectory);
        if (!Files.exists(pluginsPath)) {
            try {
                Files.createDirectories(pluginsPath);
                logger.info("Created plugins directory: {}", pluginsPath.toAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to create plugins directory: {}", e.getMessage(), e);
            }
        }

        // Load plugins from the plugins directory
        try {
            List<Plugin> plugins = scanForPlugins(pluginsPath);
            logger.info("Found {} plugins in directory: {}", plugins.size(), pluginsPath.toAbsolutePath());
            for (Plugin plugin : plugins) {
                registerPlugin(plugin);
            }
        } catch (Exception e) {
            logger.error("Error scanning for plugins: {}", e.getMessage(), e);
        }

        // Scan classpath for plugins if enabled
        if (scanClasspath) {
            try {
                List<Plugin> classpathPlugins = scanClasspathForPlugins();
                logger.info("Found {} plugins in classpath", classpathPlugins.size());
                for (Plugin plugin : classpathPlugins) {
                    registerPlugin(plugin);
                }
            } catch (Exception e) {
                logger.error("Error scanning classpath for plugins: {}", e.getMessage(), e);
            }
        }

        logger.info("PluginManagerService initialized with {} plugins", pluginRegistry.getAllPlugins().size());
    }

    /**
     * Loads a plugin from the specified JAR file.
     *
     * @param jarPath Path to the JAR file containing the plugin
     * @return The loaded plugin, or empty if loading failed
     */
    @Override
    public Optional<Plugin> loadPlugin(Path jarPath) {
        if (jarPath == null || !Files.exists(jarPath)) {
            logger.warn("Invalid JAR path: {}", jarPath);
            return Optional.empty();
        }

        logger.info("Loading plugin from JAR: {}", jarPath);

        try {
            // Create a URL class loader for the JAR file
            URL jarUrl = jarPath.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarUrl},
                    getClass().getClassLoader()
            );

            // Find plugin classes in the JAR
            List<Class<?>> pluginClasses = findPluginClassesInJar(jarPath.toFile(), classLoader);
            if (pluginClasses.isEmpty()) {
                logger.warn("No plugin classes found in JAR: {}", jarPath);
                return Optional.empty();
            }

            // Instantiate the first plugin class found
            Class<?> pluginClass = pluginClasses.get(0);
            Plugin plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
            logger.info("Loaded plugin: {} ({})", plugin.getName(), plugin.getId());

            return Optional.of(plugin);
        } catch (Exception e) {
            logger.error("Failed to load plugin from JAR {}: {}", jarPath, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Registers a plugin with the plugin manager.
     *
     * @param plugin The plugin to register
     * @return true if registration was successful, false otherwise
     */
    @Override
    public boolean registerPlugin(Plugin plugin) {
        if (plugin == null) {
            logger.warn("Attempted to register null plugin");
            return false;
        }

        logger.info("Registering plugin: {} ({})", plugin.getName(), plugin.getId());
        return pluginRegistry.registerPlugin(plugin);
    }

    /**
     * Unregisters a plugin from the plugin manager.
     *
     * @param pluginId The ID of the plugin to unregister
     * @return true if unregistration was successful, false otherwise
     */
    @Override
    public boolean unregisterPlugin(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            logger.warn("Attempted to unregister plugin with null or empty ID");
            return false;
        }

        // Disable the plugin first if it's enabled
        if (pluginRegistry.isPluginEnabled(pluginId)) {
            disablePlugin(pluginId);
        }

        logger.info("Unregistering plugin: {}", pluginId);
        return pluginRegistry.unregisterPlugin(pluginId);
    }

    /**
     * Enables a plugin.
     * This will initialize and start the plugin if it's not already running.
     *
     * @param pluginId The ID of the plugin to enable
     * @return true if the plugin was enabled successfully, false otherwise
     */
    @Override
    public boolean enablePlugin(String pluginId) {
        Optional<Plugin> optionalPlugin = pluginRegistry.getPluginById(pluginId);
        if (!optionalPlugin.isPresent()) {
            logger.warn("Plugin with ID {} not found", pluginId);
            return false;
        }

        Plugin plugin = optionalPlugin.get();
        if (pluginRegistry.isPluginEnabled(pluginId)) {
            logger.info("Plugin {} is already enabled", pluginId);
            return true;
        }

        logger.info("Enabling plugin: {} ({})", plugin.getName(), pluginId);

        try {
            // Initialize and start the plugin
            plugin.initialize();
            plugin.start();

            // Mark the plugin as enabled
            pluginRegistry.setPluginEnabled(pluginId, true);
            logger.info("Plugin {} enabled successfully", pluginId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to enable plugin {}: {}", pluginId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Disables a plugin.
     * This will stop the plugin if it's running.
     *
     * @param pluginId The ID of the plugin to disable
     * @return true if the plugin was disabled successfully, false otherwise
     */
    @Override
    public boolean disablePlugin(String pluginId) {
        Optional<Plugin> optionalPlugin = pluginRegistry.getPluginById(pluginId);
        if (!optionalPlugin.isPresent()) {
            logger.warn("Plugin with ID {} not found", pluginId);
            return false;
        }

        Plugin plugin = optionalPlugin.get();
        if (!pluginRegistry.isPluginEnabled(pluginId)) {
            logger.info("Plugin {} is already disabled", pluginId);
            return true;
        }

        logger.info("Disabling plugin: {} ({})", plugin.getName(), pluginId);

        try {
            // Stop the plugin
            plugin.stop();

            // Mark the plugin as disabled
            pluginRegistry.setPluginEnabled(pluginId, false);
            logger.info("Plugin {} disabled successfully", pluginId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to disable plugin {}: {}", pluginId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns a list of all registered plugins.
     *
     * @return List of all plugins
     */
    @Override
    public List<Plugin> getAllPlugins() {
        return pluginRegistry.getAllPlugins();
    }

    /**
     * Returns a list of all plugins of the specified type.
     *
     * @param pluginClass The class or interface that plugins should implement
     * @param <T>         The type of plugin to retrieve
     * @return List of plugins of the specified type
     */
    @Override
    public <T extends Plugin> List<T> getPluginsByType(Class<T> pluginClass) {
        return pluginRegistry.getPluginsByType(pluginClass);
    }

    /**
     * Returns a plugin by its ID.
     *
     * @param pluginId The ID of the plugin to retrieve
     * @return The plugin, or empty if not found
     */
    @Override
    public Optional<Plugin> getPluginById(String pluginId) {
        return pluginRegistry.getPluginById(pluginId);
    }

    /**
     * Returns the configuration for a plugin.
     *
     * @param pluginId The ID of the plugin
     * @return The plugin configuration, or empty if not found
     */
    @Override
    public Optional<Map<String, Object>> getPluginConfiguration(String pluginId) {
        return pluginRegistry.getPluginConfiguration(pluginId);
    }

    /**
     * Updates the configuration for a plugin.
     *
     * @param pluginId      The ID of the plugin
     * @param configuration The new configuration
     * @return true if the configuration was updated successfully, false otherwise
     */
    @Override
    public boolean updatePluginConfiguration(String pluginId, Map<String, Object> configuration) {
        if (pluginId == null || pluginId.isEmpty()) {
            logger.warn("Attempted to update configuration for plugin with null or empty ID");
            return false;
        }

        if (!pluginRegistry.getPluginById(pluginId).isPresent()) {
            logger.warn("Plugin with ID {} not found", pluginId);
            return false;
        }

        if (configuration == null) {
            logger.warn("Attempted to update plugin {} with null configuration", pluginId);
            return false;
        }

        logger.info("Updating configuration for plugin: {}", pluginId);
        return pluginRegistry.setPluginConfiguration(pluginId, configuration);
    }

    /**
     * Scans for plugins in the specified directory.
     *
     * @param directory The directory to scan for plugin JAR files
     * @return List of discovered plugins
     */
    @Override
    public List<Plugin> scanForPlugins(Path directory) {
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            logger.warn("Invalid plugins directory: {}", directory);
            return Collections.emptyList();
        }

        logger.info("Scanning for plugins in directory: {}", directory.toAbsolutePath());

        try (Stream<Path> paths = Files.list(directory)) {
            List<Plugin> plugins = new ArrayList<>();

            // Find all JAR files in the directory
            List<Path> jarFiles = paths
                    .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                    .collect(Collectors.toList());

            // Load plugins from each JAR file
            for (Path jarPath : jarFiles) {
                Optional<Plugin> plugin = loadPlugin(jarPath);
                plugin.ifPresent(plugins::add);
            }

            return plugins;
        } catch (IOException e) {
            logger.error("Error scanning for plugins: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Reloads all plugins.
     * This will stop all running plugins, unregister them, and then load and register them again.
     *
     * @return true if all plugins were reloaded successfully, false otherwise
     */
    @Override
    public boolean reloadAllPlugins() {
        logger.info("Reloading all plugins");

        // Get all registered plugins
        List<Plugin> plugins = pluginRegistry.getAllPlugins();
        
        // Disable all enabled plugins
        for (Plugin plugin : plugins) {
            if (pluginRegistry.isPluginEnabled(plugin.getId())) {
                disablePlugin(plugin.getId());
            }
        }

        // Clear the registry
        pluginRegistry.clear();

        // Re-initialize the plugin manager
        initialize();

        return true;
    }

    /**
     * Scans the classpath for plugins.
     *
     * @return List of discovered plugins
     * @throws IOException if an I/O error occurs
     */
    private List<Plugin> scanClasspathForPlugins() throws IOException {
        logger.info("Scanning classpath for plugins");
        List<Plugin> plugins = new ArrayList<>();

        // Use Spring's resource resolver to find classes
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:**/*.class");

        for (Resource resource : resources) {
            try {
                // Convert resource path to class name
                String path = resource.getURL().toString();
                if (!path.contains("/classes/")) {
                    continue; // Skip non-application classes
                }

                String className = path.substring(path.indexOf("/classes/") + 9, path.length() - 6)
                        .replace('/', '.');

                // Load the class
                Class<?> clazz = Class.forName(className);

                // Check if it's a plugin implementation (not an interface or abstract class)
                if (Plugin.class.isAssignableFrom(clazz) && 
                        !clazz.isInterface() && 
                        !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers()) &&
                        !clazz.equals(AbstractPlugin.class)) {
                    
                    // Instantiate the plugin
                    try {
                        Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
                        plugins.add(plugin);
                        logger.info("Found plugin in classpath: {} ({})", plugin.getName(), plugin.getId());
                    } catch (Exception e) {
                        logger.warn("Failed to instantiate plugin class {}: {}", className, e.getMessage());
                    }
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // Skip classes that can't be loaded
                logger.debug("Skipping class: {}", e.getMessage());
            } catch (Exception e) {
                logger.warn("Error processing class: {}", e.getMessage());
            }
        }

        return plugins;
    }

    /**
     * Finds plugin classes in a JAR file.
     *
     * @param jarFile     The JAR file to search
     * @param classLoader The class loader to use for loading classes
     * @return List of plugin classes found in the JAR
     * @throws IOException if an I/O error occurs
     */
    private List<Class<?>> findPluginClassesInJar(File jarFile, ClassLoader classLoader) throws IOException {
        List<Class<?>> pluginClasses = new ArrayList<>();

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Skip directories and non-class files
                if (entry.isDirectory() || !entryName.endsWith(".class")) {
                    continue;
                }

                // Convert entry name to class name
                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');

                try {
                    // Load the class
                    Class<?> clazz = Class.forName(className, false, classLoader);

                    // Check if it's a plugin implementation (not an interface or abstract class)
                    if (Plugin.class.isAssignableFrom(clazz) && 
                            !clazz.isInterface() && 
                            !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                        pluginClasses.add(clazz);
                        logger.debug("Found plugin class in JAR: {}", className);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Skip classes that can't be loaded
                    logger.debug("Skipping class: {}", e.getMessage());
                } catch (Exception e) {
                    logger.warn("Error processing class {}: {}", className, e.getMessage());
                }
            }
        }

        return pluginClasses;
    }
}