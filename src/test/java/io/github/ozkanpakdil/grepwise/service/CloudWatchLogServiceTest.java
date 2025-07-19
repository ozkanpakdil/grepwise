package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CloudWatchLogService.
 * These tests verify the basic functionality without actually connecting to AWS.
 */
public class CloudWatchLogServiceTest {

    @Mock
    private LuceneService luceneService;

    @Mock
    private LogBufferService logBufferService;

    private CloudWatchLogService cloudWatchLogService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        cloudWatchLogService = new CloudWatchLogService(luceneService, logBufferService);
    }

    @Test
    public void testRegisterSource_ValidConfig_ReturnsTrue() {
        // Create a valid CloudWatch log source configuration
        LogSourceConfig config = LogSourceConfig.createCloudWatchSource(
                UUID.randomUUID().toString(),
                "Test CloudWatch Source",
                "us-east-1",
                "test-log-group",
                "test-log-stream",
                null,  // No AWS access key (will use default credentials provider)
                null,  // No AWS secret key (will use default credentials provider)
                60,    // Query refresh interval in seconds
                true   // Enabled
        );

        // Mock the behavior to avoid actual AWS calls
        // This is a simplified test that just verifies the registration logic

        // Call the method under test
        boolean result = cloudWatchLogService.registerSource(config);

        // Verify the result
        assertTrue(result, "Should successfully register a valid CloudWatch source");
        assertEquals(1, cloudWatchLogService.getActiveSourceCount(), "Should have one active source");
    }

    @Test
    public void testRegisterSource_DisabledConfig_ReturnsFalse() {
        // Create a disabled CloudWatch log source configuration
        LogSourceConfig config = LogSourceConfig.createCloudWatchSource(
                UUID.randomUUID().toString(),
                "Disabled CloudWatch Source",
                "us-east-1",
                "test-log-group",
                "test-log-stream",
                null,
                null,
                60,
                false  // Disabled
        );

        // Call the method under test
        boolean result = cloudWatchLogService.registerSource(config);

        // Verify the result
        assertFalse(result, "Should not register a disabled CloudWatch source");
        assertEquals(0, cloudWatchLogService.getActiveSourceCount(), "Should have no active sources");
    }

    @Test
    public void testRegisterSource_MissingLogGroup_ReturnsFalse() {
        // Create a CloudWatch log source configuration with missing log group
        LogSourceConfig config = LogSourceConfig.createCloudWatchSource(
                UUID.randomUUID().toString(),
                "Invalid CloudWatch Source",
                "us-east-1",
                "",  // Empty log group name
                "test-log-stream",
                null,
                null,
                60,
                true
        );

        // Call the method under test
        boolean result = cloudWatchLogService.registerSource(config);

        // Verify the result
        assertFalse(result, "Should not register a CloudWatch source with missing log group");
        assertEquals(0, cloudWatchLogService.getActiveSourceCount(), "Should have no active sources");
    }

    @Test
    public void testRegisterSource_NonCloudWatchConfig_ReturnsFalse() {
        // Create a non-CloudWatch log source configuration
        LogSourceConfig config = LogSourceConfig.createFileSource(
                UUID.randomUUID().toString(),
                "File Source",
                "/var/log",
                "*.log",
                60,
                true
        );

        // Call the method under test
        boolean result = cloudWatchLogService.registerSource(config);

        // Verify the result
        assertFalse(result, "Should not register a non-CloudWatch source");
        assertEquals(0, cloudWatchLogService.getActiveSourceCount(), "Should have no active sources");
    }

    @Test
    public void testUnregisterSource_ExistingSource_ReturnsTrue() {
        // Create and register a CloudWatch log source
        String sourceId = UUID.randomUUID().toString();
        LogSourceConfig config = LogSourceConfig.createCloudWatchSource(
                sourceId,
                "Test CloudWatch Source",
                "us-east-1",
                "test-log-group",
                "test-log-stream",
                null,
                null,
                60,
                true
        );
        cloudWatchLogService.registerSource(config);

        // Call the method under test
        boolean result = cloudWatchLogService.unregisterSource(sourceId);

        // Verify the result
        assertTrue(result, "Should successfully unregister an existing source");
        assertEquals(0, cloudWatchLogService.getActiveSourceCount(), "Should have no active sources after unregistering");
    }

    @Test
    public void testUnregisterSource_NonExistingSource_ReturnsFalse() {
        // Call the method under test with a non-existing source ID
        boolean result = cloudWatchLogService.unregisterSource("non-existing-id");

        // Verify the result
        assertFalse(result, "Should not unregister a non-existing source");
        assertEquals(0, cloudWatchLogService.getActiveSourceCount(), "Should have no active sources");
    }
}