package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.AuditLog;
import io.github.ozkanpakdil.grepwise.model.ComplianceReport;
import io.github.ozkanpakdil.grepwise.repository.ComplianceReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ComplianceService class.
 */
public class ComplianceServiceTest {

    @Mock
    private ComplianceReportRepository complianceReportRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ComplianceService complianceService;

    private List<AuditLog> testAuditLogs;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup security context mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");

        // Create test audit logs
        testAuditLogs = createTestAuditLogs();

        // Setup audit log service mock
        when(auditLogService.getAuditLogsByTimeRange(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(testAuditLogs);

        // Setup compliance report repository mock
        when(complianceReportRepository.save(any(ComplianceReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testGenerateUserActivityReport() {
        // Arrange
        String name = "User Activity Report";
        String description = "Test user activity report";
        long startTime = System.currentTimeMillis() - 86400000; // 24 hours ago
        long endTime = System.currentTimeMillis();

        // Act
        ComplianceReport report = complianceService.generateUserActivityReport(name, description, startTime, endTime);

        // Assert
        assertNotNull(report);
        assertEquals(name, report.getName());
        assertEquals(description, report.getDescription());
        assertEquals("USER_ACTIVITY", report.getReportType());
        assertEquals("GENERATED", report.getStatus());
        assertEquals("testuser", report.getGeneratedBy());
        assertNotNull(report.getGeneratedAt());
        assertNotNull(report.getReportStartDate());
        assertNotNull(report.getReportEndDate());
        assertNotNull(report.getIncludedLogs());
        assertNotNull(report.getSummary());

        // Verify that the audit logs were filtered correctly
        assertTrue(report.getIncludedLogs().stream()
                .allMatch(log -> "USER".equals(log.getCategory()) || "AUTH".equals(log.getCategory())));

        // Verify interactions
        verify(auditLogService).getAuditLogsByTimeRange(eq(startTime), eq(endTime), eq(0), eq(Integer.MAX_VALUE));
        verify(complianceReportRepository).save(any(ComplianceReport.class));
    }

    @Test
    public void testGenerateDataAccessReport() {
        // Arrange
        String name = "Data Access Report";
        String description = "Test data access report";
        long startTime = System.currentTimeMillis() - 86400000; // 24 hours ago
        long endTime = System.currentTimeMillis();

        // Act
        ComplianceReport report = complianceService.generateDataAccessReport(name, description, startTime, endTime);

        // Assert
        assertNotNull(report);
        assertEquals(name, report.getName());
        assertEquals(description, report.getDescription());
        assertEquals("DATA_ACCESS", report.getReportType());
        assertEquals("GENERATED", report.getStatus());
        assertEquals("testuser", report.getGeneratedBy());
        assertNotNull(report.getGeneratedAt());
        assertNotNull(report.getReportStartDate());
        assertNotNull(report.getReportEndDate());
        assertNotNull(report.getIncludedLogs());
        assertNotNull(report.getSummary());

        // Verify that the audit logs were filtered correctly
        assertTrue(report.getIncludedLogs().stream()
                .allMatch(log -> "DATA_ACCESS".equals(log.getCategory())));

        // Verify interactions
        verify(auditLogService).getAuditLogsByTimeRange(eq(startTime), eq(endTime), eq(0), eq(Integer.MAX_VALUE));
        verify(complianceReportRepository).save(any(ComplianceReport.class));
    }

    @Test
    public void testGenerateSecurityReport() {
        // Arrange
        String name = "Security Report";
        String description = "Test security report";
        long startTime = System.currentTimeMillis() - 86400000; // 24 hours ago
        long endTime = System.currentTimeMillis();

        // Act
        ComplianceReport report = complianceService.generateSecurityReport(name, description, startTime, endTime);

        // Assert
        assertNotNull(report);
        assertEquals(name, report.getName());
        assertEquals(description, report.getDescription());
        assertEquals("SECURITY", report.getReportType());
        assertEquals("GENERATED", report.getStatus());
        assertEquals("testuser", report.getGeneratedBy());
        assertNotNull(report.getGeneratedAt());
        assertNotNull(report.getReportStartDate());
        assertNotNull(report.getReportEndDate());
        assertNotNull(report.getIncludedLogs());
        assertNotNull(report.getSummary());

        // Verify that the audit logs were filtered correctly
        assertTrue(report.getIncludedLogs().stream()
                .allMatch(log -> "SECURITY".equals(log.getCategory()) || "AUTH".equals(log.getCategory())));

        // Verify interactions
        verify(auditLogService).getAuditLogsByTimeRange(eq(startTime), eq(endTime), eq(0), eq(Integer.MAX_VALUE));
        verify(complianceReportRepository).save(any(ComplianceReport.class));
    }

    @Test
    public void testGenerateSystemChangesReport() {
        // Arrange
        String name = "System Changes Report";
        String description = "Test system changes report";
        long startTime = System.currentTimeMillis() - 86400000; // 24 hours ago
        long endTime = System.currentTimeMillis();

        // Act
        ComplianceReport report = complianceService.generateSystemChangesReport(name, description, startTime, endTime);

        // Assert
        assertNotNull(report);
        assertEquals(name, report.getName());
        assertEquals(description, report.getDescription());
        assertEquals("SYSTEM_CHANGES", report.getReportType());
        assertEquals("GENERATED", report.getStatus());
        assertEquals("testuser", report.getGeneratedBy());
        assertNotNull(report.getGeneratedAt());
        assertNotNull(report.getReportStartDate());
        assertNotNull(report.getReportEndDate());
        assertNotNull(report.getIncludedLogs());
        assertNotNull(report.getSummary());

        // Verify that the audit logs were filtered correctly
        assertTrue(report.getIncludedLogs().stream()
                .allMatch(log -> "SYSTEM".equals(log.getCategory()) || "SETTINGS".equals(log.getCategory()) || "CONFIG".equals(log.getCategory())));

        // Verify interactions
        verify(auditLogService).getAuditLogsByTimeRange(eq(startTime), eq(endTime), eq(0), eq(Integer.MAX_VALUE));
        verify(complianceReportRepository).save(any(ComplianceReport.class));
    }

    @Test
    public void testGenerateCustomReport() {
        // Arrange
        String name = "Custom Report";
        String description = "Test custom report";
        String reportType = "CUSTOM";
        long startTime = System.currentTimeMillis() - 86400000; // 24 hours ago
        long endTime = System.currentTimeMillis();
        Map<String, Object> filters = new HashMap<>();
        filters.put("category", "USER");
        filters.put("action", "LOGIN");

        when(auditLogService.getAuditLogsWithFilters(anyMap(), anyInt(), anyInt()))
                .thenReturn(testAuditLogs.subList(0, 2));

        // Act
        ComplianceReport report = complianceService.generateCustomReport(name, description, reportType, startTime, endTime, filters);

        // Assert
        assertNotNull(report);
        assertEquals(name, report.getName());
        assertEquals(description, report.getDescription());
        assertEquals(reportType, report.getReportType());
        assertEquals("GENERATED", report.getStatus());
        assertEquals("testuser", report.getGeneratedBy());
        assertNotNull(report.getGeneratedAt());
        assertNotNull(report.getReportStartDate());
        assertNotNull(report.getReportEndDate());
        assertNotNull(report.getIncludedLogs());
        assertNotNull(report.getSummary());
        assertNotNull(report.getMetadata());
        assertEquals(filters, report.getMetadata());

        // Verify interactions
        verify(auditLogService).getAuditLogsByTimeRange(eq(startTime), eq(endTime), eq(0), eq(Integer.MAX_VALUE));
        verify(auditLogService).getAuditLogsWithFilters(eq(filters), eq(0), eq(Integer.MAX_VALUE));
        verify(complianceReportRepository).save(any(ComplianceReport.class));
    }

    @Test
    public void testGetReportById() {
        // Arrange
        String id = "test-report-id";
        ComplianceReport testReport = new ComplianceReport();
        testReport.setId(id);
        testReport.setName("Test Report");

        when(complianceReportRepository.findById(id)).thenReturn(testReport);

        // Act
        ComplianceReport result = complianceService.getReportById(id);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Test Report", result.getName());

        // Verify interactions
        verify(complianceReportRepository).findById(id);
    }

    @Test
    public void testGetAllReports() {
        // Arrange
        List<ComplianceReport> testReports = Arrays.asList(
                createTestReport("1", "Report 1"),
                createTestReport("2", "Report 2")
        );

        when(complianceReportRepository.findAll()).thenReturn(testReports);

        // Act
        List<ComplianceReport> result = complianceService.getAllReports();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Report 1", result.get(0).getName());
        assertEquals("Report 2", result.get(1).getName());

        // Verify interactions
        verify(complianceReportRepository).findAll();
    }

    @Test
    public void testGetReports() {
        // Arrange
        int page = 0;
        int size = 10;
        List<ComplianceReport> testReports = Arrays.asList(
                createTestReport("1", "Report 1"),
                createTestReport("2", "Report 2")
        );

        when(complianceReportRepository.findAll(page, size)).thenReturn(testReports);

        // Act
        List<ComplianceReport> result = complianceService.getReports(page, size);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Report 1", result.get(0).getName());
        assertEquals("Report 2", result.get(1).getName());

        // Verify interactions
        verify(complianceReportRepository).findAll(page, size);
    }

    @Test
    public void testUpdateReportStatus() {
        // Arrange
        String id = "test-report-id";
        String newStatus = "APPROVED";
        ComplianceReport testReport = new ComplianceReport();
        testReport.setId(id);
        testReport.setName("Test Report");
        testReport.setStatus("GENERATED");

        when(complianceReportRepository.findById(id)).thenReturn(testReport);
        when(complianceReportRepository.save(any(ComplianceReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ComplianceReport result = complianceService.updateReportStatus(id, newStatus);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("Test Report", result.getName());
        assertEquals(newStatus, result.getStatus());

        // Verify interactions
        verify(complianceReportRepository).findById(id);
        verify(complianceReportRepository).save(any(ComplianceReport.class));
    }

    @Test
    public void testUpdateReportStatus_NotFound() {
        // Arrange
        String id = "non-existent-id";
        String newStatus = "APPROVED";

        when(complianceReportRepository.findById(id)).thenReturn(null);

        // Act
        ComplianceReport result = complianceService.updateReportStatus(id, newStatus);

        // Assert
        assertNull(result);

        // Verify interactions
        verify(complianceReportRepository).findById(id);
        verify(complianceReportRepository, never()).save(any(ComplianceReport.class));
    }

    @Test
    public void testDeleteReport() {
        // Arrange
        String id = "test-report-id";

        when(complianceReportRepository.deleteById(id)).thenReturn(true);

        // Act
        boolean result = complianceService.deleteReport(id);

        // Assert
        assertTrue(result);

        // Verify interactions
        verify(complianceReportRepository).deleteById(id);
    }

    @Test
    public void testDeleteReport_NotFound() {
        // Arrange
        String id = "non-existent-id";

        when(complianceReportRepository.deleteById(id)).thenReturn(false);

        // Act
        boolean result = complianceService.deleteReport(id);

        // Assert
        assertFalse(result);

        // Verify interactions
        verify(complianceReportRepository).deleteById(id);
    }

    /**
     * Helper method to create test audit logs
     */
    private List<AuditLog> createTestAuditLogs() {
        List<AuditLog> logs = new ArrayList<>();

        // User activity logs
        AuditLog userLog1 = new AuditLog();
        userLog1.setId("1");
        userLog1.setTimestamp(System.currentTimeMillis());
        userLog1.setUserId("user1");
        userLog1.setUsername("User One");
        userLog1.setCategory("USER");
        userLog1.setAction("CREATE");
        userLog1.setStatus("SUCCESS");
        userLog1.setDescription("Created user account");
        logs.add(userLog1);

        AuditLog userLog2 = new AuditLog();
        userLog2.setId("2");
        userLog2.setTimestamp(System.currentTimeMillis());
        userLog2.setUserId("user2");
        userLog2.setUsername("User Two");
        userLog2.setCategory("USER");
        userLog2.setAction("UPDATE");
        userLog2.setStatus("SUCCESS");
        userLog2.setDescription("Updated user profile");
        logs.add(userLog2);

        // Auth logs
        AuditLog authLog1 = new AuditLog();
        authLog1.setId("3");
        authLog1.setTimestamp(System.currentTimeMillis());
        authLog1.setUserId("user1");
        authLog1.setUsername("User One");
        authLog1.setCategory("AUTH");
        authLog1.setAction("LOGIN");
        authLog1.setStatus("SUCCESS");
        authLog1.setDescription("User logged in");
        logs.add(authLog1);

        AuditLog authLog2 = new AuditLog();
        authLog2.setId("4");
        authLog2.setTimestamp(System.currentTimeMillis());
        authLog2.setUserId("user2");
        authLog2.setUsername("User Two");
        authLog2.setCategory("AUTH");
        authLog2.setAction("LOGIN");
        authLog2.setStatus("FAILURE");
        authLog2.setDescription("Failed login attempt");
        logs.add(authLog2);

        // Data access logs
        AuditLog dataLog1 = new AuditLog();
        dataLog1.setId("5");
        dataLog1.setTimestamp(System.currentTimeMillis());
        dataLog1.setUserId("user1");
        dataLog1.setUsername("User One");
        dataLog1.setCategory("DATA_ACCESS");
        dataLog1.setAction("READ");
        dataLog1.setStatus("SUCCESS");
        dataLog1.setDescription("Read sensitive data");
        dataLog1.setTargetId("data1");
        dataLog1.setTargetType("SENSITIVE_DATA");
        logs.add(dataLog1);

        // Security logs
        AuditLog securityLog1 = new AuditLog();
        securityLog1.setId("6");
        securityLog1.setTimestamp(System.currentTimeMillis());
        securityLog1.setUserId("user1");
        securityLog1.setUsername("User One");
        securityLog1.setCategory("SECURITY");
        securityLog1.setAction("PERMISSION_CHANGE");
        securityLog1.setStatus("SUCCESS");
        securityLog1.setDescription("Changed user permissions");
        logs.add(securityLog1);

        // System logs
        AuditLog systemLog1 = new AuditLog();
        systemLog1.setId("7");
        systemLog1.setTimestamp(System.currentTimeMillis());
        systemLog1.setUserId("admin");
        systemLog1.setUsername("Admin User");
        systemLog1.setCategory("SYSTEM");
        systemLog1.setAction("CONFIG_CHANGE");
        systemLog1.setStatus("SUCCESS");
        systemLog1.setDescription("Changed system configuration");
        logs.add(systemLog1);

        AuditLog settingsLog1 = new AuditLog();
        settingsLog1.setId("8");
        settingsLog1.setTimestamp(System.currentTimeMillis());
        settingsLog1.setUserId("admin");
        settingsLog1.setUsername("Admin User");
        settingsLog1.setCategory("SETTINGS");
        settingsLog1.setAction("UPDATE");
        settingsLog1.setStatus("SUCCESS");
        settingsLog1.setDescription("Updated application settings");
        logs.add(settingsLog1);

        return logs;
    }

    /**
     * Helper method to create a test compliance report
     */
    private ComplianceReport createTestReport(String id, String name) {
        ComplianceReport report = new ComplianceReport();
        report.setId(id);
        report.setName(name);
        report.setDescription("Test report description");
        report.setGeneratedAt(LocalDateTime.now());
        report.setReportType("TEST");
        report.setStatus("GENERATED");
        report.setGeneratedBy("testuser");
        return report;
    }
}