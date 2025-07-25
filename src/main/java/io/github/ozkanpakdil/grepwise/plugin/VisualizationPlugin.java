package io.github.ozkanpakdil.grepwise.plugin;

import java.util.List;
import java.util.Map;

/**
 * Interface for plugins that provide custom visualizations for dashboards.
 * Implementations of this interface can add new visualization types
 * to the dashboard system.
 */
public interface VisualizationPlugin extends Plugin {
    
    /**
     * Returns the type of visualization this plugin provides.
     * 
     * @return The visualization type (e.g., "chart", "gauge", "map", etc.)
     */
    String getVisualizationType();
    
    /**
     * Returns the display name of this visualization type.
     * 
     * @return The display name to show in the UI
     */
    String getDisplayName();
    
    /**
     * Returns a description of this visualization type.
     * 
     * @return The description to show in the UI
     */
    String getVisualizationDescription();
    
    /**
     * Returns the frontend component name that renders this visualization.
     * 
     * @return The name of the frontend component
     */
    String getFrontendComponent();
    
    /**
     * Returns the configuration schema for this visualization.
     * The schema defines the configuration parameters required by this visualization.
     * 
     * @return A JSON schema string describing the configuration parameters
     */
    String getConfigurationSchema();
    
    /**
     * Transforms the input data for visualization.
     * This method processes the raw data and returns it in a format suitable
     * for the frontend visualization component.
     * 
     * @param data The raw data to transform
     * @param config The visualization configuration
     * @return The transformed data ready for visualization
     * @throws Exception if the transformation fails
     */
    Map<String, Object> transformData(List<Map<String, Object>> data, Map<String, Object> config) throws Exception;
    
    /**
     * Validates the configuration for this visualization.
     * 
     * @param config The configuration to validate
     * @return true if the configuration is valid, false otherwise
     */
    boolean validateConfiguration(Map<String, Object> config);
    
    /**
     * Returns a list of supported data formats for this visualization.
     * 
     * @return A list of supported data format identifiers
     */
    List<String> getSupportedDataFormats();
}