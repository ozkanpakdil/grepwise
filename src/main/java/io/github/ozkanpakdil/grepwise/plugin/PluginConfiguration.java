package io.github.ozkanpakdil.grepwise.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a plugin.
 * This class represents the configuration for a plugin, including its enabled state
 * and any custom configuration properties.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginConfiguration {
    
    private String pluginId;
    private boolean enabled = true;
    private Map<String, Object> properties = new HashMap<>();
    
    @JsonIgnore
    private transient Plugin plugin;
    
    /**
     * Default constructor for serialization.
     */
    public PluginConfiguration() {
    }
    
    /**
     * Creates a new plugin configuration for the specified plugin.
     * 
     * @param plugin The plugin this configuration is for
     */
    public PluginConfiguration(Plugin plugin) {
        this.plugin = plugin;
        this.pluginId = plugin.getId();
    }
    
    /**
     * Returns the ID of the plugin this configuration is for.
     * 
     * @return The plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * Sets the ID of the plugin this configuration is for.
     * 
     * @param pluginId The plugin ID
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
    
    /**
     * Returns whether the plugin is enabled.
     * 
     * @return true if the plugin is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets whether the plugin is enabled.
     * 
     * @param enabled true to enable the plugin, false to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Returns the custom configuration properties for the plugin.
     * 
     * @return A map of property names to values
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * Sets the custom configuration properties for the plugin.
     * 
     * @param properties A map of property names to values
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    /**
     * Returns the plugin this configuration is for.
     * 
     * @return The plugin
     */
    @JsonIgnore
    public Plugin getPlugin() {
        return plugin;
    }
    
    /**
     * Sets the plugin this configuration is for.
     * 
     * @param plugin The plugin
     */
    @JsonIgnore
    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Returns a configuration property as a string.
     * 
     * @param key The property key
     * @return The property value as a string, or null if not found
     */
    public String getString(String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Returns a configuration property as a string, with a default value.
     * 
     * @param key The property key
     * @param defaultValue The default value to return if the property is not found
     * @return The property value as a string, or the default value if not found
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Returns a configuration property as an integer.
     * 
     * @param key The property key
     * @return The property value as an integer, or null if not found or not an integer
     */
    public Integer getInteger(String key) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Returns a configuration property as an integer, with a default value.
     * 
     * @param key The property key
     * @param defaultValue The default value to return if the property is not found or not an integer
     * @return The property value as an integer, or the default value if not found or not an integer
     */
    public int getInteger(String key, int defaultValue) {
        Integer value = getInteger(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Returns a configuration property as a boolean.
     * 
     * @param key The property key
     * @return The property value as a boolean, or null if not found
     */
    public Boolean getBoolean(String key) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    /**
     * Returns a configuration property as a boolean, with a default value.
     * 
     * @param key The property key
     * @param defaultValue The default value to return if the property is not found
     * @return The property value as a boolean, or the default value if not found
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Returns a configuration property as a JsonNode.
     * 
     * @param key The property key
     * @return The property value as a JsonNode, or null if not found or not a JsonNode
     */
    public JsonNode getJsonNode(String key) {
        Object value = properties.get(key);
        if (value instanceof JsonNode) {
            return (JsonNode) value;
        }
        return null;
    }
    
    /**
     * Sets a configuration property.
     * 
     * @param key The property key
     * @param value The property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /**
     * Removes a configuration property.
     * 
     * @param key The property key
     * @return The previous value of the property, or null if it did not exist
     */
    public Object removeProperty(String key) {
        return properties.remove(key);
    }
    
    /**
     * Checks if a configuration property exists.
     * 
     * @param key The property key
     * @return true if the property exists, false otherwise
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * Clears all configuration properties.
     */
    public void clearProperties() {
        properties.clear();
    }
}