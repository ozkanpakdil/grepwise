package io.github.ozkanpakdil.grepwise.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.grepwise.model.ComplianceReport;
import io.github.ozkanpakdil.grepwise.service.ComplianceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the ComplianceController class.
 */
public class ComplianceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ComplianceService complianceService;

    @InjectMocks
    private ComplianceController complianceController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(complianceController).build();
        
        // Configure ObjectMapper for LocalDateTime serialization
        objectMapper.findAndRegisterModules();
    }

    @Test
    public void testGetReports() throws Exception {
        // Arrange
        List<ComplianceReport> testReports = Arrays.asList(
                createTestReport("1", "Report 1"),
                createTestReport("2", "Report 2")
        );

        when(complianceService.getReports(anyInt(), anyInt())).thenReturn(testReports);

        // Act & Assert
        mockMvc.perform(get("/api/compliance/reports")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("1")))
                .andExpect(jsonPath("$[0].name", is("Report 1")))
                .andExpect(jsonPath("$[1].id", is("2")))
                .andExpect(jsonPath("$[1].name", is("Report 2")));

        // Verify
        verify(complianceService).getReports(0, 10);
    }

    @Test
    public void testGetReportsWithFilters() throws Exception {
        // Arrange
        List<ComplianceReport> testReports = Arrays.asList(
                createTestReport("1", "Report 1")
        );

        when(complianceService.getReportsWithFilters(anyMap(), anyInt(), anyInt())).thenReturn(testReports);

        // Act & Assert
        mockMvc.perform(get("/api/compliance/reports")
                .param("page", "0")
                .param("size", "10")
                .param("reportType", "USER_ACTIVITY")
                .param("status", "GENERATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("1")))
                .andExpect(jsonPath("$[0].name", is("Report 1")));

        // Verify
        verify(complianceService).getReportsWithFilters(argThat(filters -> 
                "USER_ACTIVITY".equals(filters.get("reportType")) && 
                "GENERATED".equals(filters.get("status"))), eq(0), eq(10));
    }

    @Test
    public void testGetReportById() throws Exception {
        // Arrange
        ComplianceReport testReport = createTestReport("1", "Report 1");
        when(complianceService.getReportById("1")).thenReturn(testReport);

        // Act & Assert
        mockMvc.perform(get("/api/compliance/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Report 1")));

        // Verify
        verify(complianceService).getReportById("1");
    }

    @Test
    public void testGetReportById_NotFound() throws Exception {
        // Arrange
        when(complianceService.getReportById("999")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/compliance/reports/999"))
                .andExpect(status().isNotFound());

        // Verify
        verify(complianceService).getReportById("999");
    }

    @Test
    public void testGenerateUserActivityReport() throws Exception {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "User Activity Report");
        requestBody.put("description", "Test user activity report");
        requestBody.put("startTime", 1627776000000L);
        requestBody.put("endTime", 1627862400000L);

        ComplianceReport testReport = createTestReport("1", "User Activity Report");
        testReport.setReportType("USER_ACTIVITY");
        
        when(complianceService.generateUserActivityReport(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(testReport);

        // Act & Assert
        mockMvc.perform(post("/api/compliance/reports/user-activity")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("User Activity Report")))
                .andExpect(jsonPath("$.reportType", is("USER_ACTIVITY")));

        // Verify
        verify(complianceService).generateUserActivityReport(
                eq("User Activity Report"), 
                eq("Test user activity report"), 
                eq(1627776000000L), 
                eq(1627862400000L));
    }

    @Test
    public void testGenerateDataAccessReport() throws Exception {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Data Access Report");
        requestBody.put("description", "Test data access report");
        requestBody.put("startTime", 1627776000000L);
        requestBody.put("endTime", 1627862400000L);

        ComplianceReport testReport = createTestReport("1", "Data Access Report");
        testReport.setReportType("DATA_ACCESS");
        
        when(complianceService.generateDataAccessReport(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(testReport);

        // Act & Assert
        mockMvc.perform(post("/api/compliance/reports/data-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Data Access Report")))
                .andExpect(jsonPath("$.reportType", is("DATA_ACCESS")));

        // Verify
        verify(complianceService).generateDataAccessReport(
                eq("Data Access Report"), 
                eq("Test data access report"), 
                eq(1627776000000L), 
                eq(1627862400000L));
    }

    @Test
    public void testGenerateSecurityReport() throws Exception {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Security Report");
        requestBody.put("description", "Test security report");
        requestBody.put("startTime", 1627776000000L);
        requestBody.put("endTime", 1627862400000L);

        ComplianceReport testReport = createTestReport("1", "Security Report");
        testReport.setReportType("SECURITY");
        
        when(complianceService.generateSecurityReport(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(testReport);

        // Act & Assert
        mockMvc.perform(post("/api/compliance/reports/security")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Security Report")))
                .andExpect(jsonPath("$.reportType", is("SECURITY")));

        // Verify
        verify(complianceService).generateSecurityReport(
                eq("Security Report"), 
                eq("Test security report"), 
                eq(1627776000000L), 
                eq(1627862400000L));
    }

    @Test
    public void testGenerateSystemChangesReport() throws Exception {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "System Changes Report");
        requestBody.put("description", "Test system changes report");
        requestBody.put("startTime", 1627776000000L);
        requestBody.put("endTime", 1627862400000L);

        ComplianceReport testReport = createTestReport("1", "System Changes Report");
        testReport.setReportType("SYSTEM_CHANGES");
        
        when(complianceService.generateSystemChangesReport(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(testReport);

        // Act & Assert
        mockMvc.perform(post("/api/compliance/reports/system-changes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("System Changes Report")))
                .andExpect(jsonPath("$.reportType", is("SYSTEM_CHANGES")));

        // Verify
        verify(complianceService).generateSystemChangesReport(
                eq("System Changes Report"), 
                eq("Test system changes report"), 
                eq(1627776000000L), 
                eq(1627862400000L));
    }

    @Test
    public void testGenerateCustomReport() throws Exception {
        // Arrange
        Map<String, Object> filters = new HashMap<>();
        filters.put("category", "USER");
        filters.put("action", "LOGIN");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "Custom Report");
        requestBody.put("description", "Test custom report");
        requestBody.put("reportType", "CUSTOM");
        requestBody.put("startTime", 1627776000000L);
        requestBody.put("endTime", 1627862400000L);
        requestBody.put("filters", filters);

        ComplianceReport testReport = createTestReport("1", "Custom Report");
        testReport.setReportType("CUSTOM");
        
        when(complianceService.generateCustomReport(anyString(), anyString(), anyString(), anyLong(), anyLong(), anyMap()))
                .thenReturn(testReport);

        // Act & Assert
        mockMvc.perform(post("/api/compliance/reports/custom")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Custom Report")))
                .andExpect(jsonPath("$.reportType", is("CUSTOM")));

        // Verify
        verify(complianceService).generateCustomReport(
                eq("Custom Report"), 
                eq("Test custom report"), 
                eq("CUSTOM"),
                eq(1627776000000L), 
                eq(1627862400000L),
                eq(filters));
    }

    @Test
    public void testUpdateReportStatus() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("status", "APPROVED");

        ComplianceReport testReport = createTestReport("1", "Report 1");
        testReport.setStatus("APPROVED");
        
        when(complianceService.updateReportStatus("1", "APPROVED")).thenReturn(testReport);

        // Act & Assert
        mockMvc.perform(put("/api/compliance/reports/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Report 1")))
                .andExpect(jsonPath("$.status", is("APPROVED")));

        // Verify
        verify(complianceService).updateReportStatus("1", "APPROVED");
    }

    @Test
    public void testUpdateReportStatus_BadRequest() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        // Missing status field

        // Act & Assert
        mockMvc.perform(put("/api/compliance/reports/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());

        // Verify
        verify(complianceService, never()).updateReportStatus(anyString(), anyString());
    }

    @Test
    public void testUpdateReportStatus_NotFound() throws Exception {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("status", "APPROVED");
        
        when(complianceService.updateReportStatus("999", "APPROVED")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(put("/api/compliance/reports/999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());

        // Verify
        verify(complianceService).updateReportStatus("999", "APPROVED");
    }

    @Test
    public void testDeleteReport() throws Exception {
        // Arrange
        when(complianceService.deleteReport("1")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/compliance/reports/1"))
                .andExpect(status().isNoContent());

        // Verify
        verify(complianceService).deleteReport("1");
    }

    @Test
    public void testDeleteReport_NotFound() throws Exception {
        // Arrange
        when(complianceService.deleteReport("999")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/compliance/reports/999"))
                .andExpect(status().isNotFound());

        // Verify
        verify(complianceService).deleteReport("999");
    }

    @Test
    public void testGetReportTypes() throws Exception {
        // Arrange
        List<ComplianceReport> testReports = Arrays.asList(
                createTestReportWithType("1", "Report 1", "USER_ACTIVITY"),
                createTestReportWithType("2", "Report 2", "DATA_ACCESS"),
                createTestReportWithType("3", "Report 3", "SECURITY"),
                createTestReportWithType("4", "Report 4", "SYSTEM_CHANGES")
        );

        when(complianceService.getReportsByType(eq(""), anyInt(), anyInt())).thenReturn(testReports);

        // Act & Assert
        mockMvc.perform(get("/api/compliance/report-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$", containsInAnyOrder("USER_ACTIVITY", "DATA_ACCESS", "SECURITY", "SYSTEM_CHANGES")));

        // Verify
        verify(complianceService).getReportsByType("", 0, Integer.MAX_VALUE);
    }

    @Test
    public void testGetReportStatuses() throws Exception {
        // Arrange
        List<ComplianceReport> testReports = Arrays.asList(
                createTestReportWithStatus("1", "Report 1", "GENERATED"),
                createTestReportWithStatus("2", "Report 2", "APPROVED"),
                createTestReportWithStatus("3", "Report 3", "REJECTED")
        );

        when(complianceService.getReportsByStatus(eq(""), anyInt(), anyInt())).thenReturn(testReports);

        // Act & Assert
        mockMvc.perform(get("/api/compliance/report-statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", containsInAnyOrder("GENERATED", "APPROVED", "REJECTED")));

        // Verify
        verify(complianceService).getReportsByStatus("", 0, Integer.MAX_VALUE);
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

    /**
     * Helper method to create a test compliance report with a specific type
     */
    private ComplianceReport createTestReportWithType(String id, String name, String reportType) {
        ComplianceReport report = createTestReport(id, name);
        report.setReportType(reportType);
        return report;
    }

    /**
     * Helper method to create a test compliance report with a specific status
     */
    private ComplianceReport createTestReportWithStatus(String id, String name, String status) {
        ComplianceReport report = createTestReport(id, name);
        report.setStatus(status);
        return report;
    }
}