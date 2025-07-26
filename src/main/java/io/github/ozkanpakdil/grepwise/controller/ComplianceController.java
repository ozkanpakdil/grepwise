package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.ComplianceReport;
import io.github.ozkanpakdil.grepwise.service.ComplianceService;
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
 * REST controller for compliance reporting functionality.
 * Provides endpoints for generating, retrieving, and managing compliance reports.
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceController.class);

    @Autowired
    private ComplianceService complianceService;

    /**
     * Get compliance reports with optional filtering
     * @param page the page number (0-based)
     * @param size the page size
     * @param reportType filter by report type
     * @param status filter by status
     * @param generatedBy filter by user who generated the report
     * @param startDate filter by start date (epoch millis)
     * @param endDate filter by end date (epoch millis)
     * @param searchText search in name and description
     * @return list of compliance reports
     */
    @GetMapping("/reports")
    public ResponseEntity<List<ComplianceReport>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String generatedBy,
            @RequestParam(required = false) Long startDate,
            @RequestParam(required = false) Long endDate,
            @RequestParam(required = false) String searchText) {
        
        logger.info("Getting compliance reports with filters: page={}, size={}, reportType={}, status={}, generatedBy={}, startDate={}, endDate={}, searchText={}",
                page, size, reportType, status, generatedBy, startDate, endDate, searchText);
        
        List<ComplianceReport> reports;
        
        if (reportType != null || status != null || generatedBy != null || startDate != null || endDate != null || searchText != null) {
            // Apply filters
            Map<String, Object> filters = new HashMap<>();
            if (reportType != null) {
                filters.put("reportType", reportType);
            }
            if (status != null) {
                filters.put("status", status);
            }
            if (generatedBy != null) {
                filters.put("generatedBy", generatedBy);
            }
            if (startDate != null) {
                filters.put("startDate", startDate);
            }
            if (endDate != null) {
                filters.put("endDate", endDate);
            }
            if (searchText != null) {
                filters.put("searchText", searchText);
            }
            
            reports = complianceService.getReportsWithFilters(filters, page, size);
        } else {
            // Get all reports
            reports = complianceService.getReports(page, size);
        }
        
        return ResponseEntity.ok(reports);
    }

    /**
     * Get a compliance report by ID
     * @param id the ID of the report
     * @return the compliance report
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<ComplianceReport> getReportById(@PathVariable String id) {
        logger.info("Getting compliance report by ID: {}", id);
        
        ComplianceReport report = complianceService.getReportById(id);
        if (report != null) {
            return ResponseEntity.ok(report);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generate a user activity compliance report
     * @param request the report generation request
     * @return the generated report
     */
    @PostMapping("/reports/user-activity")
    public ResponseEntity<ComplianceReport> generateUserActivityReport(@RequestBody Map<String, Object> request) {
        logger.info("Generating user activity compliance report: {}", request);
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Long startTime = Long.valueOf(request.get("startTime").toString());
        Long endTime = Long.valueOf(request.get("endTime").toString());
        
        ComplianceReport report = complianceService.generateUserActivityReport(name, description, startTime, endTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * Generate a data access compliance report
     * @param request the report generation request
     * @return the generated report
     */
    @PostMapping("/reports/data-access")
    public ResponseEntity<ComplianceReport> generateDataAccessReport(@RequestBody Map<String, Object> request) {
        logger.info("Generating data access compliance report: {}", request);
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Long startTime = Long.valueOf(request.get("startTime").toString());
        Long endTime = Long.valueOf(request.get("endTime").toString());
        
        ComplianceReport report = complianceService.generateDataAccessReport(name, description, startTime, endTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * Generate a security compliance report
     * @param request the report generation request
     * @return the generated report
     */
    @PostMapping("/reports/security")
    public ResponseEntity<ComplianceReport> generateSecurityReport(@RequestBody Map<String, Object> request) {
        logger.info("Generating security compliance report: {}", request);
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Long startTime = Long.valueOf(request.get("startTime").toString());
        Long endTime = Long.valueOf(request.get("endTime").toString());
        
        ComplianceReport report = complianceService.generateSecurityReport(name, description, startTime, endTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * Generate a system changes compliance report
     * @param request the report generation request
     * @return the generated report
     */
    @PostMapping("/reports/system-changes")
    public ResponseEntity<ComplianceReport> generateSystemChangesReport(@RequestBody Map<String, Object> request) {
        logger.info("Generating system changes compliance report: {}", request);
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Long startTime = Long.valueOf(request.get("startTime").toString());
        Long endTime = Long.valueOf(request.get("endTime").toString());
        
        ComplianceReport report = complianceService.generateSystemChangesReport(name, description, startTime, endTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * Generate a custom compliance report
     * @param request the report generation request
     * @return the generated report
     */
    @PostMapping("/reports/custom")
    public ResponseEntity<ComplianceReport> generateCustomReport(@RequestBody Map<String, Object> request) {
        logger.info("Generating custom compliance report: {}", request);
        
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        String reportType = (String) request.get("reportType");
        Long startTime = Long.valueOf(request.get("startTime").toString());
        Long endTime = Long.valueOf(request.get("endTime").toString());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) request.get("filters");
        
        ComplianceReport report = complianceService.generateCustomReport(name, description, reportType, startTime, endTime, filters);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * Update the status of a compliance report
     * @param id the ID of the report
     * @param request the update request
     * @return the updated report
     */
    @PutMapping("/reports/{id}/status")
    public ResponseEntity<ComplianceReport> updateReportStatus(@PathVariable String id, @RequestBody Map<String, String> request) {
        logger.info("Updating compliance report status: id={}, status={}", id, request.get("status"));
        
        String status = request.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        
        ComplianceReport report = complianceService.updateReportStatus(id, status);
        if (report != null) {
            return ResponseEntity.ok(report);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a compliance report
     * @param id the ID of the report to delete
     * @return no content if successful
     */
    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable String id) {
        logger.info("Deleting compliance report: {}", id);
        
        boolean deleted = complianceService.deleteReport(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get report types
     * @return list of distinct report types
     */
    @GetMapping("/report-types")
    public ResponseEntity<List<String>> getReportTypes() {
        logger.info("Getting report types");
        
        List<String> reportTypes = complianceService.getReportsByType("", 0, Integer.MAX_VALUE)
                .stream()
                .map(ComplianceReport::getReportType)
                .distinct()
                .filter(type -> type != null && !type.isEmpty())
                .sorted()
                .toList();
        
        return ResponseEntity.ok(reportTypes);
    }

    /**
     * Get report statuses
     * @return list of distinct report statuses
     */
    @GetMapping("/report-statuses")
    public ResponseEntity<List<String>> getReportStatuses() {
        logger.info("Getting report statuses");
        
        List<String> statuses = complianceService.getReportsByStatus("", 0, Integer.MAX_VALUE)
                .stream()
                .map(ComplianceReport::getStatus)
                .distinct()
                .filter(status -> status != null && !status.isEmpty())
                .sorted()
                .toList();
        
        return ResponseEntity.ok(statuses);
    }
}