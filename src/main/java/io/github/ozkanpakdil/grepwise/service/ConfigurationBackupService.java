package io.github.ozkanpakdil.grepwise.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.model.RetentionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for backing up and restoring application configurations.
 * This service provides functionality to export all configurations as JSON
 * and import configurations from JSON.
 */
@Service
public class ConfigurationBackupService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationBackupService.class);

    private final LogScannerService logScannerService;
    private final RetentionPolicyService retentionPolicyService;
    private final FieldConfigurationService fieldConfigurationService;
    private final ObjectMapper objectMapper;

    public ConfigurationBackupService(
            LogScannerService logScannerService,
            RetentionPolicyService retentionPolicyService,
            FieldConfigurationService fieldConfigurationService,
            ObjectMapper objectMapper) {
        this.logScannerService = logScannerService;
        this.retentionPolicyService = retentionPolicyService;
        this.fieldConfigurationService = fieldConfigurationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Export all configurations as a JSON string.
     *
     * @return JSON string containing all configurations
     */
    public String exportConfigurations() {
        try {
            Map<String, Object> configurations = new HashMap<>();
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exportedAt", System.currentTimeMillis());
            metadata.put("version", "1.0");
            configurations.put("metadata", metadata);
            
            // Add log directory configurations
            List<LogDirectoryConfig> logDirectoryConfigs = logScannerService.getAllConfigs();
            configurations.put("logDirectoryConfigs", logDirectoryConfigs);
            
            // Add retention policies
            List<RetentionPolicy> retentionPolicies = retentionPolicyService.getAllPolicies();
            configurations.put("retentionPolicies", retentionPolicies);
            
            // Add field configurations
            List<FieldConfiguration> fieldConfigurations = fieldConfigurationService.getAllFieldConfigurations();
            configurations.put("fieldConfigurations", fieldConfigurations);
            
            // Convert to JSON
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(configurations);
        } catch (Exception e) {
            logger.error("Error exporting configurations", e);
            throw new RuntimeException("Failed to export configurations", e);
        }
    }

    /**
     * Import configurations from a JSON string.
     *
     * @param json JSON string containing configurations
     * @param overwrite Whether to overwrite existing configurations
     * @return Summary of the import operation
     */
    public Map<String, Object> importConfigurations(String json, boolean overwrite) {
        try {
            // Parse JSON
            Map<String, Object> configurations = objectMapper.readValue(json, Map.class);
            
            // Initialize counters for summary
            int logDirConfigsImported = 0;
            int retentionPoliciesImported = 0;
            int fieldConfigsImported = 0;
            
            // Import log directory configurations
            if (configurations.containsKey("logDirectoryConfigs")) {
                List<Map<String, Object>> logDirConfigs = (List<Map<String, Object>>) configurations.get("logDirectoryConfigs");
                for (Map<String, Object> configMap : logDirConfigs) {
                    LogDirectoryConfig config = objectMapper.convertValue(configMap, LogDirectoryConfig.class);
                    
                    // If not overwriting, skip if a config with the same path already exists
                    if (!overwrite) {
                        boolean exists = logScannerService.getAllConfigs().stream()
                                .anyMatch(c -> c.getDirectoryPath().equals(config.getDirectoryPath()));
                        if (exists) {
                            continue;
                        }
                    }
                    
                    // Clear ID to create a new config
                    config.setId(null);
                    logScannerService.saveConfig(config);
                    logDirConfigsImported++;
                }
            }
            
            // Import retention policies
            if (configurations.containsKey("retentionPolicies")) {
                List<Map<String, Object>> retentionPolicies = (List<Map<String, Object>>) configurations.get("retentionPolicies");
                for (Map<String, Object> policyMap : retentionPolicies) {
                    RetentionPolicy policy = objectMapper.convertValue(policyMap, RetentionPolicy.class);
                    
                    // If not overwriting, skip if a policy with the same name already exists
                    if (!overwrite) {
                        boolean exists = retentionPolicyService.getAllPolicies().stream()
                                .anyMatch(p -> p.getName().equals(policy.getName()));
                        if (exists) {
                            continue;
                        }
                    }
                    
                    // Clear ID to create a new policy
                    policy.setId(null);
                    retentionPolicyService.savePolicy(policy);
                    retentionPoliciesImported++;
                }
            }
            
            // Import field configurations
            if (configurations.containsKey("fieldConfigurations")) {
                List<Map<String, Object>> fieldConfigs = (List<Map<String, Object>>) configurations.get("fieldConfigurations");
                for (Map<String, Object> configMap : fieldConfigs) {
                    FieldConfiguration config = objectMapper.convertValue(configMap, FieldConfiguration.class);
                    
                    // If not overwriting, skip if a config with the same name already exists
                    if (!overwrite) {
                        boolean exists = fieldConfigurationService.getAllFieldConfigurations().stream()
                                .anyMatch(c -> c.getName().equals(config.getName()));
                        if (exists) {
                            continue;
                        }
                    }
                    
                    // Clear ID to create a new config
                    config.setId(null);
                    fieldConfigurationService.saveFieldConfiguration(config);
                    fieldConfigsImported++;
                }
            }
            
            // Create summary
            Map<String, Object> summary = new HashMap<>();
            summary.put("logDirectoryConfigsImported", logDirConfigsImported);
            summary.put("retentionPoliciesImported", retentionPoliciesImported);
            summary.put("fieldConfigurationsImported", fieldConfigsImported);
            summary.put("totalImported", logDirConfigsImported + retentionPoliciesImported + fieldConfigsImported);
            
            return summary;
        } catch (Exception e) {
            logger.error("Error importing configurations", e);
            throw new RuntimeException("Failed to import configurations", e);
        }
    }
}