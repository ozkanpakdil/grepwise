package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.NotificationChannel;
import io.github.ozkanpakdil.grepwise.repository.AlarmRepository;
import io.github.ozkanpakdil.grepwise.service.alerting.OpsGenieService;
import io.github.ozkanpakdil.grepwise.service.alerting.PagerDutyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing alarms and alarm evaluation.
 */
@Service
public class AlarmService {
    private static final Logger logger = LoggerFactory.getLogger(AlarmService.class);
    // Throttling: Track notification history for each alarm
    // Key: alarmId, Value: List of notification timestamps
    private final Map<String, List<Long>> notificationHistory = new ConcurrentHashMap<>();
    // Grouping: Track pending grouped notifications
    // Key: groupingKey, Value: List of alarms waiting to be grouped
    private final Map<String, List<GroupedAlarmNotification>> pendingGroupedNotifications = new ConcurrentHashMap<>();
    @Autowired
    private AlarmRepository alarmRepository;
    @Autowired
    private LuceneService luceneService;
    @Autowired
    private PagerDutyService pagerDutyService;
    @Autowired
    private OpsGenieService opsGenieService;

    // Default constructor for Spring
    public AlarmService() {
    }

    // Constructor for testing
    public AlarmService(AlarmRepository alarmRepository, LuceneService luceneService,
                        PagerDutyService pagerDutyService, OpsGenieService opsGenieService) {
        this.alarmRepository = alarmRepository;
        this.luceneService = luceneService;
        this.pagerDutyService = pagerDutyService;
        this.opsGenieService = opsGenieService;
    }

    /**
     * Create a new alarm.
     *
     * @param alarm The alarm to create
     * @return The created alarm
     */
    public Alarm createAlarm(Alarm alarm) {
        logger.info("Creating new alarm: {}", alarm.getName());

        // Validate alarm
        validateAlarm(alarm);

        // Check if alarm with same name already exists
        if (alarmRepository.existsByName(alarm.getName())) {
            throw new IllegalArgumentException("Alarm with name '" + alarm.getName() + "' already exists");
        }

        return alarmRepository.save(alarm);
    }

    /**
     * Update an existing alarm.
     *
     * @param alarm The alarm to update
     * @return The updated alarm
     */
    public Alarm updateAlarm(Alarm alarm) {
        logger.info("Updating alarm: {}", alarm.getId());

        // Validate alarm
        validateAlarm(alarm);

        // Check if alarm exists
        Alarm existingAlarm = alarmRepository.findById(alarm.getId());
        if (existingAlarm == null) {
            throw new IllegalArgumentException("Alarm with ID '" + alarm.getId() + "' not found");
        }

        // Check if name conflicts with another alarm
        if (!existingAlarm.getName().equals(alarm.getName()) &&
                alarmRepository.existsByName(alarm.getName())) {
            throw new IllegalArgumentException("Alarm with name '" + alarm.getName() + "' already exists");
        }

        return alarmRepository.update(alarm);
    }

    /**
     * Get all alarms.
     *
     * @return List of all alarms
     */
    public List<Alarm> getAllAlarms() {
        return alarmRepository.findAll();
    }

    /**
     * Get an alarm by ID.
     *
     * @param id The alarm ID
     * @return The alarm, or null if not found
     */
    public Alarm getAlarmById(String id) {
        return alarmRepository.findById(id);
    }

    /**
     * Delete an alarm by ID.
     *
     * @param id The alarm ID
     * @return true if deleted, false if not found
     */
    public boolean deleteAlarm(String id) {
        logger.info("Deleting alarm: {}", id);
        return alarmRepository.deleteById(id);
    }

    /**
     * Get alarms by enabled status.
     *
     * @param enabled The enabled status
     * @return List of alarms with the specified status
     */
    public List<Alarm> getAlarmsByEnabled(Boolean enabled) {
        return alarmRepository.findByEnabled(enabled);
    }

