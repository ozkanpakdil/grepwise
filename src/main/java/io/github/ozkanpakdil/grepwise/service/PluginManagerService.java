package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.plugin.Plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing plugins in the GrepWise application.
 * This service handles plugin discovery, loading, initialization, and lifecycle management.
 */
public interface PluginManagerService {

    /**
     * Initializes the plugin manager and loads all available plugins.
     * This method should be called during application startup.
     */
    void initialize();

    /**
     * Loads a plugin from the specified JAR file.
     *
     * @param jarPath Path to the JAR file containing the plugin
     * @return The loaded plugin, or empty if loading failed
     */
    Optional<Plugin> loadPlugin(Path jarPath);

    /**
     * Registers a plugin with the plugin manager.
     *
     * @param plugin The plugin to register
     * @return true if registration was successful, false otherwise
     */
    boolean registerPlugin(Plugin plugin);

    /**
     * Unregisters a plugin from the plugin manager.
     *
     * @param pluginId The ID of the plugin to unregister
     * @return true if unregistration was successful, false otherwise
     */
    boolean unregisterPlugin(String pluginId);

    /**
     * Enables a plugin.
     * This will initialize and start the plugin if it's not already running.
     *
     * @param pluginId The ID of the plugin to enable
     * @return true if the plugin was enabled successfully, false otherwise
     */
    boolean enablePlugin(String pluginId);

    /**
     * Disables a plugin.
     * This will stop the plugin if it's running.
     *
     * @param pluginId The ID of the plugin to disable
     * @return true if the plugin was disabled successfully, false otherwise
     */
    boolean disablePlugin(String pluginId);

    /**
     * Returns a list of all registered plugins.
     *
     * @return List of all plugins
     */
    List<Plugin> getAllPlugins();

    /**
     * Returns a list of all plugins of the specified type.
     *
     * @param pluginClass The class or interface that plugins should implement
     * @param <T>         The type of plugin to retrieve
     * @return List of plugins of the specified type
     */
    <T extends Plugin> List<T> getPluginsByType(Class<T> pluginClass);

    /**
     * Returns a plugin by its ID.
     *
     * @param pluginId The ID of the plugin to retrieve
     * @return The plugin, or empty if not found
     */
    Optional<Plugin> getPluginById(String pluginId);

    /**
     * Returns the configuration for a plugin.
     *
     * @param pluginId The ID of the plugin
     * @return The plugin configuration, or empty if not found
     */
    Optional<Map<String, Object>> getPluginConfiguration(String pluginId);

    /**
     * Updates the configuration for a plugin.
     *
     * @param pluginId      The ID of the plugin
     * @param configuration The new configuration
     * @return true if the configuration was updated successfully, false otherwise
     */
    boolean updatePluginConfiguration(String pluginId, Map<String, Object> configuration);

    /**
     * Scans for plugins in the specified directory.
     *
     * @param directory The directory to scan for plugin JAR files
     * @return List of discovered plugins
     */
    List<Plugin> scanForPlugins(Path directory);

    /**
     * Reloads all plugins.
     * This will stop all running plugins, unregister them, and then load and register them again.
     *
     * @return true if all plugins were reloaded successfully, false otherwise
     */
    boolean reloadAllPlugins();
}