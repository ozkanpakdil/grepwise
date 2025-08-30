package io.github.ozkanpakdil.grepwise.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing plugins in the GrepWise application.
 * This class stores all registered plugins and provides methods to retrieve them.
 */
@Component
public class PluginRegistry {

    private static final Logger logger = LoggerFactory.getLogger(PluginRegistry.class);

    // Map of plugin ID to plugin instance
    private final Map<String, Plugin> pluginsById = new ConcurrentHashMap<>();

    // Map of plugin type to set of plugin IDs
    private final Map<Class<?>, Set<String>> pluginsByType = new ConcurrentHashMap<>();

    // Map of plugin ID to plugin configuration
    private final Map<String, Map<String, Object>> pluginConfigurations = new ConcurrentHashMap<>();

    // Map of plugin ID to enabled status
    private final Map<String, Boolean> pluginEnabledStatus = new ConcurrentHashMap<>();

    /**
     * Registers a plugin with the registry.
     *
     * @param plugin The plugin to register
     * @return true if registration was successful, false otherwise
     */
    public boolean registerPlugin(Plugin plugin) {
        if (plugin == null) {
            logger.warn("Attempted to register null plugin");
            return false;
        }

        String pluginId = plugin.getId();
        if (pluginId == null || pluginId.isEmpty()) {
            logger.warn("Attempted to register plugin with null or empty ID");
            return false;
        }

        if (pluginsById.containsKey(pluginId)) {
            logger.warn("Plugin with ID {} is already registered", pluginId);
            return false;
        }

        logger.info("Registering plugin: {} ({})", plugin.getName(), pluginId);
        pluginsById.put(pluginId, plugin);
        pluginEnabledStatus.put(pluginId, false);

        // Register plugin with all its interfaces and superclasses
        registerPluginTypes(plugin);

        // Initialize the plugin
        try {
            plugin.initialize();
        } catch (Exception e) {
            logger.error("Failed to initialize plugin {} ({}): {}", plugin.getName(), pluginId, e.getMessage(), e);
            // Keep the plugin registered even if initialization fails
        }

        return true;
    }

    /**
     * Unregisters a plugin from the registry.
     * This will stop the plugin if it is running.
     *
     * @param pluginId The ID of the plugin to unregister
     * @return true if unregistration was successful, false otherwise
     */
    public boolean unregisterPlugin(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            logger.warn("Attempted to unregister plugin with null or empty ID");
            return false;
        }

        Plugin plugin = pluginsById.get(pluginId);
        if (plugin == null) {
            logger.warn("Plugin with ID {} is not registered", pluginId);
            return false;
        }

        logger.info("Unregistering plugin: {} ({})", plugin.getName(), pluginId);

        // Stop the plugin if it's running
        try {
            plugin.stop();
        } catch (Exception e) {
            logger.error("Failed to stop plugin {} ({}): {}", plugin.getName(), pluginId, e.getMessage(), e);
            // Continue with unregistration even if stopping fails
        }

        // Remove plugin from all type maps
        for (Map.Entry<Class<?>, Set<String>> entry : pluginsByType.entrySet()) {
            entry.getValue().remove(pluginId);
        }

        // Clean up empty type sets
        pluginsByType.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Remove plugin from other maps
        pluginsById.remove(pluginId);
        pluginConfigurations.remove(pluginId);
        pluginEnabledStatus.remove(pluginId);

