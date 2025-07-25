package io.github.ozkanpakdil.grepwise.plugin;

import io.github.ozkanpakdil.grepwise.model.LogEntry;

import java.util.List;

/**
 * Interface for plugins that provide log data from external sources.
 * Implementations of this interface can collect logs from various sources
 * such as cloud services, databases, or custom applications.
 */
public interface LogSourcePlugin extends Plugin {
    
    /**
     * Returns the type of log source this plugin supports.
     * 
     * @return The log source type (e.g., "aws", "azure", "database", etc.)
     */
    String getSourceType();
    
    /**
     * Collects log entries from the source.
     * 
     * @param maxEntries The maximum number of entries to collect
     * @param fromTimestamp The timestamp to collect logs from (milliseconds since epoch)
     * @return A list of collected log entries
     * @throws Exception if log collection fails
     */
    List<LogEntry> collectLogs(int maxEntries, long fromTimestamp) throws Exception;
    
    /**
     * Tests the connection to the log source.
     * 
     * @return true if the connection is successful, false otherwise
     */
    boolean testConnection();
    
    /**
     * Returns the configuration schema for this log source plugin.
     * The schema defines the configuration parameters required by this plugin.
     * 
     * @return A JSON schema string describing the configuration parameters
     */
    String getConfigurationSchema();
}