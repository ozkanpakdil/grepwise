# GrepWise Plugin Development Guide

This guide provides information on how to develop plugins for the GrepWise platform. Plugins allow you to extend GrepWise's functionality with custom log sources, integrations, and visualizations.

## Table of Contents

1. [Plugin System Overview](#plugin-system-overview)
2. [Plugin Types](#plugin-types)
3. [Creating a Plugin](#creating-a-plugin)
4. [Plugin Lifecycle](#plugin-lifecycle)
5. [Plugin Configuration](#plugin-configuration)
6. [Packaging and Deployment](#packaging-and-deployment)
7. [Testing Plugins](#testing-plugins)
8. [Best Practices](#best-practices)

## Plugin System Overview

The GrepWise plugin system allows developers to extend the platform's functionality without modifying the core codebase. Plugins are loaded dynamically at runtime and can be enabled or disabled as needed.

Key components of the plugin system:

- **Plugin Interface**: The base interface that all plugins must implement
- **Plugin Registry**: Stores and manages registered plugins
- **Plugin Manager**: Handles plugin lifecycle and discovery

## Plugin Types

GrepWise supports several types of plugins:

### 1. Log Source Plugins

Log source plugins collect logs from external sources such as cloud services, databases, or custom applications. They implement the `LogSourcePlugin` interface.

Example use cases:
- Collecting logs from AWS CloudWatch
- Retrieving logs from a database
- Monitoring custom application logs

### 2. Integration Plugins

Integration plugins connect GrepWise with external systems such as alerting platforms, ticketing systems, or other monitoring tools. They implement the `IntegrationPlugin` interface.

Example use cases:
- Sending alerts to PagerDuty or OpsGenie
- Creating tickets in JIRA
- Integrating with monitoring dashboards

### 3. Visualization Plugins

Visualization plugins add new ways to visualize log data in dashboards. They implement the `VisualizationPlugin` interface.

Example use cases:
- Custom chart types
- Specialized data visualizations
- Interactive data explorers

## Creating a Plugin

### Step 1: Set Up Your Development Environment

1. Clone the GrepWise repository
2. Set up your development environment with Java 17 and Maven
3. Import the project into your IDE

### Step 2: Choose a Plugin Type

Decide which type of plugin you want to create based on your requirements.

### Step 3: Create a Plugin Class

Create a new class that extends `AbstractPlugin` and implements the appropriate plugin interface.

Example for a Log Source Plugin:

```java
package com.example.grepwise.plugin;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.plugin.AbstractPlugin;
import io.github.ozkanpakdil.grepwise.plugin.LogSourcePlugin;

import java.util.List;
import java.util.Map;

public class MyCustomLogSourcePlugin extends AbstractPlugin implements LogSourcePlugin {

    public MyCustomLogSourcePlugin() {
        super(
            "my-custom-log-source",
            "My Custom Log Source",
            "1.0.0",
            "A custom log source plugin that collects logs from my application"
        );
    }

    @Override
    public String getSourceType() {
        return "my-custom-source";
    }

    @Override
    public List<LogEntry> collectLogs(int maxEntries, long fromTimestamp) throws Exception {
        // Implement log collection logic
        // ...
        return collectedLogs;
    }

    @Override
    public boolean testConnection() {
        // Implement connection testing logic
        // ...
        return connectionSuccessful;
    }

    @Override
    public String getConfigurationSchema() {
        // Return a JSON schema describing the configuration parameters
        return "{"
            + "\"type\": \"object\","
            + "\"properties\": {"
            + "  \"apiKey\": {"
            + "    \"type\": \"string\","
            + "    \"description\": \"API key for authentication\""
            + "  },"
            + "  \"endpoint\": {"
            + "    \"type\": \"string\","
            + "    \"description\": \"API endpoint URL\""
            + "  }"
            + "},"
            + "\"required\": [\"apiKey\", \"endpoint\"]"
            + "}";
    }

    @Override
    protected void doInitialize() throws Exception {
        // Initialize the plugin
        // ...
    }

    @Override
    protected void doStart() throws Exception {
        // Start the plugin
        // ...
    }

    @Override
    protected void doStop() throws Exception {
        // Stop the plugin
        // ...
    }
}
```

## Plugin Lifecycle

Plugins in GrepWise have a defined lifecycle:

1. **Loading**: The plugin is loaded from a JAR file or the classpath
2. **Registration**: The plugin is registered with the plugin registry
3. **Initialization**: The plugin's `initialize()` method is called
4. **Starting**: The plugin's `start()` method is called when the plugin is enabled
5. **Stopping**: The plugin's `stop()` method is called when the plugin is disabled
6. **Unregistration**: The plugin is unregistered from the plugin registry

The `AbstractPlugin` class handles most of the lifecycle management, so you only need to implement the `doInitialize()`, `doStart()`, and `doStop()` methods.

## Plugin Configuration

Plugins can define a configuration schema using JSON Schema. This schema is used to generate a configuration UI in the GrepWise web interface.

The configuration is passed to the plugin when it's initialized and can be updated at runtime.

To access the configuration in your plugin:

```java
@Override
protected void doInitialize() throws Exception {
    // Get the configuration from the plugin manager
    Map<String, Object> config = pluginManagerService.getPluginConfiguration(getId()).orElse(new HashMap<>());
    
    // Access configuration parameters
    String apiKey = (String) config.get("apiKey");
    String endpoint = (String) config.get("endpoint");
    
    // Use the configuration
    // ...
}
```

## Packaging and Deployment

### Step 1: Package Your Plugin

Package your plugin as a JAR file using Maven or Gradle.

Example Maven POM:

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-grepwise-plugin</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <dependency>
            <groupId>io.github.ozkanpakdil</groupId>
            <artifactId>grepwise</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
        <!-- Other dependencies -->
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <Plugin-Id>my-custom-log-source</Plugin-Id>
                            <Plugin-Version>1.0.0</Plugin-Version>
                            <Plugin-Provider>Example Inc.</Plugin-Provider>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 2: Deploy Your Plugin

1. Copy the JAR file to the `plugins` directory in your GrepWise installation
2. Restart GrepWise or use the plugin manager to reload plugins

## Testing Plugins

### Unit Testing

Create unit tests for your plugin using JUnit and Mockito.

Example:

```java
@ExtendWith(MockitoExtension.class)
public class MyCustomLogSourcePluginTest {

    @Mock
    private PluginManagerService pluginManagerService;
    
    private MyCustomLogSourcePlugin plugin;
    
    @BeforeEach
    void setUp() {
        plugin = new MyCustomLogSourcePlugin();
        
        // Mock configuration
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", "test-api-key");
        config.put("endpoint", "https://api.example.com");
        
        when(pluginManagerService.getPluginConfiguration(plugin.getId()))
            .thenReturn(Optional.of(config));
    }
    
    @Test
    void testCollectLogs() throws Exception {
        // Initialize and start the plugin
        plugin.initialize();
        plugin.start();
        
        // Test collecting logs
        List<LogEntry> logs = plugin.collectLogs(10, System.currentTimeMillis() - 3600000);
        
        // Verify results
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        // Additional assertions...
        
        // Stop the plugin
        plugin.stop();
    }
}
```

### Integration Testing

Test your plugin with a running GrepWise instance:

1. Deploy your plugin to a test instance of GrepWise
2. Enable the plugin through the web interface
3. Verify that the plugin works as expected

## Best Practices

1. **Error Handling**: Implement robust error handling in your plugin
2. **Logging**: Use SLF4J for logging and include appropriate log levels
3. **Resource Management**: Properly manage resources and clean up in the `doStop()` method
4. **Configuration Validation**: Validate configuration parameters before using them
5. **Performance**: Optimize your plugin for performance, especially for log source plugins
6. **Security**: Handle sensitive information securely and follow security best practices
7. **Documentation**: Document your plugin thoroughly, including configuration parameters
8. **Versioning**: Use semantic versioning for your plugin

## Example Plugins

For reference, check out these example plugins:

- [SampleLogSourcePlugin](src/main/java/io/github/ozkanpakdil/grepwise/plugin/sample/SampleLogSourcePlugin.java): A sample log source plugin that generates random log entries

## Support and Community

If you have questions or need help developing plugins for GrepWise, please:

- Open an issue on GitHub
- Join our community forum
- Contact the GrepWise team

Happy plugin development!