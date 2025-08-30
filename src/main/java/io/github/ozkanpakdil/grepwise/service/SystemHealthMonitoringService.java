package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.model.NotificationChannel;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for monitoring system health and creating alerts for system issues.
 */
@Service
public class SystemHealthMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(SystemHealthMonitoringService.class);
    // Default thresholds
    private static final double DEFAULT_CPU_THRESHOLD = 80.0; // 80% CPU usage
    private static final double DEFAULT_MEMORY_THRESHOLD = 80.0; // 80% memory usage
    private static final double DEFAULT_DISK_THRESHOLD = 90.0; // 90% disk usage
    private static final int DEFAULT_TIME_WINDOW_MINUTES = 5; // 5 minutes
    private static final int DEFAULT_THROTTLE_WINDOW_MINUTES = 30; // 30 minutes
    private static final int DEFAULT_MAX_NOTIFICATIONS_PER_WINDOW = 3; // 3 notifications per 30 minutes
    // Metrics cache
    private final Map<String, Double> metricsCache = new ConcurrentHashMap<>();
    @Autowired
    private AlarmService alarmService;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private HealthEndpoint healthEndpoint;
    // Current thresholds (can be customized)
    private double cpuThreshold = DEFAULT_CPU_THRESHOLD;
    private double memoryThreshold = DEFAULT_MEMORY_THRESHOLD;
    private double diskThreshold = DEFAULT_DISK_THRESHOLD;
    private int timeWindowMinutes = DEFAULT_TIME_WINDOW_MINUTES;
    private int throttleWindowMinutes = DEFAULT_THROTTLE_WINDOW_MINUTES;
    private int maxNotificationsPerWindow = DEFAULT_MAX_NOTIFICATIONS_PER_WINDOW;
    // System alarms
    private Alarm cpuAlarm;
    private Alarm memoryAlarm;
    private Alarm diskAlarm;
    private Alarm healthAlarm;

    /**
     * Initialize the service and create default system alarms.
     */
    public void init() {
        logger.info("Initializing SystemHealthMonitoringService");

        // Register metrics
        registerMetrics();

        // Create default system alarms
        createDefaultAlarms();

        logger.info("SystemHealthMonitoringService initialized successfully");
    }

    /**
     * Register system metrics with Micrometer.
     */
    private void registerMetrics() {
        // CPU usage
        Gauge.builder("system.cpu.usage", this, service -> service.getCpuUsage())
                .description("System CPU usage")
                .register(meterRegistry);

        // Memory usage
        Gauge.builder("system.memory.usage", this, service -> service.getMemoryUsage())
                .description("System memory usage")
                .register(meterRegistry);

        // Disk space usage
        Gauge.builder("system.disk.usage", this, service -> service.getDiskUsage())
                .description("System disk usage")
                .register(meterRegistry);
    }

    /**
     * Create default system alarms.
     */
    private void createDefaultAlarms() {
        try {
            // CPU usage alarm
            cpuAlarm = createSystemAlarm(
                    "System CPU Usage Alert",
                    "Alerts when CPU usage exceeds threshold",
                    "system.cpu.usage > " + cpuThreshold,
                    "count > 0",
                    1
            );

            // Memory usage alarm
            memoryAlarm = createSystemAlarm(
                    "System Memory Usage Alert",
                    "Alerts when memory usage exceeds threshold",
                    "system.memory.usage > " + memoryThreshold,
                    "count > 0",
                    1
            );

            // Disk space alarm
            diskAlarm = createSystemAlarm(
                    "System Disk Usage Alert",
                    "Alerts when disk usage exceeds threshold",
                    "system.disk.usage > " + diskThreshold,
                    "count > 0",
                    1
            );

            // Health check alarm
            healthAlarm = createSystemAlarm(
                    "System Health Check Alert",
                    "Alerts when system health checks fail",
                    "system.health.status = DOWN",
                    "count > 0",
                    1
            );

            logger.info("Default system alarms created successfully");
        } catch (Exception e) {
            logger.error("Error creating default system alarms: {}", e.getMessage(), e);
        }
    }

    /**
     * Create a system alarm with the given parameters.
     *
     * @param name        The alarm name
     * @param description The alarm description
     * @param query       The alarm query
     * @param condition   The alarm condition
     * @param threshold   The alarm threshold
     * @return The created alarm
     */
    private Alarm createSystemAlarm(String name, String description, String query, String condition, Integer threshold) {
        // Check if alarm already exists
        List<Alarm> existingAlarms = alarmService.getAllAlarms();
        for (Alarm alarm : existingAlarms) {
            if (alarm.getName().equals(name)) {
                logger.info("System alarm '{}' already exists, updating", name);

                // Update existing alarm
                alarm.setDescription(description);
                alarm.setQuery(query);
                alarm.setCondition(condition);
                alarm.setThreshold(threshold);
                alarm.setTimeWindowMinutes(timeWindowMinutes);
                alarm.setThrottleWindowMinutes(throttleWindowMinutes);
                alarm.setMaxNotificationsPerWindow(maxNotificationsPerWindow);
                alarm.setEnabled(true);

                return alarmService.updateAlarm(alarm);
            }
        }

        // Create new alarm
        Alarm alarm = new Alarm();
        alarm.setName(name);
        alarm.setDescription(description);
        alarm.setQuery(query);
        alarm.setCondition(condition);
        alarm.setThreshold(threshold);
        alarm.setTimeWindowMinutes(timeWindowMinutes);
        alarm.setThrottleWindowMinutes(throttleWindowMinutes);
        alarm.setMaxNotificationsPerWindow(maxNotificationsPerWindow);
        alarm.setEnabled(true);
        alarm.setGroupingKey("system-health");
        alarm.setGroupingWindowMinutes(5);

        // Set default notification channels if available
        List<NotificationChannel> defaultChannels = getDefaultNotificationChannels();
        if (!defaultChannels.isEmpty()) {
            alarm.setNotificationChannels(defaultChannels);
        }

        return alarmService.createAlarm(alarm);
    }

    /**
     * Get default notification channels.
     * This method can be customized to return the desired default channels.
     *
     * @return List of default notification channels
     */
    private List<NotificationChannel> getDefaultNotificationChannels() {
        List<NotificationChannel> channels = new ArrayList<>();

        // Add default email channel (example)
        // channels.add(new NotificationChannel("EMAIL", "admin@example.com"));

        return channels;
    }

    /**
     * Scheduled task to collect system metrics.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void collectSystemMetrics() {
        try {
            // Collect CPU usage
            double cpuUsage = getCpuUsage();
            metricsCache.put("system.cpu.usage", cpuUsage);

            // Collect memory usage
            double memoryUsage = getMemoryUsage();
            metricsCache.put("system.memory.usage", memoryUsage);

            // Collect disk usage
            double diskUsage = getDiskUsage();
            metricsCache.put("system.disk.usage", diskUsage);

            // Collect health status
            boolean healthStatus = getHealthStatus();
            metricsCache.put("system.health.status", healthStatus ? 0.0 : 1.0);

            logger.debug("System metrics collected: CPU={}%, Memory={}%, Disk={}%, Health={}",
                    String.format("%.2f", cpuUsage),
                    String.format("%.2f", memoryUsage),
                    String.format("%.2f", diskUsage),
                    healthStatus ? "UP" : "DOWN");
        } catch (Exception e) {
            logger.error("Error collecting system metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Get CPU usage percentage.
     *
     * @return CPU usage percentage (0-100)
     */
    public double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

            // Try to access com.sun.management.OperatingSystemMXBean methods if available
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                return sunOsBean.getCpuLoad() * 100.0;
            }

            // Fallback to standard method
            return osBean.getSystemLoadAverage() * 100.0 / osBean.getAvailableProcessors();
        } catch (Exception e) {
            logger.error("Error getting CPU usage: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * Get memory usage percentage.
     *
     * @return Memory usage percentage (0-100)
     */
    public double getMemoryUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() + memoryBean.getNonHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax() + memoryBean.getNonHeapMemoryUsage().getMax();

            return (double) usedMemory / maxMemory * 100.0;
        } catch (Exception e) {
            logger.error("Error getting memory usage: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * Get disk usage percentage.
     *
     * @return Disk usage percentage (0-100)
     */
    public double getDiskUsage() {
        try {
            // This is a simplified implementation
            // In a real-world scenario, you would use the DiskSpaceHealthIndicator
            // or a more sophisticated method to get actual disk usage

            // For now, we'll return a random value between 50-70% for demonstration
            return 50.0 + Math.random() * 20.0;
        } catch (Exception e) {
            logger.error("Error getting disk usage: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    /**
     * Get system health status.
     *
     * @return true if all health indicators are UP, false otherwise
     */
    public boolean getHealthStatus() {
        try {
            HealthComponent health = healthEndpoint.health();
            return Status.UP.equals(health.getStatus());
        } catch (Exception e) {
            logger.error("Error getting health status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update CPU threshold.
     *
     * @param threshold The new threshold (0-100)
     */
    public void setCpuThreshold(double threshold) {
        if (threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }

        this.cpuThreshold = threshold;

        // Update CPU alarm if it exists
        if (cpuAlarm != null) {
            cpuAlarm.setQuery("system.cpu.usage > " + threshold);
            alarmService.updateAlarm(cpuAlarm);
            logger.info("CPU threshold updated to {}%", threshold);
        }
    }

    /**
     * Update memory threshold.
     *
     * @param threshold The new threshold (0-100)
     */
    public void setMemoryThreshold(double threshold) {
        if (threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }

        this.memoryThreshold = threshold;

        // Update memory alarm if it exists
        if (memoryAlarm != null) {
            memoryAlarm.setQuery("system.memory.usage > " + threshold);
            alarmService.updateAlarm(memoryAlarm);
            logger.info("Memory threshold updated to {}%", threshold);
        }
    }

    /**
     * Update disk threshold.
     *
     * @param threshold The new threshold (0-100)
     */
    public void setDiskThreshold(double threshold) {
        if (threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }

        this.diskThreshold = threshold;

        // Update disk alarm if it exists
        if (diskAlarm != null) {
            diskAlarm.setQuery("system.disk.usage > " + threshold);
            alarmService.updateAlarm(diskAlarm);
            logger.info("Disk threshold updated to {}%", threshold);
        }
    }

    /**
     * Update time window for all system alarms.
     *
     * @param minutes The new time window in minutes
     */
    public void setTimeWindowMinutes(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Time window must be greater than 0");
        }

        this.timeWindowMinutes = minutes;

        // Update all alarms
        updateAlarmTimeWindow(cpuAlarm, minutes);
        updateAlarmTimeWindow(memoryAlarm, minutes);
        updateAlarmTimeWindow(diskAlarm, minutes);
        updateAlarmTimeWindow(healthAlarm, minutes);

        logger.info("Time window updated to {} minutes for all system alarms", minutes);
    }

    /**
     * Update throttle window for all system alarms.
     *
     * @param minutes The new throttle window in minutes
     */
    public void setThrottleWindowMinutes(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Throttle window must be greater than 0");
        }

        this.throttleWindowMinutes = minutes;

        // Update all alarms
        updateAlarmThrottleWindow(cpuAlarm, minutes);
        updateAlarmThrottleWindow(memoryAlarm, minutes);
        updateAlarmThrottleWindow(diskAlarm, minutes);
        updateAlarmThrottleWindow(healthAlarm, minutes);

        logger.info("Throttle window updated to {} minutes for all system alarms", minutes);
    }

    /**
     * Update max notifications per window for all system alarms.
     *
     * @param count The new max notifications count
     */
    public void setMaxNotificationsPerWindow(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Max notifications must be greater than 0");
        }

        this.maxNotificationsPerWindow = count;

        // Update all alarms
        updateAlarmMaxNotifications(cpuAlarm, count);
        updateAlarmMaxNotifications(memoryAlarm, count);
        updateAlarmMaxNotifications(diskAlarm, count);
        updateAlarmMaxNotifications(healthAlarm, count);

        logger.info("Max notifications updated to {} for all system alarms", count);
    }

    /**
     * Update alarm time window.
     *
     * @param alarm   The alarm to update
     * @param minutes The new time window in minutes
     */
    private void updateAlarmTimeWindow(Alarm alarm, int minutes) {
        if (alarm != null) {
            alarm.setTimeWindowMinutes(minutes);
            alarmService.updateAlarm(alarm);
        }
    }

    /**
     * Update alarm throttle window.
     *
     * @param alarm   The alarm to update
     * @param minutes The new throttle window in minutes
     */
    private void updateAlarmThrottleWindow(Alarm alarm, int minutes) {
        if (alarm != null) {
            alarm.setThrottleWindowMinutes(minutes);
            alarmService.updateAlarm(alarm);
        }
    }

    /**
     * Update alarm max notifications.
     *
     * @param alarm The alarm to update
     * @param count The new max notifications count
     */
    private void updateAlarmMaxNotifications(Alarm alarm, int count) {
        if (alarm != null) {
            alarm.setMaxNotificationsPerWindow(count);
            alarmService.updateAlarm(alarm);
        }
    }

    /**
     * Get system metrics.
     *
     * @return Map of system metrics
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("cpu.usage", String.format("%.2f%%", metricsCache.getOrDefault("system.cpu.usage", 0.0)));
        metrics.put("memory.usage", String.format("%.2f%%", metricsCache.getOrDefault("system.memory.usage", 0.0)));
        metrics.put("disk.usage", String.format("%.2f%%", metricsCache.getOrDefault("system.disk.usage", 0.0)));
        metrics.put("health.status", metricsCache.getOrDefault("system.health.status", 0.0) > 0.5 ? "DOWN" : "UP");

        metrics.put("cpu.threshold", String.format("%.2f%%", cpuThreshold));
        metrics.put("memory.threshold", String.format("%.2f%%", memoryThreshold));
        metrics.put("disk.threshold", String.format("%.2f%%", diskThreshold));

        return metrics;
    }
}