package io.github.ozkanpakdil.grepwise.marketplace.model;

/**
 * Enum representing the different types of app packages in the marketplace.
 */
public enum AppPackageType {
    /**
     * A plugin that extends the core functionality of the application.
     */
    PLUGIN,
    
    /**
     * A visualization that can be used in dashboards.
     */
    VISUALIZATION,
    
    /**
     * A dashboard template that can be imported.
     */
    DASHBOARD_TEMPLATE,
    
    /**
     * A data source integration for collecting logs.
     */
    DATA_SOURCE,
    
    /**
     * A theme for customizing the application's appearance.
     */
    THEME,
    
    /**
     * A set of predefined alerts and dashboards for specific use cases.
     */
    SOLUTION_PACK,
    
    /**
     * A utility tool that provides additional functionality.
     */
    UTILITY
}