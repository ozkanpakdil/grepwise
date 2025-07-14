package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.model.NotificationChannel;
import io.github.ozkanpakdil.grepwise.service.AlarmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST controller for alarm management.
 */
@RestController
@RequestMapping("/api/alarms")
@CrossOrigin(origins = "*")
public class AlarmController {
    private static final Logger logger = LoggerFactory.getLogger(AlarmController.class);

    @Autowired
    private AlarmService alarmService;

    /**
     * Get all alarms.
     *
     * @return List of all alarms
     */
    @GetMapping
    public ResponseEntity<List<Alarm>> getAllAlarms() {
        try {
            List<Alarm> alarms = alarmService.getAllAlarms();
            return ResponseEntity.ok(alarms);
        } catch (Exception e) {
            logger.error("Error retrieving alarms: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get an alarm by ID.
     *
     * @param id The alarm ID
     * @return The alarm
     */
    @GetMapping("/{id}")
    public ResponseEntity<Alarm> getAlarmById(@PathVariable String id) {
        try {
            Alarm alarm = alarmService.getAlarmById(id);
            if (alarm != null) {
                return ResponseEntity.ok(alarm);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving alarm {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new alarm.
     *
     * @param alarmRequest The alarm creation request
     * @return The created alarm
     */
    @PostMapping
    public ResponseEntity<?> createAlarm(@RequestBody AlarmRequest alarmRequest) {
        try {
            // Convert request to Alarm entity
            Alarm alarm = convertRequestToAlarm(alarmRequest);

            Alarm createdAlarm = alarmService.createAlarm(alarm);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAlarm);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid alarm creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating alarm: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update an existing alarm.
     *
     * @param id The alarm ID
     * @param alarmRequest The alarm update request
     * @return The updated alarm
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAlarm(@PathVariable String id, @RequestBody AlarmRequest alarmRequest) {
        try {
            // Convert request to Alarm entity
            Alarm alarm = convertRequestToAlarm(alarmRequest);
            alarm.setId(id);

            Alarm updatedAlarm = alarmService.updateAlarm(alarm);
            return ResponseEntity.ok(updatedAlarm);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid alarm update request for {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating alarm {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Delete an alarm.
     *
     * @param id The alarm ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlarm(@PathVariable String id) {
        try {
            boolean deleted = alarmService.deleteAlarm(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Alarm deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting alarm {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Toggle alarm enabled status.
     *
     * @param id The alarm ID
     * @return The updated alarm
     */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggleAlarm(@PathVariable String id) {
        try {
            Alarm updatedAlarm = alarmService.toggleAlarmEnabled(id);
            if (updatedAlarm != null) {
                return ResponseEntity.ok(updatedAlarm);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error toggling alarm {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get alarms by enabled status.
     *
     * @param enabled The enabled status
     * @return List of alarms with the specified status
     */
    @GetMapping("/status/{enabled}")
    public ResponseEntity<List<Alarm>> getAlarmsByEnabled(@PathVariable Boolean enabled) {
        try {
            List<Alarm> alarms = alarmService.getAlarmsByEnabled(enabled);
            return ResponseEntity.ok(alarms);
        } catch (Exception e) {
            logger.error("Error retrieving alarms by enabled status {}: {}", enabled, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Evaluate a specific alarm.
     *
     * @param id The alarm ID
     * @return Evaluation result
     */
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<?> evaluateAlarm(@PathVariable String id) {
        try {
            Alarm alarm = alarmService.getAlarmById(id);
            if (alarm == null) {
                return ResponseEntity.notFound().build();
            }

            boolean triggered = alarmService.evaluateAlarm(alarm);
            return ResponseEntity.ok(Map.of(
                "alarmId", id,
                "triggered", triggered,
                "evaluatedAt", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            logger.error("Error evaluating alarm {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get alarm statistics.
     *
     * @return Alarm statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAlarmStatistics() {
        try {
            Map<String, Object> stats = alarmService.getAlarmStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error retrieving alarm statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Convert AlarmRequest to Alarm entity.
     *
     * @param request The alarm request
     * @return The alarm entity
     */
    private Alarm convertRequestToAlarm(AlarmRequest request) {
        Alarm alarm = new Alarm();
        alarm.setName(request.getName());
        alarm.setDescription(request.getDescription());
        alarm.setQuery(request.getQuery());
        alarm.setCondition(request.getCondition());
        alarm.setThreshold(request.getThreshold());
        alarm.setTimeWindowMinutes(request.getTimeWindowMinutes());
        alarm.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);

        // Convert notification channels
        List<NotificationChannel> channels = new ArrayList<>();
        if (request.getNotificationEmail() != null && !request.getNotificationEmail().trim().isEmpty()) {
            channels.add(new NotificationChannel("EMAIL", request.getNotificationEmail()));
        }
        if (request.getNotificationChannels() != null) {
            channels.addAll(request.getNotificationChannels());
        }
        alarm.setNotificationChannels(channels);

        // Set throttling configuration
        if (request.getThrottleWindowMinutes() != null) {
            alarm.setThrottleWindowMinutes(request.getThrottleWindowMinutes());
        }
        if (request.getMaxNotificationsPerWindow() != null) {
            alarm.setMaxNotificationsPerWindow(request.getMaxNotificationsPerWindow());
        }

        // Set grouping configuration
        if (request.getGroupingKey() != null) {
            alarm.setGroupingKey(request.getGroupingKey());
        }
        if (request.getGroupingWindowMinutes() != null) {
            alarm.setGroupingWindowMinutes(request.getGroupingWindowMinutes());
        }

        return alarm;
    }

    /**
     * Request DTO for alarm creation/update.
     */
    public static class AlarmRequest {
        private String name;
        private String description;
        private String query;
        private String condition;
        private Integer threshold;
        private Integer timeWindowMinutes;
        private Boolean enabled;
        private String notificationEmail;
        private List<NotificationChannel> notificationChannels;

        // Throttling configuration
        private Integer throttleWindowMinutes;
        private Integer maxNotificationsPerWindow;

        // Grouping configuration
        private String groupingKey;
        private Integer groupingWindowMinutes;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public Integer getThreshold() { return threshold; }
        public void setThreshold(Integer threshold) { this.threshold = threshold; }

        public Integer getTimeWindowMinutes() { return timeWindowMinutes; }
        public void setTimeWindowMinutes(Integer timeWindowMinutes) { this.timeWindowMinutes = timeWindowMinutes; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public String getNotificationEmail() { return notificationEmail; }
        public void setNotificationEmail(String notificationEmail) { this.notificationEmail = notificationEmail; }

        public List<NotificationChannel> getNotificationChannels() { return notificationChannels; }
        public void setNotificationChannels(List<NotificationChannel> notificationChannels) { 
            this.notificationChannels = notificationChannels; 
        }

        public Integer getThrottleWindowMinutes() { return throttleWindowMinutes; }
        public void setThrottleWindowMinutes(Integer throttleWindowMinutes) { this.throttleWindowMinutes = throttleWindowMinutes; }

        public Integer getMaxNotificationsPerWindow() { return maxNotificationsPerWindow; }
        public void setMaxNotificationsPerWindow(Integer maxNotificationsPerWindow) { this.maxNotificationsPerWindow = maxNotificationsPerWindow; }

        public String getGroupingKey() { return groupingKey; }
        public void setGroupingKey(String groupingKey) { this.groupingKey = groupingKey; }

        public Integer getGroupingWindowMinutes() { return groupingWindowMinutes; }
        public void setGroupingWindowMinutes(Integer groupingWindowMinutes) { this.groupingWindowMinutes = groupingWindowMinutes; }
    }
}
