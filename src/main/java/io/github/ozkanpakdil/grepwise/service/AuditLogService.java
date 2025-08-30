package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.AuditLog;
import io.github.ozkanpakdil.grepwise.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for audit logging functionality.
 * Provides methods for creating and retrieving audit logs.
 */
@Service
public class AuditLogService {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Create an audit log entry.
     *
     * @param category    The category of the action
     * @param action      The action performed
     * @param status      The status of the action
     * @param description The description of the action
     * @param targetId    The ID of the target object
     * @param targetType  The type of the target object
     * @param details     Additional details about the action
     * @return The created audit log
     */
    public AuditLog createAuditLog(String category, String action, String status,
                                   String description, String targetId, String targetType,
                                   Map<String, String> details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setTimestamp(System.currentTimeMillis());

        // Get current user information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof io.github.ozkanpakdil.grepwise.model.User user) {
                auditLog.setUserId(user.getId());
                auditLog.setUsername(user.getUsername());
            } else {
                auditLog.setUsername(authentication.getName());
            }
        } else {
            auditLog.setUsername("anonymous");
        }

        // Set IP address to unknown since we're not using HttpServletRequest
        auditLog.setIpAddress("unknown");

        auditLog.setCategory(category);
        auditLog.setAction(action);
        auditLog.setStatus(status);
        auditLog.setDescription(description);
        auditLog.setTargetId(targetId);
        auditLog.setTargetType(targetType);
        auditLog.setDetails(details != null ? details : new HashMap<>());

        return auditLogRepository.save(auditLog);
    }

    /**
     * Create an audit log entry for authentication events.
     *
     * @param action      The authentication action (LOGIN, LOGOUT, TOKEN_REFRESH, etc.)
     * @param status      The status of the action (SUCCESS, FAILURE)
     * @param username    The username
     * @param description The description of the action
     * @return The created audit log
     */
    public AuditLog createAuthAuditLog(String action, String status, String username, String description) {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID().toString());
        auditLog.setTimestamp(System.currentTimeMillis());
        auditLog.setUsername(username);
        auditLog.setIpAddress("unknown");
        auditLog.setCategory("AUTH");
        auditLog.setAction(action);
        auditLog.setStatus(status);
        auditLog.setDescription(description);
        auditLog.setTargetType("USER");

        return auditLogRepository.save(auditLog);
    }

    /**
     * Create an audit log entry for user management events.
     *
     * @param action         The user management action (CREATE, UPDATE, DELETE, etc.)
     * @param status         The status of the action (SUCCESS, FAILURE)
     * @param targetUserId   The ID of the target user
     * @param targetUsername The username of the target user
     * @param description    The description of the action
     * @param details        Additional details about the action
     * @return The created audit log
     */
    public AuditLog createUserAuditLog(String action, String status, String targetUserId,
                                       String targetUsername, String description, Map<String, String> details) {
        Map<String, String> logDetails = details != null ? details : new HashMap<>();
        if (targetUsername != null) {
            logDetails.put("targetUsername", targetUsername);
        }

        return createAuditLog("USER_MGMT", action, status, description, targetUserId, "USER", logDetails);
    }

    /**
     * Create an audit log entry for dashboard events.
     *
     * @param action        The dashboard action (CREATE, UPDATE, DELETE, SHARE, etc.)
     * @param status        The status of the action (SUCCESS, FAILURE)
     * @param dashboardId   The ID of the dashboard
     * @param dashboardName The name of the dashboard
     * @param description   The description of the action
     * @param details       Additional details about the action
     * @return The created audit log
     */
    public AuditLog createDashboardAuditLog(String action, String status, String dashboardId,
                                            String dashboardName, String description, Map<String, String> details) {
        Map<String, String> logDetails = details != null ? details : new HashMap<>();
        if (dashboardName != null) {
            logDetails.put("dashboardName", dashboardName);
        }

        return createAuditLog("DASHBOARD", action, status, description, dashboardId, "DASHBOARD", logDetails);
    }

    /**
     * Create an audit log entry for alarm events.
     *
     * @param action      The alarm action (CREATE, UPDATE, DELETE, ACKNOWLEDGE, etc.)
     * @param status      The status of the action (SUCCESS, FAILURE)
     * @param alarmId     The ID of the alarm
     * @param alarmName   The name of the alarm
     * @param description The description of the action
     * @param details     Additional details about the action
     * @return The created audit log
     */
    public AuditLog createAlarmAuditLog(String action, String status, String alarmId,
                                        String alarmName, String description, Map<String, String> details) {
        Map<String, String> logDetails = details != null ? details : new HashMap<>();
        if (alarmName != null) {
            logDetails.put("alarmName", alarmName);
        }

        return createAuditLog("ALARM", action, status, description, alarmId, "ALARM", logDetails);
    }

    /**
     * Create an audit log entry for settings events.
     *
     * @param action      The settings action (UPDATE, etc.)
     * @param status      The status of the action (SUCCESS, FAILURE)
     * @param settingName The name of the setting
     * @param description The description of the action
     * @param details     Additional details about the action
     * @return The created audit log
     */
    public AuditLog createSettingsAuditLog(String action, String status, String settingName,
                                           String description, Map<String, String> details) {
        return createAuditLog("SETTINGS", action, status, description, settingName, "SETTING", details);
    }

    /**
     * Create an audit log entry for data access events.
     *
     * @param action      The data access action (EXPORT, BULK_OPERATION, etc.)
     * @param status      The status of the action (SUCCESS, FAILURE)
     * @param dataType    The type of data accessed
     * @param description The description of the action
     * @param details     Additional details about the action
     * @return The created audit log
     */
    public AuditLog createDataAccessAuditLog(String action, String status, String dataType,
                                             String description, Map<String, String> details) {
        return createAuditLog("DATA_ACCESS", action, status, description, null, dataType, details);
    }

    /**
     * Get all audit logs.
     *
     * @return List of all audit logs
     */
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    /**
     * Get audit logs with pagination.
     *
     * @param page The page number (0-based)
     * @param size The page size
     * @return List of audit logs for the specified page
     */
    public List<AuditLog> getAuditLogs(int page, int size) {
        return auditLogRepository.findAll(page, size);
    }

    /**
     * Get audit logs by user ID.
     *
     * @param userId The user ID
     * @param page   The page number (0-based)
     * @param size   The page size
     * @return List of audit logs for the specified user and page
     */
    public List<AuditLog> getAuditLogsByUserId(String userId, int page, int size) {
        return auditLogRepository.findByUserId(userId, page, size);
    }

    /**
     * Get audit logs by username.
     *
     * @param username The username
     * @param page     The page number (0-based)
     * @param size     The page size
     * @return List of audit logs for the specified username and page
     */
    public List<AuditLog> getAuditLogsByUsername(String username, int page, int size) {
        return auditLogRepository.findByUsername(username, page, size);
    }

    /**
     * Get audit logs by category.
     *
     * @param category The category
     * @param page     The page number (0-based)
     * @param size     The page size
     * @return List of audit logs for the specified category and page
     */
    public List<AuditLog> getAuditLogsByCategory(String category, int page, int size) {
        return auditLogRepository.findByCategory(category, page, size);
    }

    /**
     * Get audit logs by action.
     *
     * @param action The action
     * @param page   The page number (0-based)
     * @param size   The page size
     * @return List of audit logs for the specified action and page
     */
    public List<AuditLog> getAuditLogsByAction(String action, int page, int size) {
        return auditLogRepository.findByAction(action, page, size);
    }

    /**
     * Get audit logs by time range.
     *
     * @param startTime The start time (inclusive)
     * @param endTime   The end time (inclusive)
     * @param page      The page number (0-based)
     * @param size      The page size
     * @return List of audit logs within the specified time range and page
     */
    public List<AuditLog> getAuditLogsByTimeRange(long startTime, long endTime, int page, int size) {
        return auditLogRepository.findByTimestampBetween(startTime, endTime, page, size);
    }

    /**
     * Get audit logs with complex filtering.
     *
     * @param filters Map of filter criteria (key = field name, value = filter value)
     * @param page    The page number (0-based)
     * @param size    The page size
     * @return List of audit logs matching the specified filters and page
     */
    public List<AuditLog> getAuditLogsWithFilters(Map<String, Object> filters, int page, int size) {
        return auditLogRepository.findWithFilters(filters, page, size);
    }

    /**
     * Get distinct categories.
     *
     * @return List of distinct categories
     */
    public List<String> getDistinctCategories() {
        return auditLogRepository.findDistinctCategories();
    }

    /**
     * Get distinct actions.
     *
     * @return List of distinct actions
     */
    public List<String> getDistinctActions() {
        return auditLogRepository.findDistinctActions();
    }

    /**
     * Get distinct target types.
     *
     * @return List of distinct target types
     */
    public List<String> getDistinctTargetTypes() {
        return auditLogRepository.findDistinctTargetTypes();
    }

    /**
     * Count audit logs by category.
     *
     * @return Map of category counts (key = category, value = count)
     */
    public Map<String, Long> countByCategory() {
        return auditLogRepository.countByCategory();
    }

    /**
     * Count audit logs by action.
     *
     * @return Map of action counts (key = action, value = count)
     */
    public Map<String, Long> countByAction() {
        return auditLogRepository.countByAction();
    }

    /**
     * Count audit logs by status.
     *
     * @return Map of status counts (key = status, value = count)
     */
    public Map<String, Long> countByStatus() {
        return auditLogRepository.countByStatus();
    }

    /**
     * Get the total number of audit logs.
     *
     * @return The total number of audit logs
     */
    public int count() {
        return auditLogRepository.count();
    }

    /**
     * Delete an audit log by ID.
     *
     * @param id The ID of the audit log to delete
     * @return true if the audit log was deleted, false otherwise
     */
    public boolean deleteAuditLog(String id) {
        return auditLogRepository.deleteById(id);
    }

    /**
     * Delete all audit logs.
     */
    public void deleteAllAuditLogs() {
        auditLogRepository.deleteAll();
    }
}