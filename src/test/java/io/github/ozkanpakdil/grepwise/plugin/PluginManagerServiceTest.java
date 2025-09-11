package io.github.ozkanpakdil.grepwise.plugin;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.plugin.sample.SampleLogSourcePlugin;
import io.github.ozkanpakdil.grepwise.service.impl.PluginManagerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PluginManagerServiceTest {

    private PluginRegistry pluginRegistry;
    private PluginManagerServiceImpl pluginManagerService;

    private SampleLogSourcePlugin samplePlugin;
    private Path tempPluginsDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create a fresh registry and service for each test (no Spring context)
        pluginRegistry = new PluginRegistry();
        pluginManagerService = new PluginManagerServiceImpl();

        // Avoid scanning classpath or filesystem during tests
        tempPluginsDir = Files.createTempDirectory("plugins-test");
        ReflectionTestUtils.setField(pluginManagerService, "pluginRegistry", pluginRegistry);
        ReflectionTestUtils.setField(pluginManagerService, "scanClasspath", false);
        ReflectionTestUtils.setField(pluginManagerService, "pluginsDirectory", tempPluginsDir.toString());

        // Initialize the service (will scan empty temp dir, classpath scanning disabled)
        pluginManagerService.initialize();

        // Create a sample plugin for testing
        samplePlugin = new SampleLogSourcePlugin();
    }

    @Test
    void testRegisterPlugin() {
        // Register the plugin
        boolean result = pluginManagerService.registerPlugin(samplePlugin);

        // Verify registration was successful
        assertTrue(result);
        assertEquals(1, pluginManagerService.getAllPlugins().size());
        assertEquals(samplePlugin, pluginManagerService.getPluginById(samplePlugin.getId()).orElse(null));
    }

    @Test
    void testUnregisterPlugin() {
        // Register the plugin
        pluginManagerService.registerPlugin(samplePlugin);

        // Unregister the plugin
        boolean result = pluginManagerService.unregisterPlugin(samplePlugin.getId());

        // Verify unregistration was successful
        assertTrue(result);
        assertEquals(0, pluginManagerService.getAllPlugins().size());
        assertFalse(pluginManagerService.getPluginById(samplePlugin.getId()).isPresent());
    }

    @Test
    void testEnableDisablePlugin() throws Exception {
        // Register the plugin
        pluginManagerService.registerPlugin(samplePlugin);

        // Enable the plugin
        boolean enableResult = pluginManagerService.enablePlugin(samplePlugin.getId());

        // Verify plugin was enabled
        assertTrue(enableResult);
        assertTrue(pluginRegistry.isPluginEnabled(samplePlugin.getId()));

        // Disable the plugin
        boolean disableResult = pluginManagerService.disablePlugin(samplePlugin.getId());

        // Verify plugin was disabled
        assertTrue(disableResult);
        assertFalse(pluginRegistry.isPluginEnabled(samplePlugin.getId()));
    }

    @Test
    void testGetPluginsByType() {
        // Register the plugin
        pluginManagerService.registerPlugin(samplePlugin);

        // Get plugins by type
        List<LogSourcePlugin> logSourcePlugins = pluginManagerService.getPluginsByType(LogSourcePlugin.class);
        List<IntegrationPlugin> integrationPlugins = pluginManagerService.getPluginsByType(IntegrationPlugin.class);

        // Verify correct plugins were returned
        assertEquals(1, logSourcePlugins.size());
        assertEquals(samplePlugin, logSourcePlugins.get(0));
        assertEquals(0, integrationPlugins.size());
    }

    @Test
    void testPluginConfiguration() {
        // Register the plugin
        pluginManagerService.registerPlugin(samplePlugin);

        // Create a configuration
        Map<String, Object> config = new HashMap<>();
        config.put("maxEntriesPerRequest", 50);
        config.put("generateErrorsFrequency", 0.2);

        // Update the configuration
        boolean updateResult = pluginManagerService.updatePluginConfiguration(samplePlugin.getId(), config);

        // Verify configuration was updated
        assertTrue(updateResult);
        Optional<Map<String, Object>> retrievedConfig = pluginManagerService.getPluginConfiguration(samplePlugin.getId());
        assertTrue(retrievedConfig.isPresent());
        assertEquals(config.get("maxEntriesPerRequest"), retrievedConfig.get().get("maxEntriesPerRequest"));
        assertEquals(config.get("generateErrorsFrequency"), retrievedConfig.get().get("generateErrorsFrequency"));
    }

    @Test
    void testSampleLogSourcePlugin() throws Exception {
        // Register and enable the plugin
        pluginManagerService.registerPlugin(samplePlugin);
        pluginManagerService.enablePlugin(samplePlugin.getId());

        // Test the plugin functionality
        List<LogEntry> logs = samplePlugin.collectLogs(10, System.currentTimeMillis() - 3600000);

        // Verify logs were collected
        assertNotNull(logs);
        assertEquals(10, logs.size());
        
        // Verify log entries have the expected source
        for (LogEntry log : logs) {
            assertEquals("sample-log-source", log.source());
            assertNotNull(log.id());
            assertNotNull(log.message());
            assertNotNull(log.level());
            assertNotNull(log.timestamp());
            assertNotNull(log.rawContent());
            assertNotNull(log.metadata());
        }
    }
}