package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.AuditLog;
import io.github.ozkanpakdil.grepwise.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for audit log operations.
 * Provides endpoints for retrieving and filtering audit logs.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogController.class);

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Get audit logs with pagination and filtering.
     *
     * @param page       The page number (0-based, default: 0)
     * @param size       The page size (default: 20)
     * @param userId     Filter by user ID (optional)
     * @param username   Filter by username (optional)
     * @param category   Filter by category (optional)
     * @param action     Filter by action (optional)
     * @param status     Filter by status (optional)
     * @param targetId   Filter by target ID (optional)
     * @param targetType Filter by target type (optional)
     * @param startTime  Filter by start time (optional)
     * @param endTime    Filter by end time (optional)
     * @param searchText Search in description (optional)
     * @return List of audit logs matching the criteria
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String searchText) {

        try {
            // Create filters map
            Map<String, Object> filters = new HashMap<>();
            if (userId != null) filters.put("userId", userId);
            if (username != null) filters.put("username", username);
            if (category != null) filters.put("category", category);
            if (action != null) filters.put("action", action);
            if (status != null) filters.put("status", status);
            if (targetId != null) filters.put("targetId", targetId);
            if (targetType != null) filters.put("targetType", targetType);
            if (startTime != null) filters.put("startTime", startTime);
            if (endTime != null) filters.put("endTime", endTime);
            if (searchText != null) filters.put("searchText", searchText);

            List<AuditLog> auditLogs;
            if (filters.isEmpty()) {
                auditLogs = auditLogService.getAuditLogs(page, size);
            } else {
                auditLogs = auditLogService.getAuditLogsWithFilters(filters, page, size);
            }

            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            logger.error("Error retrieving audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific audit log by ID.
     *
     * @param id The ID of the audit log
     * @return The audit log with the specified ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable String id) {
        try {
            AuditLog auditLog = auditLogService.getAllAuditLogs().stream()
                    .filter(log -> id.equals(log.getId()))
                    .findFirst()
                    .orElse(null);

            if (auditLog != null) {
                return ResponseEntity.ok(auditLog);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving audit log with ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get distinct categories.
     *
     * @return List of distinct categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        try {
            List<String> categories = auditLogService.getDistinctCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            logger.error("Error retrieving audit log categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get distinct actions.
     *
     * @return List of distinct actions
     */
    @GetMapping("/actions")
    public ResponseEntity<List<String>> getActions() {
        try {
            List<String> actions = auditLogService.getDistinctActions();
            return ResponseEntity.ok(actions);
        } catch (Exception e) {
            logger.error("Error retrieving audit log actions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get distinct target types.
     *
     * @return List of distinct target types
     */
    @GetMapping("/target-types")
    public ResponseEntity<List<String>> getTargetTypes() {
        try {
            List<String> targetTypes = auditLogService.getDistinctTargetTypes();
            return ResponseEntity.ok(targetTypes);
        } catch (Exception e) {
            logger.error("Error retrieving audit log target types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get audit log counts by category.
     *
     * @return Map of category counts
     */
    @GetMapping("/counts/by-category")
    public ResponseEntity<Map<String, Long>> getCountsByCategory() {
        try {
            Map<String, Long> counts = auditLogService.countByCategory();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            logger.error("Error retrieving audit log counts by category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get audit log counts by action.
     *
     * @return Map of action counts
     */
    @GetMapping("/counts/by-action")
    public ResponseEntity<Map<String, Long>> getCountsByAction() {
        try {
            Map<String, Long> counts = auditLogService.countByAction();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            logger.error("Error retrieving audit log counts by action", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get audit log counts by status.
     *
     * @return Map of status counts
     */
    @GetMapping("/counts/by-status")
    public ResponseEntity<Map<String, Long>> getCountsByStatus() {
        try {
            Map<String, Long> counts = auditLogService.countByStatus();
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            logger.error("Error retrieving audit log counts by status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the total count of audit logs.
     *
     * @return The total count
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getCount() {
        try {
            int count = auditLogService.count();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Error retrieving audit log count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete an audit log by ID.
     * This endpoint should be restricted to administrators.
     *
     * @param id The ID of the audit log to delete
     * @return Success/failure response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuditLog(@PathVariable String id) {
        try {
            boolean deleted = auditLogService.deleteAuditLog(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting audit log with ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete all audit logs.
     * This endpoint should be restricted to administrators.
     *
     * @return Success/failure response
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllAuditLogs() {
        try {
            auditLogService.deleteAllAuditLogs();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting all audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}