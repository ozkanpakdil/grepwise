package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.plugin.Plugin;
import io.github.ozkanpakdil.grepwise.plugin.PluginConfiguration;
import io.github.ozkanpakdil.grepwise.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for plugin management.
 * This controller provides endpoints for listing, enabling, disabling, and configuring plugins.
 */
@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private static final Logger logger = LoggerFactory.getLogger(PluginController.class);

    @Autowired
    private PluginManager pluginManager;

    /**
     * Returns a list of all plugins.
     *
     * @return A list of plugin information
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllPlugins() {
        logger.debug("Getting all plugins");

        List<Map<String, Object>> pluginInfoList = pluginManager.getAllPlugins().stream()
                .map(this::createPluginInfo)
                .collect(Collectors.toList());

        return ResponseEntity.ok(pluginInfoList);
    }

    /**
     * Returns information about a specific plugin.
     *
     * @param pluginId The ID of the plugin
     * @return The plugin information
     */
    @GetMapping("/{pluginId}")
    public ResponseEntity<Map<String, Object>> getPlugin(@PathVariable String pluginId) {
        logger.debug("Getting plugin: {}", pluginId);

        Plugin plugin = pluginManager.getPlugin(pluginId);

        if (plugin == null) {
            logger.warn("Plugin not found: {}", pluginId);
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> pluginInfo = createPluginInfo(plugin);
        return ResponseEntity.ok(pluginInfo);
    }

    /**
     * Returns the configuration for a specific plugin.
     *
     * @param pluginId The ID of the plugin
     * @return The plugin configuration
     */
    @GetMapping("/{pluginId}/config")
    public ResponseEntity<PluginConfiguration> getPluginConfiguration(@PathVariable String pluginId) {
        logger.debug("Getting configuration for plugin: {}", pluginId);

        Plugin plugin = pluginManager.getPlugin(pluginId);

        if (plugin == null) {
            logger.warn("Plugin not found: {}", pluginId);
            return ResponseEntity.notFound().build();
        }

        PluginConfiguration config = pluginManager.getConfiguration(pluginId);
        return ResponseEntity.ok(config);
    }

    /**
     * Updates the configuration for a specific plugin.
     *
     * @param pluginId The ID of the plugin
     * @param config   The updated configuration
     * @return The updated plugin configuration
     */
    @PutMapping("/{pluginId}/config")
    public ResponseEntity<PluginConfiguration> updatePluginConfiguration(
            @PathVariable String pluginId,
            @RequestBody PluginConfiguration config) {

        logger.debug("Updating configuration for plugin: {}", pluginId);

        // Ensure the plugin ID in the path matches the one in the configuration
        if (!pluginId.equals(config.getPluginId())) {
            logger.warn("Plugin ID mismatch: {} vs {}", pluginId, config.getPluginId());
            return ResponseEntity.badRequest().build();
        }

        Plugin plugin = pluginManager.getPlugin(pluginId);

        if (plugin == null) {
            logger.warn("Plugin not found: {}", pluginId);
            return ResponseEntity.notFound().build();
        }

        boolean updated = pluginManager.updateConfiguration(config);

        if (!updated) {
            logger.warn("Failed to update configuration for plugin: {}", pluginId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(config);
    }

    /**
     * Enables a plugin.
     *
     * @param pluginId The ID of the plugin
     * @return A success message
     */
    @PostMapping("/{pluginId}/enable")
    public ResponseEntity<Map<String, Object>> enablePlugin(@PathVariable String pluginId) {
        logger.debug("Enabling plugin: {}", pluginId);

        Plugin plugin = pluginManager.getPlugin(pluginId);

        if (plugin == null) {
            logger.warn("Plugin not found: {}", pluginId);
            return ResponseEntity.notFound().build();
        }

        boolean enabled = pluginManager.enablePlugin(pluginId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", enabled);
        response.put("message", enabled ? "Plugin enabled" : "Plugin already enabled");

        return ResponseEntity.ok(response);
    }

    /**
     * Disables a plugin.
     *
     * @param pluginId The ID of the plugin
     * @return A success message
     */
    @PostMapping("/{pluginId}/disable")
    public ResponseEntity<Map<String, Object>> disablePlugin(@PathVariable String pluginId) {
        logger.debug("Disabling plugin: {}", pluginId);

        Plugin plugin = pluginManager.getPlugin(pluginId);

        if (plugin == null) {
            logger.warn("Plugin not found: {}", pluginId);
            return ResponseEntity.notFound().build();
        }

        boolean disabled = pluginManager.disablePlugin(pluginId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", disabled);
        response.put("message", disabled ? "Plugin disabled" : "Plugin already disabled");

        return ResponseEntity.ok(response);
    }

    /**
     * Creates a map of plugin information.
     *
     * @param plugin The plugin
     * @return A map of plugin information
     */
    private Map<String, Object> createPluginInfo(Plugin plugin) {
        Map<String, Object> info = new HashMap<>();

        String pluginId = plugin.getId();
        PluginConfiguration config = pluginManager.getConfiguration(pluginId);

        info.put("id", pluginId);
        info.put("name", plugin.getName());
        info.put("description", plugin.getDescription());
        info.put("version", plugin.getVersion());
        info.put("enabled", config.isEnabled());

        // Add plugin type information
        List<String> types = new ArrayList<>();
        for (Class<?> iface : plugin.getClass().getInterfaces()) {
            if (Plugin.class.isAssignableFrom(iface) && iface != Plugin.class) {
                types.add(iface.getSimpleName());
            }
        }

        info.put("types", types);

        return info;
    }
}