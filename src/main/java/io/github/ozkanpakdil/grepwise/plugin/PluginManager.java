package io.github.ozkanpakdil.grepwise.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing plugins and their configurations.
 * This service provides higher-level management functions for plugins,
 * including configuration persistence and plugin lifecycle management.
 */
@Service
public class PluginManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${grepwise.plugins.config-dir:./plugins/config}")
    private String configDir;
    
    private final Map<String, PluginConfiguration> configurations = new ConcurrentHashMap<>();
    
    /**
     * Initializes the plugin manager.
     * This method is called automatically after the bean is created.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing plugin manager");
        
        // Create config directory if it doesn't exist
        createConfigDirectory();
        
        // Load plugin configurations
        loadConfigurations();
        
        // Start enabled plugins
        startEnabledPlugins();
    }
    
    /**
     * Creates the plugin configuration directory if it doesn't exist.
     */
    private void createConfigDirectory() {
        Path configPath = Paths.get(configDir);
        if (!Files.exists(configPath)) {
            try {
                Files.createDirectories(configPath);
                logger.info("Created plugin configuration directory: {}", configPath);
            } catch (IOException e) {
                logger.error("Failed to create plugin configuration directory: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Loads plugin configurations from disk.
     */
    private void loadConfigurations() {
        logger.info("Loading plugin configurations from {}", configDir);
        
        Path configPath = Paths.get(configDir);
        if (!Files.exists(configPath)) {
            logger.warn("Plugin configuration directory does not exist: {}", configPath);
            return;
        }
        
        try {
            Files.list(configPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(this::loadConfiguration);
        } catch (IOException e) {
            logger.error("Failed to list plugin configuration files: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Loads a plugin configuration from a file.
     * 
     * @param configFile The configuration file
     */
    private void loadConfiguration(Path configFile) {
        try {
            logger.info("Loading plugin configuration from {}", configFile);
            
            PluginConfiguration config = objectMapper.readValue(configFile.toFile(), PluginConfiguration.class);
            String pluginId = config.getPluginId();
            
            if (pluginId == null) {
                logger.warn("Plugin configuration file {} does not contain a plugin ID", configFile);
                return;
            }
            
            // Find the plugin
            Plugin plugin = pluginRegistry.getPlugin(pluginId);
            if (plugin == null) {
                logger.warn("Plugin with ID {} not found, but configuration exists", pluginId);
                // Still store the configuration in case the plugin is loaded later
            } else {
                config.setPlugin(plugin);
            }
            
            configurations.put(pluginId, config);
            
        } catch (IOException e) {
            logger.error("Failed to load plugin configuration from {}: {}", configFile, e.getMessage(), e);
        }
    }
    
    /**
     * Starts all enabled plugins.
     */
    private void startEnabledPlugins() {
        logger.info("Starting enabled plugins");
        
        for (Plugin plugin : pluginRegistry.getAllPlugins()) {
            String pluginId = plugin.getId();
            PluginConfiguration config = getConfiguration(pluginId);
            
            if (config.isEnabled()) {
                try {
                    plugin.start();
                    logger.info("Started plugin: {} ({})", plugin.getName(), pluginId);
                } catch (Exception e) {
                    logger.error("Failed to start plugin {}: {}", pluginId, e.getMessage(), e);
                }
            } else {
                logger.info("Plugin {} is disabled, not starting", pluginId);
            }
        }
    }
    
    /**
     * Returns the configuration for a plugin.
     * If no configuration exists, a default configuration is created.
     * 
     * @param pluginId The ID of the plugin
     * @return The plugin configuration
     */
    public PluginConfiguration getConfiguration(String pluginId) {
        return configurations.computeIfAbsent(pluginId, id -> {
            Plugin plugin = pluginRegistry.getPlugin(id);
            if (plugin != null) {
                return new PluginConfiguration(plugin);
            } else {
                PluginConfiguration config = new PluginConfiguration();
                config.setPluginId(id);
                return config;
            }
        });
    }
    
    /**
     * Returns all plugin configurations.
     * 
     * @return A list of all plugin configurations
     */
    public List<PluginConfiguration> getAllConfigurations() {
        return new ArrayList<>(configurations.values());
    }
    
    /**
     * Updates the configuration for a plugin.
     * 
     * @param config The updated configuration
     * @return true if the configuration was updated, false if the plugin was not found
     */
    public boolean updateConfiguration(PluginConfiguration config) {
        String pluginId = config.getPluginId();
        Plugin plugin = pluginRegistry.getPlugin(pluginId);
        
        if (plugin == null) {
            logger.warn("Cannot update configuration for unknown plugin: {}", pluginId);
            return false;
        }
        
        // Update the plugin reference
        config.setPlugin(plugin);
        
        // Store the configuration
        configurations.put(pluginId, config);
        
        // Save the configuration to disk
        saveConfiguration(config);
        
        // Handle enabled state change
        PluginConfiguration oldConfig = configurations.get(pluginId);
        boolean wasEnabled = oldConfig != null && oldConfig.isEnabled();
        boolean shouldBeEnabled = config.isEnabled();
        
        if (wasEnabled && !shouldBeEnabled) {
            // Plugin was enabled but should be disabled
            try {
                plugin.stop();
                logger.info("Stopped plugin: {} ({})", plugin.getName(), pluginId);
            } catch (Exception e) {
                logger.error("Failed to stop plugin {}: {}", pluginId, e.getMessage(), e);
            }
        } else if (!wasEnabled && shouldBeEnabled) {
            // Plugin was disabled but should be enabled
            try {
                plugin.start();
                logger.info("Started plugin: {} ({})", plugin.getName(), pluginId);
            } catch (Exception e) {
                logger.error("Failed to start plugin {}: {}", pluginId, e.getMessage(), e);
            }
        }
        
        return true;
    }
    
    /**
     * Saves a plugin configuration to disk.
     * 
     * @param config The configuration to save
     */
    private void saveConfiguration(PluginConfiguration config) {
        String pluginId = config.getPluginId();
        Path configFile = Paths.get(configDir, pluginId + ".json");
        
        try {
            objectMapper.writeValue(configFile.toFile(), config);
            logger.info("Saved plugin configuration to {}", configFile);
        } catch (IOException e) {
            logger.error("Failed to save plugin configuration to {}: {}", configFile, e.getMessage(), e);
        }
    }
    
    /**
     * Deletes the configuration for a plugin.
     * 
     * @param pluginId The ID of the plugin
     * @return true if the configuration was deleted, false if it was not found
     */
    public boolean deleteConfiguration(String pluginId) {
        PluginConfiguration config = configurations.remove(pluginId);
        
        if (config == null) {
            logger.warn("Configuration for plugin {} not found", pluginId);
            return false;
        }
        
        // Delete the configuration file
        Path configFile = Paths.get(configDir, pluginId + ".json");
        try {
            Files.deleteIfExists(configFile);
            logger.info("Deleted plugin configuration file {}", configFile);
        } catch (IOException e) {
            logger.error("Failed to delete plugin configuration file {}: {}", configFile, e.getMessage(), e);
        }
        
        return true;
    }
    
    /**
     * Enables a plugin.
     * 
     * @param pluginId The ID of the plugin
     * @return true if the plugin was enabled, false if it was not found or already enabled
     */
    public boolean enablePlugin(String pluginId) {
        Plugin plugin = pluginRegistry.getPlugin(pluginId);
        
        if (plugin == null) {
            logger.warn("Cannot enable unknown plugin: {}", pluginId);
            return false;
        }
        
        PluginConfiguration config = getConfiguration(pluginId);
        
        if (config.isEnabled()) {
            logger.info("Plugin {} is already enabled", pluginId);
            return false;
        }
        
        // Update the configuration
        config.setEnabled(true);
        saveConfiguration(config);
        
        // Start the plugin
        try {
            plugin.start();
            logger.info("Started plugin: {} ({})", plugin.getName(), pluginId);
        } catch (Exception e) {
            logger.error("Failed to start plugin {}: {}", pluginId, e.getMessage(), e);
            return false;
        }
        
        return true;
    }
    
    /**
     * Disables a plugin.
     * 
     * @param pluginId The ID of the plugin
     * @return true if the plugin was disabled, false if it was not found or already disabled
     */
    public boolean disablePlugin(String pluginId) {
        Plugin plugin = pluginRegistry.getPlugin(pluginId);
        
        if (plugin == null) {
            logger.warn("Cannot disable unknown plugin: {}", pluginId);
            return false;
        }
        
        PluginConfiguration config = getConfiguration(pluginId);
        
        if (!config.isEnabled()) {
            logger.info("Plugin {} is already disabled", pluginId);
            return false;
        }
        
        // Update the configuration
        config.setEnabled(false);
        saveConfiguration(config);
        
        // Stop the plugin
        try {
            plugin.stop();
            logger.info("Stopped plugin: {} ({})", plugin.getName(), pluginId);
        } catch (Exception e) {
            logger.error("Failed to stop plugin {}: {}", pluginId, e.getMessage(), e);
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns all plugins of a specific type.
     * 
     * @param <T> The plugin type
     * @param pluginType The class object representing the plugin type
     * @return A list of plugins of the specified type
     */
    public <T extends Plugin> List<T> getPlugins(Class<T> pluginType) {
        return pluginRegistry.getPlugins(pluginType);
    }
    
    /**
     * Returns all enabled plugins of a specific type.
     * 
     * @param <T> The plugin type
     * @param pluginType The class object representing the plugin type
     * @return A list of enabled plugins of the specified type
     */
    public <T extends Plugin> List<T> getEnabledPlugins(Class<T> pluginType) {
        return pluginRegistry.getPlugins(pluginType).stream()
                .filter(plugin -> getConfiguration(plugin.getId()).isEnabled())
                .collect(Collectors.toList());
    }
    
    /**
     * Returns a plugin by its ID.
     * 
     * @param pluginId The ID of the plugin to retrieve
     * @return The plugin, or null if not found
     */
    public Plugin getPlugin(String pluginId) {
        return pluginRegistry.getPlugin(pluginId);
    }
    
    /**
     * Returns all registered plugins.
     * 
     * @return A list of all plugins
     */
    public List<Plugin> getAllPlugins() {
        return pluginRegistry.getAllPlugins();
    }
    
    /**
     * Returns all enabled plugins.
     * 
     * @return A list of all enabled plugins
     */
    public List<Plugin> getAllEnabledPlugins() {
        return pluginRegistry.getAllPlugins().stream()
                .filter(plugin -> getConfiguration(plugin.getId()).isEnabled())
                .collect(Collectors.toList());
    }
    
    /**
     * Returns a map of plugin IDs to their enabled state.
     * 
     * @return A map of plugin IDs to their enabled state
     */
    public Map<String, Boolean> getPluginEnabledStates() {
        Map<String, Boolean> states = new HashMap<>();
        
        for (Plugin plugin : pluginRegistry.getAllPlugins()) {
            String pluginId = plugin.getId();
            boolean enabled = getConfiguration(pluginId).isEnabled();
            states.put(pluginId, enabled);
        }
        
        return states;
    }
}