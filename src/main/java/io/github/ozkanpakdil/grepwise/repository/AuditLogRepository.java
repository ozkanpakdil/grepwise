package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.AuditLog;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving audit log information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class AuditLogRepository {
    private final Map<String, AuditLog> auditLogs = new ConcurrentHashMap<>();

    /**
     * Save an audit log.
     *
     * @param auditLog The audit log to save
     * @return The saved audit log with a generated ID
     */
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null || auditLog.getId().isEmpty()) {
            auditLog.setId(UUID.randomUUID().toString());
        }

        // Set timestamp if not already set
        if (auditLog.getTimestamp() == 0) {
            auditLog.setTimestamp(System.currentTimeMillis());
        }

        auditLogs.put(auditLog.getId(), auditLog);
        return auditLog;
    }

    /**
     * Find an audit log by ID.
     *
     * @param id The ID of the audit log to find
     * @return The audit log, or null if not found
     */
    public AuditLog findById(String id) {
        return auditLogs.get(id);
    }

    /**
     * Find all audit logs.
     *
     * @return A list of all audit logs
     */
    public List<AuditLog> findAll() {
        return new ArrayList<>(auditLogs.values());
    }

    /**
     * Find audit logs with pagination.
     *
     * @param page The page number (0-based)
     * @param size The page size
     * @return A list of audit logs for the specified page
     */
    public List<AuditLog> findAll(int page, int size) {
        return auditLogs.values().stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by user ID.
     *
     * @param userId The user ID to filter by
     * @return A list of audit logs for the specified user
     */
    public List<AuditLog> findByUserId(String userId) {
        return auditLogs.values().stream()
                .filter(log -> userId.equals(log.getUserId()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by user ID with pagination.
     *
     * @param userId The user ID to filter by
     * @param page   The page number (0-based)
     * @param size   The page size
     * @return A list of audit logs for the specified user and page
     */
    public List<AuditLog> findByUserId(String userId, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> userId.equals(log.getUserId()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by username.
     *
     * @param username The username to filter by
     * @return A list of audit logs for the specified username
     */
    public List<AuditLog> findByUsername(String username) {
        return auditLogs.values().stream()
                .filter(log -> username.equals(log.getUsername()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by username with pagination.
     *
     * @param username The username to filter by
     * @param page     The page number (0-based)
     * @param size     The page size
     * @return A list of audit logs for the specified username and page
     */
    public List<AuditLog> findByUsername(String username, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> username.equals(log.getUsername()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by category.
     *
     * @param category The category to filter by
     * @return A list of audit logs for the specified category
     */
    public List<AuditLog> findByCategory(String category) {
        return auditLogs.values().stream()
                .filter(log -> category.equals(log.getCategory()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by category with pagination.
     *
     * @param category The category to filter by
     * @param page     The page number (0-based)
     * @param size     The page size
     * @return A list of audit logs for the specified category and page
     */
    public List<AuditLog> findByCategory(String category, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> category.equals(log.getCategory()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by action.
     *
     * @param action The action to filter by
     * @return A list of audit logs for the specified action
     */
    public List<AuditLog> findByAction(String action) {
        return auditLogs.values().stream()
                .filter(log -> action.equals(log.getAction()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by action with pagination.
     *
     * @param action The action to filter by
     * @param page   The page number (0-based)
     * @param size   The page size
     * @return A list of audit logs for the specified action and page
     */
    public List<AuditLog> findByAction(String action, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> action.equals(log.getAction()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by status.
     *
     * @param status The status to filter by
     * @return A list of audit logs for the specified status
     */
    public List<AuditLog> findByStatus(String status) {
        return auditLogs.values().stream()
                .filter(log -> status.equals(log.getStatus()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by status with pagination.
     *
     * @param status The status to filter by
     * @param page   The page number (0-based)
     * @param size   The page size
     * @return A list of audit logs for the specified status and page
     */
    public List<AuditLog> findByStatus(String status, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> status.equals(log.getStatus()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by target ID.
     *
     * @param targetId The target ID to filter by
     * @return A list of audit logs for the specified target ID
     */
    public List<AuditLog> findByTargetId(String targetId) {
        return auditLogs.values().stream()
                .filter(log -> targetId.equals(log.getTargetId()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by target ID with pagination.
     *
     * @param targetId The target ID to filter by
     * @param page     The page number (0-based)
     * @param size     The page size
     * @return A list of audit logs for the specified target ID and page
     */
    public List<AuditLog> findByTargetId(String targetId, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> targetId.equals(log.getTargetId()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by target type.
     *
     * @param targetType The target type to filter by
     * @return A list of audit logs for the specified target type
     */
    public List<AuditLog> findByTargetType(String targetType) {
        return auditLogs.values().stream()
                .filter(log -> targetType.equals(log.getTargetType()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by target type with pagination.
     *
     * @param targetType The target type to filter by
     * @param page       The page number (0-based)
     * @param size       The page size
     * @return A list of audit logs for the specified target type and page
     */
    public List<AuditLog> findByTargetType(String targetType, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> targetType.equals(log.getTargetType()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by time range.
     *
     * @param startTime The start time (inclusive)
     * @param endTime   The end time (inclusive)
     * @return A list of audit logs within the specified time range
     */
    public List<AuditLog> findByTimestampBetween(long startTime, long endTime) {
        return auditLogs.values().stream()
                .filter(log -> log.getTimestamp() >= startTime && log.getTimestamp() <= endTime)
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by time range with pagination.
     *
     * @param startTime The start time (inclusive)
     * @param endTime   The end time (inclusive)
     * @param page      The page number (0-based)
     * @param size      The page size
     * @return A list of audit logs within the specified time range and page
     */
    public List<AuditLog> findByTimestampBetween(long startTime, long endTime, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> log.getTimestamp() >= startTime && log.getTimestamp() <= endTime)
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by description containing text.
     *
     * @param text The text to search for
     * @return A list of audit logs with descriptions containing the specified text
     */
    public List<AuditLog> findByDescriptionContaining(String text) {
        return auditLogs.values().stream()
                .filter(log -> log.getDescription() != null &&
                        log.getDescription().toLowerCase().contains(text.toLowerCase()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs by description containing text with pagination.
     *
     * @param text The text to search for
     * @param page The page number (0-based)
     * @param size The page size
     * @return A list of audit logs with descriptions containing the specified text and page
     */
    public List<AuditLog> findByDescriptionContaining(String text, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> log.getDescription() != null &&
                        log.getDescription().toLowerCase().contains(text.toLowerCase()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs with complex filtering.
     *
     * @param filters Map of filter criteria (key = field name, value = filter value)
     * @return A list of audit logs matching the specified filters
     */
    public List<AuditLog> findWithFilters(Map<String, Object> filters) {
        return auditLogs.values().stream()
                .filter(log -> matchesFilters(log, filters))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find audit logs with complex filtering and pagination.
     *
     * @param filters Map of filter criteria (key = field name, value = filter value)
     * @param page    The page number (0-based)
     * @param size    The page size
     * @return A list of audit logs matching the specified filters and page
     */
    public List<AuditLog> findWithFilters(Map<String, Object> filters, int page, int size) {
        return auditLogs.values().stream()
                .filter(log -> matchesFilters(log, filters))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * Check if an audit log matches the specified filters.
     *
     * @param log     The audit log to check
     * @param filters Map of filter criteria (key = field name, value = filter value)
     * @return true if the audit log matches all filters, false otherwise
     */
    private boolean matchesFilters(AuditLog log, Map<String, Object> filters) {
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            switch (key) {
                case "userId":
                    if (!value.equals(log.getUserId())) {
                        return false;
                    }
                    break;
                case "username":
                    if (!value.equals(log.getUsername())) {
                        return false;
                    }
                    break;
                case "category":
                    if (!value.equals(log.getCategory())) {
                        return false;
                    }
                    break;
                case "action":
                    if (!value.equals(log.getAction())) {
                        return false;
                    }
                    break;
                case "status":
                    if (!value.equals(log.getStatus())) {
                        return false;
                    }
                    break;
                case "targetId":
                    if (!value.equals(log.getTargetId())) {
                        return false;
                    }
                    break;
                case "targetType":
                    if (!value.equals(log.getTargetType())) {
                        return false;
                    }
                    break;
                case "startTime":
                    if (log.getTimestamp() < (Long) value) {
                        return false;
                    }
                    break;
                case "endTime":
                    if (log.getTimestamp() > (Long) value) {
                        return false;
                    }
                    break;
                case "searchText":
                    if (log.getDescription() == null ||
                            !log.getDescription().toLowerCase().contains(((String) value).toLowerCase())) {
                        return false;
                    }
                    break;
            }
        }

        return true;
    }

    /**
     * Get distinct categories.
     *
     * @return List of distinct categories
     */
    public List<String> findDistinctCategories() {
        return auditLogs.values().stream()
                .map(AuditLog::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get distinct actions.
     *
     * @return List of distinct actions
     */
    public List<String> findDistinctActions() {
        return auditLogs.values().stream()
                .map(AuditLog::getAction)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get distinct target types.
     *
     * @return List of distinct target types
     */
    public List<String> findDistinctTargetTypes() {
        return auditLogs.values().stream()
                .map(AuditLog::getTargetType)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Count audit logs by category.
     *
     * @return Map of category counts (key = category, value = count)
     */
    public Map<String, Long> countByCategory() {
        return auditLogs.values().stream()
                .filter(log -> log.getCategory() != null)
                .collect(Collectors.groupingBy(AuditLog::getCategory, Collectors.counting()));
    }

    /**
     * Count audit logs by action.
     *
     * @return Map of action counts (key = action, value = count)
     */
    public Map<String, Long> countByAction() {
        return auditLogs.values().stream()
                .filter(log -> log.getAction() != null)
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
    }

    /**
     * Count audit logs by status.
     *
     * @return Map of status counts (key = status, value = count)
     */
    public Map<String, Long> countByStatus() {
        return auditLogs.values().stream()
                .filter(log -> log.getStatus() != null)
                .collect(Collectors.groupingBy(AuditLog::getStatus, Collectors.counting()));
    }

    /**
     * Get the total number of audit logs.
     *
     * @return The total number of audit logs
     */
    public int count() {
        return auditLogs.size();
    }

    /**
     * Delete an audit log by ID.
     *
     * @param id The ID of the audit log to delete
     * @return true if the audit log was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return auditLogs.remove(id) != null;
    }

    /**
     * Delete all audit logs.
     */
    public void deleteAll() {
        auditLogs.clear();
    }
}