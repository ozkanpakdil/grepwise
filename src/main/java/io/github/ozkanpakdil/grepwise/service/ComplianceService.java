package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.AuditLog;
import io.github.ozkanpakdil.grepwise.model.ComplianceReport;
import io.github.ozkanpakdil.grepwise.repository.ComplianceReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating and managing compliance reports.
 * This service uses audit logs to generate structured reports for compliance purposes.
 */
@Service
public class ComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceService.class);

    @Autowired
    private ComplianceReportRepository complianceReportRepository;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Generate a user activity compliance report for a specific time period
     *
     * @param name        the name of the report
     * @param description the description of the report
     * @param startTime   the start time of the period to include in the report (epoch millis)
     * @param endTime     the end time of the period to include in the report (epoch millis)
     * @return the generated compliance report
     */
    public ComplianceReport generateUserActivityReport(String name, String description, long startTime, long endTime) {
        logger.info("Generating user activity compliance report: {}", name);

        // Get the current authenticated user
        String generatedBy = getCurrentUsername();

        // Get audit logs for the specified time period
        List<AuditLog> logs = auditLogService.getAuditLogsByTimeRange(startTime, endTime, 0, Integer.MAX_VALUE);

        // Filter logs related to user activity
        List<AuditLog> userActivityLogs = logs.stream()
                .filter(log -> "USER".equals(log.getCategory()) || "AUTH".equals(log.getCategory()))
                .collect(Collectors.toList());

        // Create a new compliance report
        ComplianceReport report = new ComplianceReport();
        report.setName(name);
        report.setDescription(description);
        report.setGeneratedAt(LocalDateTime.now());
        report.setReportStartDate(LocalDateTime.ofEpochSecond(startTime / 1000, 0, ZoneOffset.UTC));
        report.setReportEndDate(LocalDateTime.ofEpochSecond(endTime / 1000, 0, ZoneOffset.UTC));
        report.setGeneratedBy(generatedBy);
        report.setReportType("USER_ACTIVITY");
        report.setStatus("GENERATED");
        report.setIncludedLogs(userActivityLogs);

        // Generate summary statistics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLogs", userActivityLogs.size());

        // Count logs by action
        Map<String, Long> actionCounts = userActivityLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.put("actionCounts", actionCounts);

        // Count logs by status
        Map<String, Long> statusCounts = userActivityLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getStatus, Collectors.counting()));
        summary.put("statusCounts", statusCounts);

        // Count logs by user
        Map<String, Long> userCounts = userActivityLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getUsername, Collectors.counting()));
        summary.put("userCounts", userCounts);

        report.setSummary(summary);

        // Save the report
        return complianceReportRepository.save(report);
    }

    /**
     * Generate a data access compliance report for a specific time period
     *
     * @param name        the name of the report
     * @param description the description of the report
     * @param startTime   the start time of the period to include in the report (epoch millis)
     * @param endTime     the end time of the period to include in the report (epoch millis)
     * @return the generated compliance report
     */
    public ComplianceReport generateDataAccessReport(String name, String description, long startTime, long endTime) {
        logger.info("Generating data access compliance report: {}", name);

        // Get the current authenticated user
        String generatedBy = getCurrentUsername();

        // Get audit logs for the specified time period
        List<AuditLog> logs = auditLogService.getAuditLogsByTimeRange(startTime, endTime, 0, Integer.MAX_VALUE);

        // Filter logs related to data access
        List<AuditLog> dataAccessLogs = logs.stream()
                .filter(log -> "DATA_ACCESS".equals(log.getCategory()))
                .collect(Collectors.toList());

        // Create a new compliance report
        ComplianceReport report = new ComplianceReport();
        report.setName(name);
        report.setDescription(description);
        report.setGeneratedAt(LocalDateTime.now());
        report.setReportStartDate(LocalDateTime.ofEpochSecond(startTime / 1000, 0, ZoneOffset.UTC));
        report.setReportEndDate(LocalDateTime.ofEpochSecond(endTime / 1000, 0, ZoneOffset.UTC));
        report.setGeneratedBy(generatedBy);
        report.setReportType("DATA_ACCESS");
        report.setStatus("GENERATED");
        report.setIncludedLogs(dataAccessLogs);

        // Generate summary statistics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLogs", dataAccessLogs.size());

        // Count logs by action
        Map<String, Long> actionCounts = dataAccessLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.put("actionCounts", actionCounts);

        // Count logs by status
        Map<String, Long> statusCounts = dataAccessLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getStatus, Collectors.counting()));
        summary.put("statusCounts", statusCounts);

        // Count logs by target type
        Map<String, Long> targetTypeCounts = dataAccessLogs.stream()
                .filter(log -> log.getTargetType() != null)
                .collect(Collectors.groupingBy(AuditLog::getTargetType, Collectors.counting()));
        summary.put("targetTypeCounts", targetTypeCounts);

        report.setSummary(summary);

        // Save the report
        return complianceReportRepository.save(report);
    }

    /**
     * Generate a security compliance report for a specific time period
     *
     * @param name        the name of the report
     * @param description the description of the report
     * @param startTime   the start time of the period to include in the report (epoch millis)
     * @param endTime     the end time of the period to include in the report (epoch millis)
     * @return the generated compliance report
     */
    public ComplianceReport generateSecurityReport(String name, String description, long startTime, long endTime) {
        logger.info("Generating security compliance report: {}", name);

        // Get the current authenticated user
        String generatedBy = getCurrentUsername();

        // Get audit logs for the specified time period
        List<AuditLog> logs = auditLogService.getAuditLogsByTimeRange(startTime, endTime, 0, Integer.MAX_VALUE);

        // Filter logs related to security
        List<AuditLog> securityLogs = logs.stream()
                .filter(log -> "SECURITY".equals(log.getCategory()) || "AUTH".equals(log.getCategory()))
                .collect(Collectors.toList());

        // Create a new compliance report
        ComplianceReport report = new ComplianceReport();
        report.setName(name);
        report.setDescription(description);
        report.setGeneratedAt(LocalDateTime.now());
        report.setReportStartDate(LocalDateTime.ofEpochSecond(startTime / 1000, 0, ZoneOffset.UTC));
        report.setReportEndDate(LocalDateTime.ofEpochSecond(endTime / 1000, 0, ZoneOffset.UTC));
        report.setGeneratedBy(generatedBy);
        report.setReportType("SECURITY");
        report.setStatus("GENERATED");
        report.setIncludedLogs(securityLogs);

        // Generate summary statistics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLogs", securityLogs.size());

        // Count logs by action
        Map<String, Long> actionCounts = securityLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.put("actionCounts", actionCounts);

        // Count logs by status
        Map<String, Long> statusCounts = securityLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getStatus, Collectors.counting()));
        summary.put("statusCounts", statusCounts);

        // Count failed login attempts
        long failedLoginAttempts = securityLogs.stream()
                .filter(log -> "LOGIN".equals(log.getAction()) && "FAILURE".equals(log.getStatus()))
                .count();
        summary.put("failedLoginAttempts", failedLoginAttempts);

        report.setSummary(summary);

        // Save the report
        return complianceReportRepository.save(report);
    }

    /**
     * Generate a system changes compliance report for a specific time period
     *
     * @param name        the name of the report
     * @param description the description of the report
     * @param startTime   the start time of the period to include in the report (epoch millis)
     * @param endTime     the end time of the period to include in the report (epoch millis)
     * @return the generated compliance report
     */
    public ComplianceReport generateSystemChangesReport(String name, String description, long startTime, long endTime) {
        logger.info("Generating system changes compliance report: {}", name);

        // Get the current authenticated user
        String generatedBy = getCurrentUsername();

        // Get audit logs for the specified time period
        List<AuditLog> logs = auditLogService.getAuditLogsByTimeRange(startTime, endTime, 0, Integer.MAX_VALUE);

        // Filter logs related to system changes
        List<AuditLog> systemChangesLogs = logs.stream()
                .filter(log -> "SYSTEM".equals(log.getCategory()) || "SETTINGS".equals(log.getCategory()) || "CONFIG".equals(log.getCategory()))
                .collect(Collectors.toList());

        // Create a new compliance report
        ComplianceReport report = new ComplianceReport();
        report.setName(name);
        report.setDescription(description);
        report.setGeneratedAt(LocalDateTime.now());
        report.setReportStartDate(LocalDateTime.ofEpochSecond(startTime / 1000, 0, ZoneOffset.UTC));
        report.setReportEndDate(LocalDateTime.ofEpochSecond(endTime / 1000, 0, ZoneOffset.UTC));
        report.setGeneratedBy(generatedBy);
        report.setReportType("SYSTEM_CHANGES");
        report.setStatus("GENERATED");
        report.setIncludedLogs(systemChangesLogs);

        // Generate summary statistics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLogs", systemChangesLogs.size());

        // Count logs by action
        Map<String, Long> actionCounts = systemChangesLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.put("actionCounts", actionCounts);

        // Count logs by status
        Map<String, Long> statusCounts = systemChangesLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getStatus, Collectors.counting()));
        summary.put("statusCounts", statusCounts);

        // Count logs by user
        Map<String, Long> userCounts = systemChangesLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getUsername, Collectors.counting()));
        summary.put("userCounts", userCounts);

        report.setSummary(summary);

        // Save the report
        return complianceReportRepository.save(report);
    }

    /**
     * Generate a custom compliance report for a specific time period with custom filters
     *
     * @param name        the name of the report
     * @param description the description of the report
     * @param reportType  the type of the report
     * @param startTime   the start time of the period to include in the report (epoch millis)
     * @param endTime     the end time of the period to include in the report (epoch millis)
     * @param filters     additional filters to apply to the audit logs
     * @return the generated compliance report
     */
    public ComplianceReport generateCustomReport(String name, String description, String reportType,
                                                 long startTime, long endTime, Map<String, Object> filters) {
        logger.info("Generating custom compliance report: {}", name);

        // Get the current authenticated user
        String generatedBy = getCurrentUsername();

        // Get audit logs for the specified time period
        List<AuditLog> logs = auditLogService.getAuditLogsByTimeRange(startTime, endTime, 0, Integer.MAX_VALUE);

        // Apply additional filters if provided
        if (filters != null && !filters.isEmpty()) {
            logs = auditLogService.getAuditLogsWithFilters(filters, 0, Integer.MAX_VALUE);
        }

        // Create a new compliance report
        ComplianceReport report = new ComplianceReport();
        report.setName(name);
        report.setDescription(description);
        report.setGeneratedAt(LocalDateTime.now());
        report.setReportStartDate(LocalDateTime.ofEpochSecond(startTime / 1000, 0, ZoneOffset.UTC));
        report.setReportEndDate(LocalDateTime.ofEpochSecond(endTime / 1000, 0, ZoneOffset.UTC));
        report.setGeneratedBy(generatedBy);
        report.setReportType(reportType);
        report.setStatus("GENERATED");
        report.setIncludedLogs(logs);

        // Generate summary statistics
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLogs", logs.size());

        // Count logs by category
        Map<String, Long> categoryCounts = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getCategory, Collectors.counting()));
        summary.put("categoryCounts", categoryCounts);

        // Count logs by action
        Map<String, Long> actionCounts = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.put("actionCounts", actionCounts);

        // Count logs by status
        Map<String, Long> statusCounts = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getStatus, Collectors.counting()));
        summary.put("statusCounts", statusCounts);

        // Count logs by user
        Map<String, Long> userCounts = logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getUsername, Collectors.counting()));
        summary.put("userCounts", userCounts);

        report.setSummary(summary);

        // Add metadata about the filters used
        if (filters != null && !filters.isEmpty()) {
            report.setMetadata(new HashMap<>(filters));
        }

        // Save the report
        return complianceReportRepository.save(report);
    }

    /**
     * Get a compliance report by ID
     *
     * @param id the ID of the report
     * @return the compliance report, or null if not found
     */
    public ComplianceReport getReportById(String id) {
        return complianceReportRepository.findById(id);
    }

    /**
     * Get all compliance reports
     *
     * @return list of all compliance reports
     */
    public List<ComplianceReport> getAllReports() {
        return complianceReportRepository.findAll();
    }

    /**
     * Get compliance reports with pagination
     *
     * @param page the page number (0-based)
     * @param size the page size
     * @return list of compliance reports for the specified page
     */
    public List<ComplianceReport> getReports(int page, int size) {
        return complianceReportRepository.findAll(page, size);
    }

    /**
     * Get compliance reports by report type
     *
     * @param reportType the type of report
     * @param page       the page number (0-based)
     * @param size       the page size
     * @return list of compliance reports for the specified page
     */
    public List<ComplianceReport> getReportsByType(String reportType, int page, int size) {
        return complianceReportRepository.findByReportType(reportType, page, size);
    }

    /**
     * Get compliance reports by status
     *
     * @param status the status of the report
     * @param page   the page number (0-based)
     * @param size   the page size
     * @return list of compliance reports for the specified page
     */
    public List<ComplianceReport> getReportsByStatus(String status, int page, int size) {
        return complianceReportRepository.findByStatus(status, page, size);
    }

    /**
     * Get compliance reports by generated by user
     *
     * @param generatedBy the user who generated the report
     * @param page        the page number (0-based)
     * @param size        the page size
     * @return list of compliance reports for the specified page
     */
    public List<ComplianceReport> getReportsByGeneratedBy(String generatedBy, int page, int size) {
        return complianceReportRepository.findByGeneratedBy(generatedBy, page, size);
    }

    /**
     * Get compliance reports with filters
     *
     * @param filters map of filter criteria
     * @param page    the page number (0-based)
     * @param size    the page size
     * @return list of compliance reports matching the filters for the specified page
     */
    public List<ComplianceReport> getReportsWithFilters(Map<String, Object> filters, int page, int size) {
        return complianceReportRepository.findWithFilters(filters, page, size);
    }

    /**
     * Update the status of a compliance report
     *
     * @param id     the ID of the report
     * @param status the new status
     * @return the updated report, or null if not found
     */
    public ComplianceReport updateReportStatus(String id, String status) {
        ComplianceReport report = complianceReportRepository.findById(id);
        if (report != null) {
            report.setStatus(status);
            return complianceReportRepository.save(report);
        }
        return null;
    }

    /**
     * Delete a compliance report
     *
     * @param id the ID of the report to delete
     * @return true if the report was deleted, false if it wasn't found
     */
    public boolean deleteReport(String id) {
        return complianceReportRepository.deleteById(id);
    }

    /**
     * Get the username of the currently authenticated user
     *
     * @return the username, or "system" if no user is authenticated
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}