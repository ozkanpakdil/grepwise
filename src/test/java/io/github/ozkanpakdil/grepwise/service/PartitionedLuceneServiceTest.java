package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.PartitionConfiguration;
import io.github.ozkanpakdil.grepwise.repository.PartitionConfigurationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartitionedLuceneServiceTest {

    @Mock
    private FieldConfigurationService fieldConfigurationService;

    @Mock
    private ArchiveService archiveService;

    @Mock
    private SearchCacheService searchCacheService;

    @Mock
    private PartitionConfigurationRepository partitionConfigurationRepository;

    @Spy
    @InjectMocks
    private LuceneService luceneService;

    private Path testIndexPath;
    private Path testPartitionBasePath;
    private PartitionConfiguration testConfig;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary directories for testing
        testIndexPath = Files.createTempDirectory("lucene-test-index");
        testPartitionBasePath = Files.createTempDirectory("lucene-test-partitions");

        // Set up test configuration
        testConfig = new PartitionConfiguration();
        testConfig.setId("test-config");
        testConfig.setPartitionType("MONTHLY");
        testConfig.setMaxActivePartitions(3);
        testConfig.setAutoArchivePartitions(true);
        testConfig.setPartitionBaseDirectory(testPartitionBasePath.toString());
        testConfig.setPartitioningEnabled(true);

        // Mock the repository to return our test configuration
        when(partitionConfigurationRepository.getDefaultConfiguration()).thenReturn(testConfig);

        // Set the index directory path using reflection
        ReflectionTestUtils.setField(luceneService, "indexDirPath", testIndexPath.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test directories
        luceneService.close();
        
        // Delete test directories
        if (Files.exists(testIndexPath)) {
            Files.walk(testIndexPath)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
        
        if (Files.exists(testPartitionBasePath)) {
            Files.walk(testPartitionBasePath)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
    }

    @Test
    void testInitWithPartitioningEnabled() throws IOException {
        // Initialize the service
        luceneService.init();

        // Verify that partitioning is enabled
        boolean partitioningEnabled = (boolean) ReflectionTestUtils.getField(luceneService, "partitioningEnabled");
        assertTrue(partitioningEnabled, "Partitioning should be enabled");

        // Verify that the partition base directory was created
        assertTrue(Files.exists(testPartitionBasePath), "Partition base directory should exist");

        // Verify that at least one partition was created
        @SuppressWarnings("unchecked")
        List<String> activePartitions = (List<String>) ReflectionTestUtils.getField(luceneService, "activePartitions");
        assertNotNull(activePartitions, "Active partitions list should not be null");
        assertFalse(activePartitions.isEmpty(), "Active partitions list should not be empty");

        // Verify that the current partition was created
        String currentPartition = getCurrentPartitionName();
        assertTrue(activePartitions.contains(currentPartition), "Current partition should be in active partitions list");

        // Verify that the partition directory was created
        Path currentPartitionPath = Paths.get(testPartitionBasePath.toString(), currentPartition);
        assertTrue(Files.exists(currentPartitionPath), "Current partition directory should exist");
    }

    @Test
    void testIndexAndSearchWithPartitioning() throws IOException {
        // Initialize the service
        luceneService.init();

        // Create test log entries with different timestamps
        List<LogEntry> testLogs = new ArrayList<>();
        
        // Current month logs
        for (int i = 0; i < 5; i++) {
            testLogs.add(createTestLogEntry("current" + i, System.currentTimeMillis()));
        }
        
        // Previous month logs
        LocalDateTime prevMonth = LocalDateTime.now().minusMonths(1);
        long prevMonthTimestamp = prevMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        for (int i = 0; i < 5; i++) {
            testLogs.add(createTestLogEntry("prev" + i, prevMonthTimestamp));
        }
        
        // Index the logs
        int indexed = luceneService.indexLogEntries(testLogs);
        assertEquals(10, indexed, "All logs should be indexed");
        
        // Search for logs
        List<LogEntry> results = luceneService.search("test", false, null, null);
        assertEquals(10, results.size(), "All logs should be found");
        
        // Search for logs by level
        results = luceneService.findByLevel("INFO");
        assertEquals(10, results.size(), "All logs should be found by level");
        
        // Search for logs by source
        results = luceneService.findBySource("test-source");
        assertEquals(10, results.size(), "All logs should be found by source");
        
        // Search for a specific log by ID
        LogEntry log = luceneService.findById(testLogs.get(0).id());
        assertNotNull(log, "Log should be found by ID");
        assertEquals(testLogs.get(0).id(), log.id(), "Log ID should match");
    }

    @Test
    void testDeleteLogsWithPartitioning() throws IOException {
        // Initialize the service
        luceneService.init();

        // Create test log entries with different timestamps
        List<LogEntry> testLogs = new ArrayList<>();
        
        // Current month logs
        for (int i = 0; i < 5; i++) {
            testLogs.add(createTestLogEntry("current" + i, System.currentTimeMillis()));
        }
        
        // Previous month logs
        LocalDateTime prevMonth = LocalDateTime.now().minusMonths(1);
        long prevMonthTimestamp = prevMonth.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        for (int i = 0; i < 5; i++) {
            testLogs.add(createTestLogEntry("prev" + i, prevMonthTimestamp));
        }
        
        // Index the logs
        luceneService.indexLogEntries(testLogs);
        
        // Delete logs from previous month
        long cutoffTimestamp = LocalDateTime.now().minusDays(15)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        // Mock the archive service to return success
        when(archiveService.archiveLogsBeforeDeletion(any())).thenReturn(true);
        
        // Delete logs older than the cutoff timestamp
        long deleted = luceneService.deleteLogsOlderThan(cutoffTimestamp);
        assertEquals(5, deleted, "Previous month logs should be deleted");
        
        // Verify that only current month logs remain
        List<LogEntry> results = luceneService.search("test", false, null, null);
        assertEquals(5, results.size(), "Only current month logs should remain");
        
        // All remaining logs should have IDs starting with "current"
        for (LogEntry log : results) {
            assertTrue(log.id().startsWith("current"), "Remaining log IDs should start with 'current'");
        }
    }

    /**
     * Helper method to create a test log entry.
     */
    private LogEntry createTestLogEntry(String id, long timestamp) {
        return new LogEntry(
                id,
                timestamp,
                timestamp,
                "INFO",
                "This is a test log message",
                "test-source",
                new HashMap<>(),
                "Raw test log content"
        );
    }

    /**
     * Helper method to get the current partition name.
     */
    private String getCurrentPartitionName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return "partition_" + now.format(formatter);
    }
}