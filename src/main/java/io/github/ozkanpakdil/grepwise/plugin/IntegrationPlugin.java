package io.github.ozkanpakdil.grepwise.plugin;

import java.util.Map;

/**
 * Interface for plugins that integrate with external systems.
 * Implementations of this interface can provide integration with various
 * external services such as alerting systems, ticketing systems, or other
 * monitoring tools.
 */
public interface IntegrationPlugin extends Plugin {
    
    /**
     * Returns the type of integration this plugin supports.
     * 
     * @return The integration type (e.g., "alerting", "ticketing", "monitoring", etc.)
     */
    String getIntegrationType();
    
    /**
     * Sends data to the external system.
     * 
     * @param data The data to send, as a map of key-value pairs
     * @return A response from the external system, as a map of key-value pairs
     * @throws Exception if the operation fails
     */
    Map<String, Object> sendData(Map<String, Object> data) throws Exception;
    
    /**
     * Receives data from the external system.
     * 
     * @param query Parameters to use when querying the external system
     * @return Data received from the external system, as a map of key-value pairs
     * @throws Exception if the operation fails
     */
    Map<String, Object> receiveData(Map<String, Object> query) throws Exception;
    
    /**
     * Tests the connection to the external system.
     * 
     * @return true if the connection is successful, false otherwise
     */
    boolean testConnection();
    
    /**
     * Returns the configuration schema for this integration plugin.
     * The schema defines the configuration parameters required by this plugin.
     * 
     * @return A JSON schema string describing the configuration parameters
     */
    String getConfigurationSchema();
    
    /**
     * Returns the capabilities of this integration plugin.
     * Capabilities indicate what operations this plugin supports.
     * 
     * @return A map of capability names to boolean values indicating support
     */
    Map<String, Boolean> getCapabilities();
}