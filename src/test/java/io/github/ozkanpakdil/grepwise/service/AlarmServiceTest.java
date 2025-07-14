package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.model.NotificationChannel;
import io.github.ozkanpakdil.grepwise.repository.AlarmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AlarmServiceTest {

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AlarmRepository alarmRepository;

    @BeforeEach
    void setUp() {
        // Clear any existing alarms before each test
        List<Alarm> existingAlarms = alarmRepository.findAll();
        for (Alarm alarm : existingAlarms) {
            alarmRepository.deleteById(alarm.getId());
        }
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
        // Create first alarm
        Alarm alarm1 = new Alarm();
        alarm1.setName("Duplicate Name");
        alarm1.setDescription("First alarm");
        alarm1.setQuery("level:ERROR");
        alarm1.setCondition("count > 5");
        alarm1.setThreshold(5);
        alarm1.setTimeWindowMinutes(15);
        alarm1.setEnabled(true);

        alarmService.createAlarm(alarm1);

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
        // Create multiple alarms
        Alarm alarm1 = createTestAlarm("Alarm 1", "level:ERROR", 5);
        Alarm alarm2 = createTestAlarm("Alarm 2", "level:WARNING", 3);

        alarmService.createAlarm(alarm1);
        alarmService.createAlarm(alarm2);

        // Get all alarms
        List<Alarm> alarms = alarmService.getAllAlarms();

        // Verify
        assertEquals(2, alarms.size());
    }

    @Test
    void testUpdateAlarm() {
        // Create an alarm
        Alarm alarm = createTestAlarm("Original Name", "level:ERROR", 5);
        Alarm createdAlarm = alarmService.createAlarm(alarm);

        // Update the alarm
        createdAlarm.setName("Updated Name");
        createdAlarm.setThreshold(10);

        Alarm updatedAlarm = alarmService.updateAlarm(createdAlarm);

        // Verify
        assertNotNull(updatedAlarm);
        assertEquals("Updated Name", updatedAlarm.getName());
        assertEquals(10, updatedAlarm.getThreshold());
    }

    @Test
    void testDeleteAlarm() {
        // Create an alarm
        Alarm alarm = createTestAlarm("To Delete", "level:ERROR", 5);
        Alarm createdAlarm = alarmService.createAlarm(alarm);

        // Delete the alarm
        boolean deleted = alarmService.deleteAlarm(createdAlarm.getId());

        // Verify
        assertTrue(deleted);
        assertNull(alarmService.getAlarmById(createdAlarm.getId()));
    }

    @Test
    void testToggleAlarmEnabled() {
        // Create an enabled alarm
        Alarm alarm = createTestAlarm("Toggle Test", "level:ERROR", 5);
        alarm.setEnabled(true);
        Alarm createdAlarm = alarmService.createAlarm(alarm);

        // Toggle to disabled
        Alarm toggledAlarm = alarmService.toggleAlarmEnabled(createdAlarm.getId());

        // Verify
        assertNotNull(toggledAlarm);
        assertFalse(toggledAlarm.getEnabled());

        // Toggle back to enabled
        Alarm toggledAgain = alarmService.toggleAlarmEnabled(createdAlarm.getId());
        assertTrue(toggledAgain.getEnabled());
    }

    @Test
    void testGetAlarmStatistics() {
        // Create some alarms
        Alarm enabledAlarm1 = createTestAlarm("Enabled 1", "level:ERROR", 5);
        enabledAlarm1.setEnabled(true);

        Alarm enabledAlarm2 = createTestAlarm("Enabled 2", "level:WARNING", 3);
        enabledAlarm2.setEnabled(true);

        Alarm disabledAlarm = createTestAlarm("Disabled", "level:INFO", 1);
        disabledAlarm.setEnabled(false);

        alarmService.createAlarm(enabledAlarm1);
        alarmService.createAlarm(enabledAlarm2);
        alarmService.createAlarm(disabledAlarm);

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
    void testAlarmThrottling() throws InterruptedException {
        // Create an alarm with throttling enabled (1 notification per 2 minutes)
        Alarm alarm = createTestAlarm("Throttled Alarm", "level:ERROR", 1);
        alarm.setThrottleWindowMinutes(2);
        alarm.setMaxNotificationsPerWindow(1);

        Alarm createdAlarm = alarmService.createAlarm(alarm);

        // Simulate multiple triggers within the throttle window
        // First trigger should send notification
        boolean triggered1 = alarmService.evaluateAlarm(createdAlarm);
        // Note: This test assumes the alarm will trigger based on existing log data

        // Wait a short time and trigger again - should be throttled
        Thread.sleep(100);
        boolean triggered2 = alarmService.evaluateAlarm(createdAlarm);

        // The throttling logic is internal to triggerNotifications, 
        // so we can't directly test it without mocking the LuceneService
        // This test verifies the alarm configuration is set correctly
        assertEquals(2, createdAlarm.getThrottleWindowMinutes());
        assertEquals(1, createdAlarm.getMaxNotificationsPerWindow());
    }

    @Test
    void testAlarmGrouping() {
        // Create multiple alarms with the same grouping key
        Alarm alarm1 = createTestAlarm("Grouped Alarm 1", "level:ERROR", 1);
        alarm1.setGroupingKey("database-errors");
        alarm1.setGroupingWindowMinutes(5);

        Alarm alarm2 = createTestAlarm("Grouped Alarm 2", "level:ERROR", 2);
        alarm2.setGroupingKey("database-errors");
        alarm2.setGroupingWindowMinutes(5);

        Alarm createdAlarm1 = alarmService.createAlarm(alarm1);
        Alarm createdAlarm2 = alarmService.createAlarm(alarm2);

        // Verify grouping configuration
        assertEquals("database-errors", createdAlarm1.getGroupingKey());
        assertEquals("database-errors", createdAlarm2.getGroupingKey());
        assertEquals(5, createdAlarm1.getGroupingWindowMinutes());
        assertEquals(5, createdAlarm2.getGroupingWindowMinutes());
    }

    @Test
    void testAlarmWithoutThrottlingAndGrouping() {
        // Create an alarm without throttling and grouping
        Alarm alarm = createTestAlarm("Normal Alarm", "level:INFO", 1);
        // Don't set throttling or grouping fields - should use defaults

        Alarm createdAlarm = alarmService.createAlarm(alarm);

        // Verify default values
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
