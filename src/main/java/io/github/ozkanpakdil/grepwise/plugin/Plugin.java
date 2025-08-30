package io.github.ozkanpakdil.grepwise.plugin;

/**
 * Interface that all plugins must implement.
 * Defines the basic lifecycle methods for a plugin.
 */
public interface Plugin {

    /**
     * Returns the unique identifier for this plugin.
     *
     * @return The plugin ID
     */
    String getId();

    /**
     * Returns the name of this plugin.
     *
     * @return The plugin name
     */
    String getName();

    /**
     * Returns the version of this plugin.
     *
     * @return The plugin version
     */
    String getVersion();

    /**
     * Returns a description of this plugin.
     *
     * @return The plugin description
     */
    String getDescription();

    /**
     * Initializes the plugin. This method is called once when the plugin is loaded.
     *
     * @throws Exception if initialization fails
     */
    void initialize() throws Exception;

    /**
     * Starts the plugin. This method is called after initialization and when the plugin is enabled.
     *
     * @throws Exception if startup fails
     */
    void start() throws Exception;

    /**
     * Stops the plugin. This method is called when the plugin is disabled or the application is shutting down.
     *
     * @throws Exception if shutdown fails
     */
    void stop() throws Exception;
}