package io.github.ozkanpakdil.grepwise.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a compliance report generated from audit logs.
 * This model is used for generating structured reports for compliance purposes.
 */
public class ComplianceReport {
    private String id;
    private String name;
    private String description;
    private LocalDateTime generatedAt;
    private LocalDateTime reportStartDate;
    private LocalDateTime reportEndDate;
    private String generatedBy;
    private String reportType; // e.g., "USER_ACTIVITY", "DATA_ACCESS", "SECURITY", "SYSTEM_CHANGES"
    private String status; // e.g., "GENERATED", "APPROVED", "REJECTED"
    private Map<String, Object> summary; // Summary statistics
    private List<AuditLog> includedLogs; // Audit logs included in this report
    private Map<String, Object> metadata; // Additional metadata for the report

    /**
     * Default constructor
     */
    public ComplianceReport() {
        this.summary = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Parameterized constructor
     */
    public ComplianceReport(String id, String name, String description, LocalDateTime generatedAt,
                            LocalDateTime reportStartDate, LocalDateTime reportEndDate, String generatedBy,
                            String reportType, String status, Map<String, Object> summary,
                            List<AuditLog> includedLogs, Map<String, Object> metadata) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.generatedAt = generatedAt;
        this.reportStartDate = reportStartDate;
        this.reportEndDate = reportEndDate;
        this.generatedBy = generatedBy;
        this.reportType = reportType;
        this.status = status;
        this.summary = summary != null ? summary : new HashMap<>();
        this.includedLogs = includedLogs;
        this.metadata = metadata != null ? metadata : new HashMap<>();
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

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getReportStartDate() {
        return reportStartDate;
    }

    public void setReportStartDate(LocalDateTime reportStartDate) {
        this.reportStartDate = reportStartDate;
    }

    public LocalDateTime getReportEndDate() {
        return reportEndDate;
    }

    public void setReportEndDate(LocalDateTime reportEndDate) {
        this.reportEndDate = reportEndDate;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary != null ? summary : new HashMap<>();
    }

    public void addSummaryItem(String key, Object value) {
        if (this.summary == null) {
            this.summary = new HashMap<>();
        }
        this.summary.put(key, value);
    }

    public List<AuditLog> getIncludedLogs() {
        return includedLogs;
    }

    public void setIncludedLogs(List<AuditLog> includedLogs) {
        this.includedLogs = includedLogs;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplianceReport that = (ComplianceReport) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(generatedAt, that.generatedAt) &&
                Objects.equals(reportType, that.reportType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, generatedAt, reportType);
    }

    @Override
    public String toString() {
        return "ComplianceReport{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", generatedAt=" + generatedAt +
                ", reportStartDate=" + reportStartDate +
                ", reportEndDate=" + reportEndDate +
                ", generatedBy='" + generatedBy + '\'' +
                ", reportType='" + reportType + '\'' +
                ", status='" + status + '\'' +
                ", summary=" + summary +
                ", includedLogs=" + (includedLogs != null ? includedLogs.size() : 0) + " logs" +
                ", metadata=" + metadata +
                '}';
    }
}