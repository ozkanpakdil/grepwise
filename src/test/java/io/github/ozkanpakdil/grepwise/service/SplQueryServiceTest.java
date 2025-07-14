package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SplQueryServiceTest {

    @Mock
    private LuceneService luceneService;

    @InjectMocks
    private SplQueryService splQueryService;

    private List<LogEntry> sampleLogEntries;

    @BeforeEach
    void setUp() {
        // Create sample log entries for testing
        sampleLogEntries = Arrays.asList(
            new LogEntry("1", System.currentTimeMillis(), "ERROR", "Database connection failed", "app.log", new HashMap<>(), "raw1"),
            new LogEntry("2", System.currentTimeMillis(), "INFO", "User logged in", "app.log", new HashMap<>(), "raw2"),
            new LogEntry("3", System.currentTimeMillis(), "ERROR", "File not found", "system.log", new HashMap<>(), "raw3"),
            new LogEntry("4", System.currentTimeMillis(), "WARN", "Memory usage high", "app.log", new HashMap<>(), "raw4")
        );
    }

    @Test
    void testSimpleSearchCommand() throws IOException {
        // Mock the lucene service to return sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test simple search
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search error");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.LOG_ENTRIES, result.getResultType());
        assertNotNull(result.getLogEntries());
        assertEquals(4, result.getLogEntries().size());
    }

    @Test
    void testSearchWithFieldFilter() throws IOException {
        // Mock the lucene service for level search
        when(luceneService.findByLevel("ERROR"))
            .thenReturn(Arrays.asList(sampleLogEntries.get(0), sampleLogEntries.get(2)));

        // Test field-specific search
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search level=ERROR");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.LOG_ENTRIES, result.getResultType());
        assertEquals(2, result.getLogEntries().size());
    }

    @Test
    void testSearchWithWhereClause() throws IOException {
        // Mock the lucene service to return all sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test search with where clause
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search * | where level=ERROR");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.LOG_ENTRIES, result.getResultType());
        // Should filter to only ERROR level entries
        assertEquals(2, result.getLogEntries().size());
        assertTrue(result.getLogEntries().stream().allMatch(entry -> "ERROR".equals(entry.level())));
    }

    @Test
    void testStatsCountCommand() throws IOException {
        // Mock the lucene service to return all sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test stats count
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search * | stats count");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.STATISTICS, result.getResultType());
        assertNotNull(result.getStatistics());
        assertEquals(4, result.getStatistics().get("count"));
    }

    @Test
    void testStatsCountByLevel() throws IOException {
        // Mock the lucene service to return all sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test stats count by level
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search * | stats count by level");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.STATISTICS, result.getResultType());
        assertNotNull(result.getStatistics());
        
        Map<String, Object> stats = result.getStatistics();
        assertEquals(2L, stats.get("ERROR"));
        assertEquals(1L, stats.get("INFO"));
        assertEquals(1L, stats.get("WARN"));
    }

    @Test
    void testSortCommand() throws IOException {
        // Mock the lucene service to return all sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test sort by level
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search * | sort level");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.LOG_ENTRIES, result.getResultType());
        assertEquals(4, result.getLogEntries().size());
        
        // Check if sorted by level (ERROR, ERROR, INFO, WARN)
        List<LogEntry> entries = result.getLogEntries();
        assertEquals("ERROR", entries.get(0).level());
        assertEquals("ERROR", entries.get(1).level());
        assertEquals("INFO", entries.get(2).level());
        assertEquals("WARN", entries.get(3).level());
    }

    @Test
    void testHeadCommand() throws IOException {
        // Mock the lucene service to return all sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test head command
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search * | head 2");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.LOG_ENTRIES, result.getResultType());
        assertEquals(2, result.getLogEntries().size());
    }

    @Test
    void testTailCommand() throws IOException {
        // Mock the lucene service to return all sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test tail command
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search * | tail 2");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.LOG_ENTRIES, result.getResultType());
        assertEquals(2, result.getLogEntries().size());
    }

    @Test
    void testComplexQuery() throws IOException {
        // Mock the lucene service to return all sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test complex query: search, filter, sort, and limit
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery(
            "search * | where level=ERROR | sort -timestamp | head 1"
        );

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.LOG_ENTRIES, result.getResultType());
        assertEquals(1, result.getLogEntries().size());
        assertEquals("ERROR", result.getLogEntries().get(0).level());
    }

    @Test
    void testInvalidCommand() throws IOException {
        // Mock the lucene service to return sample data
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
            .thenReturn(sampleLogEntries);

        // Test with invalid command - should still work but log warning
        SplQueryService.SplQueryResult result = splQueryService.executeSplQuery("search * | invalidcommand");

        assertNotNull(result);
        assertEquals(SplQueryService.SplQueryResult.ResultType.LOG_ENTRIES, result.getResultType());
        assertEquals(4, result.getLogEntries().size()); // Should return all entries since invalid command is ignored
    }
}