package io.github.ozkanpakdil.grepwise.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model class for audit logs.
 * Stores information about user actions in the system for security and compliance purposes.
 */
public class AuditLog {
    
    /**
     * Unique identifier for the audit log entry
     */
    private String id;
    
    /**
     * Timestamp when the action occurred (epoch milliseconds)
     */
    private long timestamp;
    
    /**
     * User ID of the user who performed the action
     */
    private String userId;
    
    /**
     * Username of the user who performed the action
     */
    private String username;
    
    /**
     * IP address from which the action was performed
     */
    private String ipAddress;
    
    /**
     * Category of the action (e.g., AUTH, USER_MGMT, DASHBOARD, ALARM, SETTINGS)
     */
    private String category;
    
    /**
     * Type of action performed (e.g., LOGIN, CREATE, UPDATE, DELETE)
     */
    private String action;
    
    /**
     * Status of the action (e.g., SUCCESS, FAILURE)
     */
    private String status;
    
    /**
     * Description of the action
     */
    private String description;
    
    /**
     * Target of the action (e.g., user ID, dashboard ID)
     */
    private String targetId;
    
    /**
     * Type of the target (e.g., USER, DASHBOARD, ALARM)
     */
    private String targetType;
    
    /**
     * Additional details about the action (stored as key-value pairs)
     */
    private Map<String, String> details;

    /**
     * Default constructor
     */
    public AuditLog() {
        this.timestamp = System.currentTimeMillis();
        this.details = new HashMap<>();
    }

    /**
     * Constructor with all fields
     */
    public AuditLog(String id, long timestamp, String userId, String username, String ipAddress, 
                   String category, String action, String status, String description, 
                   String targetId, String targetType, Map<String, String> details) {
        this.id = id;
        this.timestamp = timestamp;
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.category = category;
        this.action = action;
        this.status = status;
        this.description = description;
        this.targetId = targetId;
        this.targetType = targetType;
        this.details = details != null ? details : new HashMap<>();
    }

    // Getters and Setters
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }
    
    /**
     * Add a detail to the details map
     */
    public void addDetail(String key, String value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }
        this.details.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return timestamp == auditLog.timestamp &&
                Objects.equals(id, auditLog.id) &&
                Objects.equals(userId, auditLog.userId) &&
                Objects.equals(username, auditLog.username) &&
                Objects.equals(ipAddress, auditLog.ipAddress) &&
                Objects.equals(category, auditLog.category) &&
                Objects.equals(action, auditLog.action) &&
                Objects.equals(status, auditLog.status) &&
                Objects.equals(description, auditLog.description) &&
                Objects.equals(targetId, auditLog.targetId) &&
                Objects.equals(targetType, auditLog.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, userId, username, ipAddress, category, 
                           action, status, description, targetId, targetType);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", category='" + category + '\'' +
                ", action='" + action + '\'' +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", targetId='" + targetId + '\'' +
                ", targetType='" + targetType + '\'' +
                ", details=" + details +
                '}';
    }
}