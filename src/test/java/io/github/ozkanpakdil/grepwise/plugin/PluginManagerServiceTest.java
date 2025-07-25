package io.github.ozkanpakdil.grepwise.plugin;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.plugin.sample.SampleLogSourcePlugin;
import io.github.ozkanpakdil.grepwise.service.PluginManagerService;
import io.github.ozkanpakdil.grepwise.service.impl.PluginManagerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class PluginManagerServiceTest {

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private PluginManagerService pluginManagerService;

    @Mock
    private ApplicationContext applicationContext;

    private SampleLogSourcePlugin samplePlugin;

    @BeforeEach
    void setUp() {
        // Clear the registry before each test
        pluginRegistry.clear();

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