    /**
     * Toggle alarm enabled status.
     *
     * @param id The alarm ID
     * @return The updated alarm, or null if not found
     */
    public Alarm toggleAlarmEnabled(String id) {
        Alarm alarm = alarmRepository.findById(id);
        if (alarm != null) {
            alarm.setEnabled(!alarm.getEnabled());
            return alarmRepository.update(alarm);
        }
        return null;
    }

    /**
     * Evaluate an alarm against current log data.
     *
     * @param alarm The alarm to evaluate
     * @return true if alarm condition is met, false otherwise
     */
    public boolean evaluateAlarm(Alarm alarm) {
        if (!alarm.getEnabled()) {
            return false;
        }

        try {
            // Calculate time window
            long currentTime = System.currentTimeMillis();
            long timeWindowMs = alarm.getTimeWindowMinutes() * 60 * 1000L;
            long startTime = currentTime - timeWindowMs;

            // Search for logs matching the alarm query within the time window
            List<LogEntry> matchingLogs = luceneService.search(
                    alarm.getQuery(),
                    false, // not regex for now
                    startTime,
                    currentTime
            );

            // Evaluate condition (for now, only support count-based conditions)
            int logCount = matchingLogs.size();

            // Parse condition (e.g., "count > 5")
            String condition = alarm.getCondition().toLowerCase().trim();
            if (condition.startsWith("count >")) {
                return logCount > alarm.getThreshold();
            } else if (condition.startsWith("count >=")) {
                return logCount >= alarm.getThreshold();
            } else if (condition.startsWith("count <")) {
                return logCount < alarm.getThreshold();
            } else if (condition.startsWith("count <=")) {
                return logCount <= alarm.getThreshold();
            } else if (condition.startsWith("count =") || condition.startsWith("count ==")) {
                return logCount == alarm.getThreshold();
            }

            logger.warn("Unsupported alarm condition: {}", condition);
            return false;

        } catch (Exception e) {
            logger.error("Error evaluating alarm {}: {}", alarm.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Scheduled task to process grouped notifications.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void processGroupedNotifications() {
        long currentTime = System.currentTimeMillis();

        // Process each grouping key
        for (Map.Entry<String, List<GroupedAlarmNotification>> entry : pendingGroupedNotifications.entrySet()) {
            String groupingKey = entry.getKey();
            List<GroupedAlarmNotification> groupedAlarms = entry.getValue();

            if (groupedAlarms.isEmpty()) {
                continue;
            }

            // Find the oldest alarm in the group to determine if grouping window has expired
            long oldestTimestamp = groupedAlarms.stream()
                    .mapToLong(GroupedAlarmNotification::triggeredAt)
                    .min()
                    .orElse(currentTime);

            // Get grouping window from the first alarm (all alarms in same group should have same window)
            Alarm firstAlarm = groupedAlarms.get(0).alarm();
            int groupingWindowMinutes = firstAlarm.getGroupingWindowMinutes() != null ?
                    firstAlarm.getGroupingWindowMinutes() : 5;
            long groupingWindowMs = groupingWindowMinutes * 60 * 1000L;

            // Check if grouping window has expired
            if (currentTime - oldestTimestamp >= groupingWindowMs) {
                sendGroupedNotification(groupingKey, new ArrayList<>(groupedAlarms), currentTime);
                groupedAlarms.clear(); // Clear the processed alarms
            }
        }

        // Clean up empty grouping keys
        pendingGroupedNotifications.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Send a grouped notification for multiple alarms.
     *
     * @param groupingKey   The grouping key
     * @param groupedAlarms List of grouped alarm notifications
     * @param currentTime   Current timestamp
     */
    private void sendGroupedNotification(String groupingKey, List<GroupedAlarmNotification> groupedAlarms, long currentTime) {
        if (groupedAlarms.isEmpty()) {
            return;
        }

        logger.info("Sending grouped notification for {} alarms with grouping key: {}",
                groupedAlarms.size(), groupingKey);

        // Get all unique notification channels from all alarms in the group
        Map<String, NotificationChannel> uniqueChannels = new HashMap<>();
        for (GroupedAlarmNotification groupedAlarm : groupedAlarms) {
            Alarm alarm = groupedAlarm.alarm();
            if (alarm.getNotificationChannels() != null) {
                for (NotificationChannel channel : alarm.getNotificationChannels()) {
                    String channelKey = channel.getType() + ":" + channel.getDestination();
                    uniqueChannels.put(channelKey, channel);
                }
            }
        }

        // Create grouped message
        StringBuilder message = new StringBuilder();
        message.append("GROUPED ALARMS TRIGGERED (").append(groupedAlarms.size()).append(" alarms)\n");
        message.append("Grouping Key: ").append(groupingKey).append("\n");
        message.append("Time: ").append(new java.util.Date(currentTime)).append("\n\n");

        for (int i = 0; i < groupedAlarms.size(); i++) {
            GroupedAlarmNotification groupedAlarm = groupedAlarms.get(i);
            Alarm alarm = groupedAlarm.alarm();
            message.append("Alarm ").append(i + 1).append(":\n");
            message.append("  Name: ").append(alarm.getName()).append("\n");
            message.append("  Description: ").append(alarm.getDescription()).append("\n");
            message.append("  Query: ").append(alarm.getQuery()).append("\n");
            message.append("  Condition: ").append(alarm.getCondition()).append("\n");
            message.append("  Threshold: ").append(alarm.getThreshold()).append("\n");
            message.append("  Triggered At: ").append(new java.util.Date(groupedAlarm.triggeredAt())).append("\n");
            if (i < groupedAlarms.size() - 1) {
                message.append("\n");
            }
        }

        // Send to all unique channels
        for (NotificationChannel channel : uniqueChannels.values()) {
            try {
                sendGroupedNotificationToChannel(message.toString(), channel, groupingKey, groupedAlarms.size());

                // Record notifications for throttling (for each alarm in the group)
                for (GroupedAlarmNotification groupedAlarm : groupedAlarms) {
                    recordNotification(groupedAlarm.alarm(), currentTime);
                }
            } catch (Exception e) {
                logger.error("Failed to send grouped notification via {}: {}",
                        channel.getType(), e.getMessage());
            }
        }
    }

    /**
     * Send a grouped notification to a specific channel.
     *
     * @param message     The grouped message
     * @param channel     The notification channel
     * @param groupingKey The grouping key
     * @param alarmCount  The number of alarms in the group
     */
    private void sendGroupedNotificationToChannel(String message, NotificationChannel channel, String groupingKey, int alarmCount) {
        switch (channel.getType().toUpperCase()) {
            case "EMAIL":
                // TODO: Implement email notification
                logger.info("EMAIL grouped notification to {}: {}", channel.getDestination(), message);
                break;
            case "SLACK":
                // TODO: Implement Slack notification
                logger.info("SLACK grouped notification to {}: {}", channel.getDestination(), message);
                break;
            case "WEBHOOK":
                // TODO: Implement webhook notification
                logger.info("WEBHOOK grouped notification to {}: {}", channel.getDestination(), message);
                break;
            case "PAGERDUTY":
                // Send grouped notification to PagerDuty
                boolean success = pagerDutyService.sendGroupedAlert(groupingKey, channel.getDestination(), message, alarmCount);
                if (success) {
                    logger.info("PagerDuty grouped notification sent successfully to integration key: {}", channel.getDestination());
                } else {
                    logger.error("Failed to send PagerDuty grouped notification for grouping key: {}", groupingKey);
                }
                break;
            case "OPSGENIE":
                // Send grouped notification to OpsGenie
                boolean opsGenieSuccess = opsGenieService.sendGroupedAlert(groupingKey, channel.getDestination(), message, alarmCount);
                if (opsGenieSuccess) {
                    logger.info("OpsGenie grouped notification sent successfully to API key: {}", channel.getDestination());
                } else {
                    logger.error("Failed to send OpsGenie grouped notification for grouping key: {}", groupingKey);
                }
                break;
            default:
                logger.warn("Unsupported notification channel type: {}", channel.getType());
        }
    }

    /**
     * Scheduled task to evaluate all enabled alarms.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void evaluateAllAlarms() {
        List<Alarm> enabledAlarms = alarmRepository.findByEnabled(true);

        for (Alarm alarm : enabledAlarms) {
            try {
                boolean triggered = evaluateAlarm(alarm);
                if (triggered) {
                    logger.info("Alarm triggered: {}", alarm.getName());
                    triggerNotifications(alarm);
                }
            } catch (Exception e) {
                logger.error("Error evaluating alarm {}: {}", alarm.getName(), e.getMessage());
            }
        }
    }

    /**
     * Trigger notifications for an alarm with throttling and grouping support.
     *
     * @param alarm The alarm that was triggered
     */
    private void triggerNotifications(Alarm alarm) {
        if (alarm.getNotificationChannels() == null || alarm.getNotificationChannels().isEmpty()) {
            logger.warn("No notification channels configured for alarm: {}", alarm.getName());
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Check throttling
        if (isThrottled(alarm, currentTime)) {
            logger.debug("Alarm {} is throttled, skipping notification", alarm.getName());
            return;
        }

        // Check if grouping is enabled
        if (alarm.getGroupingKey() != null && !alarm.getGroupingKey().trim().isEmpty()) {
            // Add to grouped notifications
            addToGroupedNotifications(alarm, currentTime);
        } else {
            // Send notification immediately (no grouping)
            sendNotificationImmediately(alarm, currentTime);
        }
    }

    /**
     * Check if an alarm is throttled based on its notification history.
     *
     * @param alarm       The alarm to check
     * @param currentTime Current timestamp
     * @return true if the alarm is throttled, false otherwise
     */
    private boolean isThrottled(Alarm alarm, long currentTime) {
        if (alarm.getThrottleWindowMinutes() == null || alarm.getThrottleWindowMinutes() <= 0) {
            return false; // No throttling configured
        }

        String alarmId = alarm.getId();
        List<Long> history = notificationHistory.computeIfAbsent(alarmId, k -> new ArrayList<>());

        // Clean up old entries outside the throttle window
        long throttleWindowMs = alarm.getThrottleWindowMinutes() * 60 * 1000L;
        long windowStart = currentTime - throttleWindowMs;
        history.removeIf(timestamp -> timestamp < windowStart);

        // Check if we've exceeded the max notifications per window
        int maxNotifications = alarm.getMaxNotificationsPerWindow() != null ?
                alarm.getMaxNotificationsPerWindow() : 1;

        return history.size() >= maxNotifications;
    }

    /**
     * Add an alarm to grouped notifications.
     *
     * @param alarm       The alarm to add
     * @param currentTime Current timestamp
     */
    private void addToGroupedNotifications(Alarm alarm, long currentTime) {
        String groupingKey = alarm.getGroupingKey();
        List<GroupedAlarmNotification> groupedAlarms = pendingGroupedNotifications
                .computeIfAbsent(groupingKey, k -> new ArrayList<>());

        groupedAlarms.add(new GroupedAlarmNotification(alarm, currentTime));
        logger.debug("Added alarm {} to grouping key {}", alarm.getName(), groupingKey);
    }

    /**
     * Send notification immediately (no grouping).
     *
     * @param alarm       The alarm to send notification for
     * @param currentTime Current timestamp
     */
    private void sendNotificationImmediately(Alarm alarm, long currentTime) {
        // Record the notification in throttling history
        recordNotification(alarm, currentTime);

        // Send notifications to all channels
        for (NotificationChannel channel : alarm.getNotificationChannels()) {
            try {
                sendNotification(alarm, channel);
            } catch (Exception e) {
                logger.error("Failed to send notification for alarm {} via {}: {}",
                        alarm.getName(), channel.getType(), e.getMessage());
            }
        }
    }

    /**
     * Record a notification in the throttling history.
     *
     * @param alarm     The alarm
     * @param timestamp The notification timestamp
     */
    private void recordNotification(Alarm alarm, long timestamp) {
        String alarmId = alarm.getId();
        List<Long> history = notificationHistory.computeIfAbsent(alarmId, k -> new ArrayList<>());
        history.add(timestamp);
    }

    /**
     * Send a notification via the specified channel.
     *
     * @param alarm   The alarm that was triggered
     * @param channel The notification channel
     */
    private void sendNotification(Alarm alarm, NotificationChannel channel) {
        String message = String.format(
                "ALARM TRIGGERED: %s\n" +
                        "Description: %s\n" +
                        "Query: %s\n" +
                        "Condition: %s\n" +
                        "Threshold: %d\n" +
                        "Time Window: %d minutes\n" +
                        "Time: %s",
                alarm.getName(),
                alarm.getDescription(),
                alarm.getQuery(),
                alarm.getCondition(),
                alarm.getThreshold(),
                alarm.getTimeWindowMinutes(),
                new java.util.Date()
        );

        switch (channel.getType().toUpperCase()) {
            case "EMAIL":
                // TODO: Implement email notification
                logger.info("EMAIL notification to {}: {}", channel.getDestination(), message);
                break;
            case "SLACK":
                // TODO: Implement Slack notification
                logger.info("SLACK notification to {}: {}", channel.getDestination(), message);
                break;
            case "WEBHOOK":
                // TODO: Implement webhook notification
                logger.info("WEBHOOK notification to {}: {}", channel.getDestination(), message);
                break;
            case "PAGERDUTY":
                // Send notification to PagerDuty
                boolean success = pagerDutyService.sendAlert(alarm, channel.getDestination(), message);
                if (success) {
                    logger.info("PagerDuty notification sent successfully to integration key: {}", channel.getDestination());
                } else {
                    logger.error("Failed to send PagerDuty notification for alarm: {}", alarm.getName());
                }
                break;
            case "OPSGENIE":
                // Send notification to OpsGenie
                boolean opsGenieSuccess = opsGenieService.sendAlert(alarm, channel.getDestination(), message);
                if (opsGenieSuccess) {
                    logger.info("OpsGenie notification sent successfully to API key: {}", channel.getDestination());
                } else {
                    logger.error("Failed to send OpsGenie notification for alarm: {}", alarm.getName());
                }
                break;
            default:
                logger.warn("Unsupported notification channel type: {}", channel.getType());
        }
    }

    /**
     * Validate an alarm configuration.
     *
     * @param alarm The alarm to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateAlarm(Alarm alarm) {
        if (alarm.getName() == null || alarm.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Alarm name is required");
        }

        if (alarm.getQuery() == null || alarm.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Alarm query is required");
        }

        if (alarm.getCondition() == null || alarm.getCondition().trim().isEmpty()) {
            throw new IllegalArgumentException("Alarm condition is required");
        }

        if (alarm.getThreshold() == null || alarm.getThreshold() < 0) {
            throw new IllegalArgumentException("Alarm threshold must be a non-negative number");
        }

        if (alarm.getTimeWindowMinutes() == null || alarm.getTimeWindowMinutes() <= 0) {
            throw new IllegalArgumentException("Alarm time window must be a positive number");
        }
    }

    /**
     * Get alarm statistics.
     *
     * @return Map containing alarm statistics
     */
    public java.util.Map<String, Object> getAlarmStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalAlarms", alarmRepository.count());
        stats.put("enabledAlarms", alarmRepository.countEnabled());
        stats.put("disabledAlarms", alarmRepository.count() - alarmRepository.countEnabled());
        return stats;
    }

    /**
         * Represents a grouped alarm notification.
         */
        private record GroupedAlarmNotification(Alarm alarm, long triggeredAt) {
    }
}
