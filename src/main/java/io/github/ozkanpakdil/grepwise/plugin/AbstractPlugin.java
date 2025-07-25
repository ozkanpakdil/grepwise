package io.github.ozkanpakdil.grepwise.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for plugins that implements common functionality.
 * Plugin developers can extend this class to create their own plugins.
 */
public abstract class AbstractPlugin implements Plugin {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    
    private PluginState state = PluginState.UNINITIALIZED;
    
    /**
     * Constructs a new AbstractPlugin with the specified metadata.
     * 
     * @param id The unique identifier for this plugin
     * @param name The name of this plugin
     * @param version The version of this plugin
     * @param description A description of this plugin
     */
    protected AbstractPlugin(String id, String name, String version, String description) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public void initialize() throws Exception {
        if (state != PluginState.UNINITIALIZED) {
            logger.warn("Plugin {} is already initialized", id);
            return;
        }
        
        logger.info("Initializing plugin: {}", id);
        try {
            doInitialize();
            state = PluginState.INITIALIZED;
            logger.info("Plugin {} initialized successfully", id);
        } catch (Exception e) {
            logger.error("Failed to initialize plugin {}: {}", id, e.getMessage(), e);
            state = PluginState.FAILED;
            throw e;
        }
    }
    
    @Override
    public void start() throws Exception {
        if (state != PluginState.INITIALIZED && state != PluginState.STOPPED) {
            logger.warn("Plugin {} cannot be started because it is in state {}", id, state);
            return;
        }
        
        logger.info("Starting plugin: {}", id);
        try {
            doStart();
            state = PluginState.STARTED;
            logger.info("Plugin {} started successfully", id);
        } catch (Exception e) {
            logger.error("Failed to start plugin {}: {}", id, e.getMessage(), e);
            state = PluginState.FAILED;
            throw e;
        }
    }
    
    @Override
    public void stop() throws Exception {
        if (state != PluginState.STARTED) {
            logger.warn("Plugin {} cannot be stopped because it is in state {}", id, state);
            return;
        }
        
        logger.info("Stopping plugin: {}", id);
        try {
            doStop();
            state = PluginState.STOPPED;
            logger.info("Plugin {} stopped successfully", id);
        } catch (Exception e) {
            logger.error("Failed to stop plugin {}: {}", id, e.getMessage(), e);
            state = PluginState.FAILED;
            throw e;
        }
    }
    
    /**
     * Returns the current state of this plugin.
     * 
     * @return The plugin state
     */
    public PluginState getState() {
        return state;
    }
    
    /**
     * Initializes the plugin. This method is called by {@link #initialize()}.
     * Subclasses should override this method to perform initialization.
     * 
     * @throws Exception if initialization fails
     */
    protected abstract void doInitialize() throws Exception;
    
    /**
     * Starts the plugin. This method is called by {@link #start()}.
     * Subclasses should override this method to perform startup.
     * 
     * @throws Exception if startup fails
     */
    protected abstract void doStart() throws Exception;
    
    /**
     * Stops the plugin. This method is called by {@link #stop()}.
     * Subclasses should override this method to perform shutdown.
     * 
     * @throws Exception if shutdown fails
     */
    protected abstract void doStop() throws Exception;
    
    /**
     * Enum representing the possible states of a plugin.
     */
    public enum PluginState {
        /** The plugin has not been initialized yet */
        UNINITIALIZED,
        
        /** The plugin has been initialized but not started */
        INITIALIZED,
        
        /** The plugin is running */
        STARTED,
        
        /** The plugin has been stopped */
        STOPPED,
        
        /** The plugin has failed to initialize, start, or stop */
        FAILED
    }
}