        return true;
    }

    /**
     * Returns a plugin by its ID.
     *
     * @param pluginId The ID of the plugin to retrieve
     * @return The plugin, or empty if not found
     */
    public Optional<Plugin> getPluginById(String pluginId) {
        return Optional.ofNullable(pluginsById.get(pluginId));
    }

    /**
     * Returns all registered plugins.
     *
     * @return List of all plugins
     */
    public List<Plugin> getAllPlugins() {
        return new ArrayList<>(pluginsById.values());
    }

    /**
     * Returns all plugins of the specified type.
     *
     * @param pluginClass The class or interface that plugins should implement
     * @param <T>         The type of plugin to retrieve
     * @return List of plugins of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T extends Plugin> List<T> getPluginsByType(Class<T> pluginClass) {
        if (pluginClass == null) {
            return Collections.emptyList();
        }

        Set<String> pluginIds = pluginsByType.getOrDefault(pluginClass, Collections.emptySet());
        return pluginIds.stream()
                .map(pluginsById::get)
                .filter(pluginClass::isInstance)
                .map(plugin -> (T) plugin)
                .collect(Collectors.toList());
    }

    /**
     * Sets the configuration for a plugin.
     *
     * @param pluginId      The ID of the plugin
     * @param configuration The configuration to set
     * @return true if the configuration was set successfully, false otherwise
     */
    public boolean setPluginConfiguration(String pluginId, Map<String, Object> configuration) {
        if (pluginId == null || pluginId.isEmpty()) {
            logger.warn("Attempted to set configuration for plugin with null or empty ID");
            return false;
        }

        if (!pluginsById.containsKey(pluginId)) {
            logger.warn("Plugin with ID {} is not registered", pluginId);
            return false;
        }

        if (configuration == null) {
            logger.warn("Attempted to set null configuration for plugin {}", pluginId);
            return false;
        }

        pluginConfigurations.put(pluginId, new HashMap<>(configuration));
        return true;
    }

    /**
     * Returns the configuration for a plugin.
     *
     * @param pluginId The ID of the plugin
     * @return The plugin configuration, or empty if not found
     */
    public Optional<Map<String, Object>> getPluginConfiguration(String pluginId) {
        Map<String, Object> config = pluginConfigurations.get(pluginId);
        return config != null ? Optional.of(new HashMap<>(config)) : Optional.empty();
    }

    /**
     * Sets the enabled status for a plugin.
     *
     * @param pluginId The ID of the plugin
     * @param enabled  The enabled status to set
     * @return true if the status was set successfully, false otherwise
     */
    public boolean setPluginEnabled(String pluginId, boolean enabled) {
        if (pluginId == null || pluginId.isEmpty()) {
            logger.warn("Attempted to set enabled status for plugin with null or empty ID");
            return false;
        }

        if (!pluginsById.containsKey(pluginId)) {
            logger.warn("Plugin with ID {} is not registered", pluginId);
            return false;
        }

        pluginEnabledStatus.put(pluginId, enabled);
        return true;
    }

    /**
     * Returns whether a plugin is enabled.
     *
     * @param pluginId The ID of the plugin
     * @return true if the plugin is enabled, false otherwise
     */
    public boolean isPluginEnabled(String pluginId) {
        return pluginEnabledStatus.getOrDefault(pluginId, false);
    }

    /**
     * Returns all enabled plugins.
     *
     * @return List of all enabled plugins
     */
    public List<Plugin> getEnabledPlugins() {
        return pluginEnabledStatus.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(entry -> pluginsById.get(entry.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Clears the registry, removing all registered plugins.
     */
    public void clear() {
        pluginsById.clear();
        pluginsByType.clear();
        pluginConfigurations.clear();
        pluginEnabledStatus.clear();
    }

    /**
     * Registers a plugin with all its interfaces and superclasses.
     *
     * @param plugin The plugin to register
     */
    private void registerPluginTypes(Plugin plugin) {
        Class<?> pluginClass = plugin.getClass();
        String pluginId = plugin.getId();

        // Register with all interfaces and superclasses
        Set<Class<?>> types = getAllTypes(pluginClass);
        for (Class<?> type : types) {
            if (Plugin.class.isAssignableFrom(type)) {
                pluginsByType.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(pluginId);
            }
        }
    }

    /**
     * Returns all interfaces and superclasses of a class.
     *
     * @param clazz The class to get types for
     * @return Set of all interfaces and superclasses
     */
    private Set<Class<?>> getAllTypes(Class<?> clazz) {
        Set<Class<?>> types = new HashSet<>();
        types.add(clazz);

        // Add all interfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            types.add(iface);
            types.addAll(getAllTypes(iface));
        }

        // Add superclass
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            types.add(superclass);
            types.addAll(getAllTypes(superclass));
        }

        return types;
    }

    /**
     * Returns a plugin by its ID.
     *
     * @param pluginId The ID of the plugin to retrieve
     * @return The plugin, or null if not found
     */
    public Plugin getPlugin(String pluginId) {
        return getPluginById(pluginId).orElse(null);
    }

    /**
     * Returns all plugins of the specified type.
     *
     * @param pluginType The class or interface that plugins should implement
     * @param <T>        The type of plugin to retrieve
     * @return List of plugins of the specified type
     */
    public <T extends Plugin> List<T> getPlugins(Class<T> pluginType) {
        return getPluginsByType(pluginType);
    }

    /**
     * Starts all plugins.
     * All plugins will be started regardless of their enabled status.
     */
    public void startAllPlugins() {
        logger.info("Starting all plugins");
        getAllPlugins().forEach(plugin -> {
            try {
                logger.info("Starting plugin: {} ({})", plugin.getName(), plugin.getId());
                plugin.start();
            } catch (Exception e) {
                logger.error("Error starting plugin: {} ({})", plugin.getName(), plugin.getId(), e);
            }
        });
    }

    /**
     * Stops all plugins.
     * All plugins will be stopped regardless of their enabled status.
     */
    public void stopAllPlugins() {
        logger.info("Stopping all plugins");
        getAllPlugins().forEach(plugin -> {
            try {
                logger.info("Stopping plugin: {} ({})", plugin.getName(), plugin.getId());
                plugin.stop();
            } catch (Exception e) {
                logger.error("Error stopping plugin: {} ({})", plugin.getName(), plugin.getId(), e);
            }
        });
    }
}