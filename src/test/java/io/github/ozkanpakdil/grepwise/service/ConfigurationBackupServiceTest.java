package io.github.ozkanpakdil.grepwise.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.model.RetentionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationBackupServiceTest {

    @Mock
    private LogScannerService logScannerService;

    @Mock
    private RetentionPolicyService retentionPolicyService;

    @Mock
    private FieldConfigurationService fieldConfigurationService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ConfigurationBackupService configurationBackupService;

    private List<LogDirectoryConfig> logDirectoryConfigs;
    private List<RetentionPolicy> retentionPolicies;
    private List<FieldConfiguration> fieldConfigurations;

    @BeforeEach
    void setUp() {
        // Setup test data
        LogDirectoryConfig config1 = new LogDirectoryConfig();
        config1.setId("1");
        config1.setDirectoryPath("C:\\logs");
        config1.setFilePattern("*.log");
        config1.setScanIntervalSeconds(60);
        config1.setEnabled(true);

        LogDirectoryConfig config2 = new LogDirectoryConfig();
        config2.setId("2");
        config2.setDirectoryPath("C:\\app\\logs");
        config2.setFilePattern("*.log");
        config2.setScanIntervalSeconds(120);
        config2.setEnabled(true);

        logDirectoryConfigs = Arrays.asList(config1, config2);

        RetentionPolicy policy1 = new RetentionPolicy();
        policy1.setId("1");
        policy1.setName("30-day retention");
        policy1.setMaxAgeDays(30);
        policy1.setEnabled(true);

        RetentionPolicy policy2 = new RetentionPolicy();
        policy2.setId("2");
        policy2.setName("90-day retention");
        policy2.setMaxAgeDays(90);
        policy2.setEnabled(true);

        retentionPolicies = Arrays.asList(policy1, policy2);

        FieldConfiguration fieldConfig1 = new FieldConfiguration();
        fieldConfig1.setId("1");
        fieldConfig1.setName("ip_address");
        fieldConfig1.setFieldType(FieldConfiguration.FieldType.STRING);
        fieldConfig1.setSourceField("message");
        fieldConfig1.setExtractionPattern("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
        fieldConfig1.setEnabled(true);

        FieldConfiguration fieldConfig2 = new FieldConfiguration();
        fieldConfig2.setId("2");
        fieldConfig2.setName("timestamp");
        fieldConfig2.setFieldType(FieldConfiguration.FieldType.DATE);
        fieldConfig2.setSourceField("message");
        fieldConfig2.setExtractionPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        fieldConfig2.setEnabled(true);

        fieldConfigurations = Arrays.asList(fieldConfig1, fieldConfig2);
    }

    @Test
    void exportConfigurations_ShouldReturnJsonWithAllConfigurations() throws Exception {
        // Arrange
        when(logScannerService.getAllConfigs()).thenReturn(logDirectoryConfigs);
        when(retentionPolicyService.getAllPolicies()).thenReturn(retentionPolicies);
        when(fieldConfigurationService.getAllFieldConfigurations()).thenReturn(fieldConfigurations);

        // Act
        String json = configurationBackupService.exportConfigurations();

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("logDirectoryConfigs"));
        assertTrue(json.contains("retentionPolicies"));
        assertTrue(json.contains("fieldConfigurations"));
        assertTrue(json.contains("C:\\\\logs")); // Path from config1
        assertTrue(json.contains("30-day retention")); // Name from policy1
        assertTrue(json.contains("ip_address")); // Name from fieldConfig1

        // Verify service calls
        verify(logScannerService).getAllConfigs();
        verify(retentionPolicyService).getAllPolicies();
        verify(fieldConfigurationService).getAllFieldConfigurations();
    }

    @Test
    void importConfigurations_WithOverwriteFalse_ShouldSkipExistingConfigurations() throws Exception {
        // Arrange
        String json = "{" +
                "\"metadata\": {\"exportedAt\": 1626912345678, \"version\": \"1.0\"}," +
                "\"logDirectoryConfigs\": [{\"id\": \"1\", \"directoryPath\": \"C:\\\\logs\", \"filePattern\": \"*.log\", \"scanIntervalSeconds\": 60, \"enabled\": true}]," +
                "\"retentionPolicies\": [{\"id\": \"1\", \"name\": \"30-day retention\", \"maxAgeDays\": 30, \"enabled\": true}]," +
                "\"fieldConfigurations\": [{\"id\": \"1\", \"name\": \"ip_address\", \"fieldType\": \"STRING\", \"sourceField\": \"message\", \"extractionPattern\": \"\\\\b(?:\\\\d{1,3}\\\\.){3}\\\\d{1,3}\\\\b\", \"enabled\": true}]" +
                "}";

        // Mock existing configurations
        when(logScannerService.getAllConfigs()).thenReturn(logDirectoryConfigs);
        when(retentionPolicyService.getAllPolicies()).thenReturn(retentionPolicies);
        when(fieldConfigurationService.getAllFieldConfigurations()).thenReturn(fieldConfigurations);

        // Act
        Map<String, Object> summary = configurationBackupService.importConfigurations(json, false);

        // Assert
        assertNotNull(summary);
        assertEquals(0, summary.get("logDirectoryConfigsImported"));
        assertEquals(0, summary.get("retentionPoliciesImported"));
        assertEquals(0, summary.get("fieldConfigurationsImported"));
        assertEquals(0, summary.get("totalImported"));

        // Verify no saves were made
        verify(logScannerService, never()).saveConfig(any());
        verify(retentionPolicyService, never()).savePolicy(any());
        verify(fieldConfigurationService, never()).saveFieldConfiguration(any());
    }

    @Test
    void importConfigurations_WithOverwriteTrue_ShouldImportAllConfigurations() throws Exception {
        // Arrange
        String json = "{" +
                "\"metadata\": {\"exportedAt\": 1626912345678, \"version\": \"1.0\"}," +
                "\"logDirectoryConfigs\": [{\"id\": \"1\", \"directoryPath\": \"C:\\\\logs\", \"filePattern\": \"*.log\", \"scanIntervalSeconds\": 60, \"enabled\": true}]," +
                "\"retentionPolicies\": [{\"id\": \"1\", \"name\": \"30-day retention\", \"maxAgeDays\": 30, \"enabled\": true}]," +
                "\"fieldConfigurations\": [{\"id\": \"1\", \"name\": \"ip_address\", \"fieldType\": \"STRING\", \"sourceField\": \"message\", \"extractionPattern\": \"\\\\b(?:\\\\d{1,3}\\\\.){3}\\\\d{1,3}\\\\b\", \"enabled\": true}]" +
                "}";

        // Mock existing configurations
        when(logScannerService.getAllConfigs()).thenReturn(logDirectoryConfigs);
        when(retentionPolicyService.getAllPolicies()).thenReturn(retentionPolicies);
        when(fieldConfigurationService.getAllFieldConfigurations()).thenReturn(fieldConfigurations);

        // Mock save operations
        when(logScannerService.saveConfig(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(retentionPolicyService.savePolicy(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fieldConfigurationService.saveFieldConfiguration(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> summary = configurationBackupService.importConfigurations(json, true);

        // Assert
        assertNotNull(summary);
        assertEquals(1, summary.get("logDirectoryConfigsImported"));
        assertEquals(1, summary.get("retentionPoliciesImported"));
        assertEquals(1, summary.get("fieldConfigurationsImported"));
        assertEquals(3, summary.get("totalImported"));

        // Verify saves were made
        verify(logScannerService, times(1)).saveConfig(any());
        verify(retentionPolicyService, times(1)).savePolicy(any());
        verify(fieldConfigurationService, times(1)).saveFieldConfiguration(any());
    }

    @Test
    void importConfigurations_WithNewConfigurations_ShouldImportOnlyNewOnes() throws Exception {
        // Arrange
        String json = "{" +
                "\"metadata\": {\"exportedAt\": 1626912345678, \"version\": \"1.0\"}," +
                "\"logDirectoryConfigs\": [{\"id\": \"3\", \"directoryPath\": \"C:\\\\new\\\\logs\", \"filePattern\": \"*.log\", \"scanIntervalSeconds\": 60, \"enabled\": true}]," +
                "\"retentionPolicies\": [{\"id\": \"3\", \"name\": \"60-day retention\", \"maxAgeDays\": 60, \"enabled\": true}]," +
                "\"fieldConfigurations\": [{\"id\": \"3\", \"name\": \"user_id\", \"fieldType\": \"STRING\", \"sourceField\": \"message\", \"extractionPattern\": \"user_id=(\\\\d+)\", \"enabled\": true}]" +
                "}";

        // Mock existing configurations
        when(logScannerService.getAllConfigs()).thenReturn(logDirectoryConfigs);
        when(retentionPolicyService.getAllPolicies()).thenReturn(retentionPolicies);
        when(fieldConfigurationService.getAllFieldConfigurations()).thenReturn(fieldConfigurations);

        // Mock save operations
        when(logScannerService.saveConfig(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(retentionPolicyService.savePolicy(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fieldConfigurationService.saveFieldConfiguration(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Map<String, Object> summary = configurationBackupService.importConfigurations(json, false);

        // Assert
        assertNotNull(summary);
        assertEquals(1, summary.get("logDirectoryConfigsImported"));
        assertEquals(1, summary.get("retentionPoliciesImported"));
        assertEquals(1, summary.get("fieldConfigurationsImported"));
        assertEquals(3, summary.get("totalImported"));

        // Verify saves were made
        verify(logScannerService, times(1)).saveConfig(any());
        verify(retentionPolicyService, times(1)).savePolicy(any());
        verify(fieldConfigurationService, times(1)).saveFieldConfiguration(any());
    }
}