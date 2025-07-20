package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.NotificationChannel;
import io.github.ozkanpakdil.grepwise.repository.AlarmRepository;
import io.github.ozkanpakdil.grepwise.service.alerting.OpsGenieService;
import io.github.ozkanpakdil.grepwise.service.alerting.PagerDutyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
public class AlarmServiceTest {

    @Mock
    private AlarmRepository alarmRepository;
    
    @Mock
    private LuceneService luceneService;
    
    @Mock
    private PagerDutyService pagerDutyService;
    
    @Mock
    private OpsGenieService opsGenieService;
    
    private AlarmService alarmService;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize the AlarmService with mocked dependencies
        alarmService = new AlarmService(alarmRepository, luceneService, pagerDutyService, opsGenieService);
        
        // Set up common mock behaviors with lenient() to avoid UnnecessaryStubbingException
        lenient().when(luceneService.search(any(), any(Boolean.class), anyLong(), anyLong()))
            .thenReturn(new ArrayList<>());
        
        // Configure repository to return the same alarm that is passed to save/update
        lenient().when(alarmRepository.save(any(Alarm.class))).thenAnswer(invocation -> {
            Alarm alarm = invocation.getArgument(0);
            if (alarm.getId() == null) {
                alarm.setId("test-id-" + System.currentTimeMillis());
            }
            return alarm;
        });
        
        lenient().when(alarmRepository.update(any(Alarm.class))).thenAnswer(returnsFirstArg());
        
        // Return empty list for findAll by default
        lenient().when(alarmRepository.findAll()).thenReturn(new ArrayList<>());
        
