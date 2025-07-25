package io.github.ozkanpakdil.grepwise.plugin;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the AbstractPlugin class.
 */
public class AbstractPluginTest {

    /**
     * Test implementation of AbstractPlugin for testing purposes.
     */
    private static class TestPlugin extends AbstractPlugin {
        private boolean doInitializeCalled = false;
        private boolean doStartCalled = false;
        private boolean doStopCalled = false;
        
        public TestPlugin(String id, String name, String description, String version) {
            super(id, name, description, version);
        }
        
        @Override
        protected void doInitialize() throws Exception {
            doInitializeCalled = true;
        }
        
        @Override
        protected void doStart() throws Exception {
            doStartCalled = true;
        }
        
        @Override
        protected void doStop() throws Exception {
            doStopCalled = true;
        }
        
        public boolean isDoInitializeCalled() {
            return doInitializeCalled;
        }
        
        public boolean isDoStartCalled() {
            return doStartCalled;
        }
        
        public boolean isDoStopCalled() {
            return doStopCalled;
        }
    }
    
    @Test
    public void testConstructor() {
        // Arrange
        String id = "test-plugin";
        String name = "Test Plugin";
        String description = "A plugin for testing";
        String version = "1.0.0";
        
        // Act
        TestPlugin plugin = new TestPlugin(id, name, description, version);
        
        // Assert
        assertEquals(id, plugin.getId());
        assertEquals(name, plugin.getName());
        assertEquals(description, plugin.getDescription());
        assertEquals(version, plugin.getVersion());
        assertEquals(AbstractPlugin.PluginState.UNINITIALIZED, plugin.getState());
    }
    
    @Test
    public void testLifecycle() throws Exception {
        // Arrange
        TestPlugin plugin = new TestPlugin("test", "Test", "Test plugin", "1.0.0");
        
        // Act & Assert - Initialize
        plugin.initialize();
        assertTrue(plugin.isDoInitializeCalled());
        assertEquals(AbstractPlugin.PluginState.INITIALIZED, plugin.getState());
        
        // Act & Assert - Start
        plugin.start();
        assertTrue(plugin.isDoStartCalled());
        assertEquals(AbstractPlugin.PluginState.STARTED, plugin.getState());
        
        // Act & Assert - Stop
        plugin.stop();
        assertTrue(plugin.isDoStopCalled());
        assertEquals(AbstractPlugin.PluginState.STOPPED, plugin.getState());
    }
    
    @Test
    public void testStateTransitions() throws Exception {
        // Arrange
        TestPlugin plugin = new TestPlugin("test", "Test", "Test plugin", "1.0.0");
        
        // Initialize the plugin
        plugin.initialize();
        assertEquals(AbstractPlugin.PluginState.INITIALIZED, plugin.getState());
        
        // Try to initialize again - should be a no-op
        plugin.initialize();
        assertEquals(AbstractPlugin.PluginState.INITIALIZED, plugin.getState());
        
        // Start the plugin
        plugin.start();
        assertEquals(AbstractPlugin.PluginState.STARTED, plugin.getState());
        
        // Try to start again - should be a no-op
        plugin.start();
        assertEquals(AbstractPlugin.PluginState.STARTED, plugin.getState());
        
        // Stop the plugin
        plugin.stop();
        assertEquals(AbstractPlugin.PluginState.STOPPED, plugin.getState());
        
        // Try to stop again - should be a no-op
        plugin.stop();
        assertEquals(AbstractPlugin.PluginState.STOPPED, plugin.getState());
        
        // Can restart after stopping
        plugin.start();
        assertEquals(AbstractPlugin.PluginState.STARTED, plugin.getState());
    }
    
    @Test
    public void testToString() {
        // Arrange
        String id = "test-plugin";
        String name = "Test Plugin";
        String description = "A plugin for testing";
        String version = "1.0.0";
        TestPlugin plugin = new TestPlugin(id, name, description, version);
        
        // Act
        String result = plugin.toString();
        
        // Assert
        assertTrue(result.contains(id) || result.contains(name) || result.contains(version),
                "toString() should contain plugin information");
    }
}