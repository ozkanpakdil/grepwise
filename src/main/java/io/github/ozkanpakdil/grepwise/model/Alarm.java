package io.github.ozkanpakdil.grepwise.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents an alarm configuration for log monitoring.
 */
public class Alarm {
    private String id;
    private String name;
    private String description;
    private String query;
    private String condition;
    private Integer threshold;
    private Integer timeWindowMinutes;
    private Boolean enabled = true;
    private long createdAt;
    private long updatedAt;
    private List<NotificationChannel> notificationChannels;

    // Throttling configuration
    private Integer throttleWindowMinutes = 60; // Default: 1 hour throttle window
    private Integer maxNotificationsPerWindow = 1; // Default: max 1 notification per throttle window

    // Grouping configuration
    private String groupingKey; // Key to group similar alarms (optional)
    private Integer groupingWindowMinutes = 5; // Default: 5 minutes grouping window

    // Constructors
    public Alarm() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Alarm(String name, String description, String query, String condition, 
                 Integer threshold, Integer timeWindowMinutes, Boolean enabled) {
        this();
        this.name = name;
        this.description = description;
        this.query = query;
        this.condition = condition;
        this.threshold = threshold;
        this.timeWindowMinutes = timeWindowMinutes;
        this.enabled = enabled;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public Integer getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(Integer timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<NotificationChannel> getNotificationChannels() {
        return notificationChannels;
    }

    public void setNotificationChannels(List<NotificationChannel> notificationChannels) {
        this.notificationChannels = notificationChannels;
    }

    public Integer getThrottleWindowMinutes() {
        return throttleWindowMinutes;
    }

    public void setThrottleWindowMinutes(Integer throttleWindowMinutes) {
        this.throttleWindowMinutes = throttleWindowMinutes;
    }

    public Integer getMaxNotificationsPerWindow() {
        return maxNotificationsPerWindow;
    }

    public void setMaxNotificationsPerWindow(Integer maxNotificationsPerWindow) {
        this.maxNotificationsPerWindow = maxNotificationsPerWindow;
    }

    public String getGroupingKey() {
        return groupingKey;
    }

    public void setGroupingKey(String groupingKey) {
        this.groupingKey = groupingKey;
    }

    public Integer getGroupingWindowMinutes() {
        return groupingWindowMinutes;
    }

    public void setGroupingWindowMinutes(Integer groupingWindowMinutes) {
        this.groupingWindowMinutes = groupingWindowMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alarm alarm = (Alarm) o;
        return createdAt == alarm.createdAt &&
                updatedAt == alarm.updatedAt &&
                Objects.equals(id, alarm.id) &&
                Objects.equals(name, alarm.name) &&
                Objects.equals(description, alarm.description) &&
                Objects.equals(query, alarm.query) &&
                Objects.equals(condition, alarm.condition) &&
                Objects.equals(threshold, alarm.threshold) &&
                Objects.equals(timeWindowMinutes, alarm.timeWindowMinutes) &&
                Objects.equals(enabled, alarm.enabled) &&
                Objects.equals(notificationChannels, alarm.notificationChannels) &&
                Objects.equals(throttleWindowMinutes, alarm.throttleWindowMinutes) &&
                Objects.equals(maxNotificationsPerWindow, alarm.maxNotificationsPerWindow) &&
                Objects.equals(groupingKey, alarm.groupingKey) &&
                Objects.equals(groupingWindowMinutes, alarm.groupingWindowMinutes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, query, condition, threshold, 
                timeWindowMinutes, enabled, createdAt, updatedAt, notificationChannels,
                throttleWindowMinutes, maxNotificationsPerWindow, groupingKey, groupingWindowMinutes);
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", query='" + query + '\'' +
                ", condition='" + condition + '\'' +
                ", threshold=" + threshold +
                ", timeWindowMinutes=" + timeWindowMinutes +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", notificationChannels=" + notificationChannels +
                ", throttleWindowMinutes=" + throttleWindowMinutes +
                ", maxNotificationsPerWindow=" + maxNotificationsPerWindow +
                ", groupingKey='" + groupingKey + '\'' +
                ", groupingWindowMinutes=" + groupingWindowMinutes +
                '}';
    }
}