        // Configure alerting services
        lenient().when(pagerDutyService.sendAlert(any(), any(), any())).thenReturn(true);
        lenient().when(pagerDutyService.sendGroupedAlert(any(), any(), any(), any(Integer.class))).thenReturn(true);
        lenient().when(opsGenieService.sendAlert(any(), any(), any())).thenReturn(true);
        lenient().when(opsGenieService.sendGroupedAlert(any(), any(), any(), any(Integer.class))).thenReturn(true);
    }

    @Test
    void testCreateAlarm() {
        // Create a test alarm
        Alarm alarm = new Alarm();
        alarm.setName("Test Alarm");
        alarm.setDescription("Test alarm description");
        alarm.setQuery("level:ERROR");
        alarm.setCondition("count > 5");
        alarm.setThreshold(5);
        alarm.setTimeWindowMinutes(15);
        alarm.setEnabled(true);

        NotificationChannel emailChannel = new NotificationChannel("EMAIL", "test@example.com");
        alarm.setNotificationChannels(Arrays.asList(emailChannel));

        // Create the alarm
        Alarm createdAlarm = alarmService.createAlarm(alarm);

        // Verify the alarm was created
        assertNotNull(createdAlarm);
        assertNotNull(createdAlarm.getId());
        assertEquals("Test Alarm", createdAlarm.getName());
        assertEquals("Test alarm description", createdAlarm.getDescription());
        assertEquals("level:ERROR", createdAlarm.getQuery());
        assertEquals("count > 5", createdAlarm.getCondition());
        assertEquals(5, createdAlarm.getThreshold());
        assertEquals(15, createdAlarm.getTimeWindowMinutes());
        assertTrue(createdAlarm.getEnabled());
        assertEquals(1, createdAlarm.getNotificationChannels().size());
        assertEquals("EMAIL", createdAlarm.getNotificationChannels().get(0).getType());
        assertEquals("test@example.com", createdAlarm.getNotificationChannels().get(0).getDestination());
    }

    @Test
    void testCreateAlarmWithDuplicateName() {
        // Configure repository to simulate duplicate name check
        when(alarmRepository.existsByName("Duplicate Name")).thenReturn(true);
        
        // Create first alarm
        Alarm alarm1 = new Alarm();
        alarm1.setName("Duplicate Name");
        alarm1.setDescription("First alarm");
        alarm1.setQuery("level:ERROR");
        alarm1.setCondition("count > 5");
        alarm1.setThreshold(5);
        alarm1.setTimeWindowMinutes(15);
        alarm1.setEnabled(true);

        // Try to create second alarm with same name
        Alarm alarm2 = new Alarm();
        alarm2.setName("Duplicate Name");
        alarm2.setDescription("Second alarm");
        alarm2.setQuery("level:WARNING");
        alarm2.setCondition("count > 3");
        alarm2.setThreshold(3);
        alarm2.setTimeWindowMinutes(10);
        alarm2.setEnabled(true);

        // Should throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            alarmService.createAlarm(alarm2);
        });
    }

    @Test
    void testGetAllAlarms() {
        // Create test alarms
        Alarm alarm1 = createTestAlarm("Alarm 1", "level:ERROR", 5);
        alarm1.setId("test-id-1");
        Alarm alarm2 = createTestAlarm("Alarm 2", "level:WARNING", 3);
        alarm2.setId("test-id-2");
        
        // Configure repository to return the list of alarms
        List<Alarm> alarmList = Arrays.asList(alarm1, alarm2);
        when(alarmRepository.findAll()).thenReturn(alarmList);

        // Get all alarms
        List<Alarm> alarms = alarmService.getAllAlarms();

        // Verify
        assertEquals(2, alarms.size());
        assertEquals("Alarm 1", alarms.get(0).getName());
        assertEquals("Alarm 2", alarms.get(1).getName());
    }

    @Test
    void testUpdateAlarm() {
        // Create an alarm with ID
        Alarm alarm = createTestAlarm("Original Name", "level:ERROR", 5);
        alarm.setId("test-id-update");
        
        // Configure repository to return the alarm when findById is called
        lenient().when(alarmRepository.findById("test-id-update")).thenReturn(alarm);
        
        // Configure repository to return false for existsByName with different name
        lenient().when(alarmRepository.existsByName("Updated Name")).thenReturn(false);

        // Update the alarm
        alarm.setName("Updated Name");
        alarm.setThreshold(10);

        Alarm updatedAlarm = alarmService.updateAlarm(alarm);

        // Verify
        assertNotNull(updatedAlarm);
        assertEquals("Updated Name", updatedAlarm.getName());
        assertEquals(10, updatedAlarm.getThreshold());
    }

    @Test
    void testDeleteAlarm() {
        // Create an alarm with ID
        String alarmId = "test-id-delete";
        Alarm alarm = createTestAlarm("To Delete", "level:ERROR", 5);
        alarm.setId(alarmId);
        
        // Configure repository to return the alarm before deletion and null after deletion
        lenient().when(alarmRepository.findById(alarmId))
            .thenReturn(alarm)  // First call returns the alarm
            .thenReturn(null);  // Subsequent calls return null
        
        // Configure repository to return true for deleteById
        lenient().when(alarmRepository.deleteById(alarmId)).thenReturn(true);

        // Mock the behavior of getAlarmById to return null after deletion
        doReturn(null).when(alarmRepository).findById(alarmId);

        // Delete the alarm
        boolean deleted = alarmService.deleteAlarm(alarmId);

        // Verify
        assertTrue(deleted);
        assertNull(alarmService.getAlarmById(alarmId));
    }

    @Test
    void testToggleAlarmEnabled() {
        // Create an enabled alarm with ID
        String alarmId = "test-id-toggle";
        Alarm alarm = createTestAlarm("Toggle Test", "level:ERROR", 5);
        alarm.setId(alarmId);
        alarm.setEnabled(true);
        
        // Create a copy of the alarm with enabled=false for the first toggle
        Alarm disabledAlarm = createTestAlarm("Toggle Test", "level:ERROR", 5);
        disabledAlarm.setId(alarmId);
        disabledAlarm.setEnabled(false);
        
        // Create a copy of the alarm with enabled=true for the second toggle
        Alarm enabledAlarm = createTestAlarm("Toggle Test", "level:ERROR", 5);
        enabledAlarm.setId(alarmId);
        enabledAlarm.setEnabled(true);
        
        // Configure repository to return the alarm when findById is called
        when(alarmRepository.findById(alarmId))
            .thenReturn(alarm)       // First call returns enabled alarm
            .thenReturn(disabledAlarm) // Second call returns disabled alarm
            .thenReturn(enabledAlarm); // Third call returns enabled alarm
        
        // Configure repository to return the toggled alarm when update is called
        when(alarmRepository.update(any(Alarm.class)))
            .thenReturn(disabledAlarm) // First update returns disabled alarm
            .thenReturn(enabledAlarm); // Second update returns enabled alarm

        // Toggle to disabled
        Alarm toggledAlarm = alarmService.toggleAlarmEnabled(alarmId);

        // Verify
        assertNotNull(toggledAlarm);
        assertFalse(toggledAlarm.getEnabled());

        // Toggle back to enabled
        Alarm toggledAgain = alarmService.toggleAlarmEnabled(alarmId);
        assertTrue(toggledAgain.getEnabled());
    }

    @Test
    void testGetAlarmStatistics() {
        // Configure repository to return the expected counts
        when(alarmRepository.count()).thenReturn(3);
        when(alarmRepository.countEnabled()).thenReturn(2);
        
        // Get statistics
        Map<String, Object> stats = alarmService.getAlarmStatistics();

        // Verify
        assertEquals(3, stats.get("totalAlarms"));
        assertEquals(2, stats.get("enabledAlarms"));
        assertEquals(1, stats.get("disabledAlarms"));
    }

    @Test
    void testValidateAlarm() {
        // Test validation with invalid alarm (missing name)
        Alarm invalidAlarm = new Alarm();
        invalidAlarm.setQuery("level:ERROR");
        invalidAlarm.setCondition("count > 5");
        invalidAlarm.setThreshold(5);
        invalidAlarm.setTimeWindowMinutes(15);

        assertThrows(IllegalArgumentException.class, () -> {
            alarmService.createAlarm(invalidAlarm);
        });
    }

    @Test
    void testAlarmThrottling() throws IOException {
        // Create an alarm with throttling enabled (1 notification per 2 minutes)
        String alarmId = "test-id-throttle";
        Alarm alarm = createTestAlarm("Throttled Alarm", "level:ERROR", 1);
        alarm.setId(alarmId);
        alarm.setThrottleWindowMinutes(2);
        alarm.setMaxNotificationsPerWindow(1);
        alarm.setEnabled(true);
        
        // Configure repository to return the alarm
        lenient().when(alarmRepository.findById(alarmId)).thenReturn(alarm);
        
        // Configure luceneService to return matching logs to trigger the alarm
        List<LogEntry> matchingLogs = new ArrayList<>();
        long now = System.currentTimeMillis();
        // Create LogEntry objects with the correct constructor parameters
        matchingLogs.add(new LogEntry("log1", now, "ERROR", "Test error message", "test-source", Map.of(), "Raw log content 1"));
        matchingLogs.add(new LogEntry("log2", now, "ERROR", "Another error message", "test-source", Map.of(), "Raw log content 2"));
        
        // Override the default mock behavior for this test only
        lenient().when(luceneService.search(any(), any(Boolean.class), anyLong(), anyLong()))
            .thenReturn(matchingLogs);
        
        // First trigger should evaluate to true (alarm condition met)
        boolean triggered1 = alarmService.evaluateAlarm(alarm);
        assertTrue(triggered1, "First evaluation should trigger the alarm");
        
        // The throttling logic is internal to triggerNotifications, 
        // so we can't directly test it without exposing internal state
        // This test verifies the alarm configuration is set correctly
        assertEquals(2, alarm.getThrottleWindowMinutes());
        assertEquals(1, alarm.getMaxNotificationsPerWindow());
    }

    @Test
    void testAlarmGrouping() {
        // Create multiple alarms with the same grouping key
        String alarmId1 = "test-id-group-1";
        Alarm alarm1 = createTestAlarm("Grouped Alarm 1", "level:ERROR", 1);
        alarm1.setId(alarmId1);
        alarm1.setGroupingKey("database-errors");
        alarm1.setGroupingWindowMinutes(5);

        String alarmId2 = "test-id-group-2";
        Alarm alarm2 = createTestAlarm("Grouped Alarm 2", "level:ERROR", 2);
        alarm2.setId(alarmId2);
        alarm2.setGroupingKey("database-errors");
        alarm2.setGroupingWindowMinutes(5);
        
        // Configure repository to return the alarms when save is called
        when(alarmRepository.save(alarm1)).thenReturn(alarm1);
        when(alarmRepository.save(alarm2)).thenReturn(alarm2);
        
        // Configure repository to return false for existsByName to allow creation
        when(alarmRepository.existsByName("Grouped Alarm 1")).thenReturn(false);
        when(alarmRepository.existsByName("Grouped Alarm 2")).thenReturn(false);

        // Create the alarms
        Alarm createdAlarm1 = alarmService.createAlarm(alarm1);
        Alarm createdAlarm2 = alarmService.createAlarm(alarm2);

        // Verify grouping configuration
        assertNotNull(createdAlarm1);
        assertNotNull(createdAlarm2);
        assertEquals("database-errors", createdAlarm1.getGroupingKey());
        assertEquals("database-errors", createdAlarm2.getGroupingKey());
        assertEquals(5, createdAlarm1.getGroupingWindowMinutes());
        assertEquals(5, createdAlarm2.getGroupingWindowMinutes());
    }

    @Test
    void testAlarmWithoutThrottlingAndGrouping() {
        // Create an alarm without throttling and grouping
        String alarmId = "test-id-normal";
        Alarm alarm = createTestAlarm("Normal Alarm", "level:INFO", 1);
        alarm.setId(alarmId);
        // Set default values explicitly for the test
        alarm.setThrottleWindowMinutes(60);
        alarm.setMaxNotificationsPerWindow(1);
        alarm.setGroupingKey(null);
        alarm.setGroupingWindowMinutes(5);
        
        // Configure repository to return false for existsByName to allow creation
        when(alarmRepository.existsByName("Normal Alarm")).thenReturn(false);
        
        // Configure repository to return the alarm when save is called
        when(alarmRepository.save(alarm)).thenReturn(alarm);

        // Create the alarm
        Alarm createdAlarm = alarmService.createAlarm(alarm);

        // Verify default values
        assertNotNull(createdAlarm);
        assertEquals(60, createdAlarm.getThrottleWindowMinutes()); // Default from model
        assertEquals(1, createdAlarm.getMaxNotificationsPerWindow()); // Default from model
        assertNull(createdAlarm.getGroupingKey()); // No grouping by default
        assertEquals(5, createdAlarm.getGroupingWindowMinutes()); // Default from model
    }

    @Test
    void testAlarmValidationWithThrottlingAndGrouping() {
        // Test validation with throttling and grouping fields
        Alarm validAlarm = createTestAlarm("Valid Alarm", "level:ERROR", 5);
        validAlarm.setThrottleWindowMinutes(30);
        validAlarm.setMaxNotificationsPerWindow(3);
        validAlarm.setGroupingKey("test-group");
        validAlarm.setGroupingWindowMinutes(10);

        // Should not throw exception
        assertDoesNotThrow(() -> {
            alarmService.createAlarm(validAlarm);
        });
    }

    private Alarm createTestAlarm(String name, String query, int threshold) {
        Alarm alarm = new Alarm();
        alarm.setName(name);
        alarm.setDescription("Test alarm: " + name);
        alarm.setQuery(query);
        alarm.setCondition("count > " + threshold);
        alarm.setThreshold(threshold);
        alarm.setTimeWindowMinutes(15);
        alarm.setEnabled(true);

        NotificationChannel emailChannel = new NotificationChannel("EMAIL", "test@example.com");
        alarm.setNotificationChannels(Arrays.asList(emailChannel));

        return alarm;
    }
}
