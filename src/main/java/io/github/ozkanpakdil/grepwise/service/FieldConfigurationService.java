package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import io.github.ozkanpakdil.grepwise.repository.FieldConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Service for managing field configurations.
 * This service provides methods for creating, retrieving, updating, and deleting field configurations,
 * as well as extracting field values from log entries based on the configurations.
 */
@Service
public class FieldConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(FieldConfigurationService.class);

    private final FieldConfigurationRepository fieldConfigurationRepository;

    /**
     * Constructor for FieldConfigurationService.
     */
    public FieldConfigurationService(FieldConfigurationRepository fieldConfigurationRepository) {
        this.fieldConfigurationRepository = fieldConfigurationRepository;
        logger.info("FieldConfigurationService initialized");
    }

    /**
     * Get all field configurations.
     *
     * @return A list of all field configurations
     */
    public List<FieldConfiguration> getAllFieldConfigurations() {
        return fieldConfigurationRepository.findAll();
    }

    /**
     * Get all enabled field configurations.
     *
     * @return A list of all enabled field configurations
     */
    public List<FieldConfiguration> getAllEnabledFieldConfigurations() {
        return fieldConfigurationRepository.findAllEnabled();
    }

    /**
     * Get a field configuration by ID.
     *
     * @param id The ID of the field configuration to retrieve
     * @return The field configuration, or null if not found
     */
    public FieldConfiguration getFieldConfigurationById(String id) {
        return fieldConfigurationRepository.findById(id);
    }

    /**
     * Get a field configuration by name.
     *
     * @param name The name of the field configuration to retrieve
     * @return The field configuration, or null if not found
     */
    public FieldConfiguration getFieldConfigurationByName(String name) {
        return fieldConfigurationRepository.findByName(name);
    }

    /**
     * Save a field configuration.
     *
     * @param fieldConfiguration The field configuration to save
     * @return The saved field configuration
     */
    public FieldConfiguration saveFieldConfiguration(FieldConfiguration fieldConfiguration) {
        // Validate the field configuration
        validateFieldConfiguration(fieldConfiguration);

        // Save the field configuration
        return fieldConfigurationRepository.save(fieldConfiguration);
    }

    /**
     * Delete a field configuration by ID.
     *
     * @param id The ID of the field configuration to delete
     * @return true if the field configuration was deleted, false otherwise
     */
    public boolean deleteFieldConfiguration(String id) {
        return fieldConfigurationRepository.deleteById(id);
    }

    /**
     * Extract a field value from a source string based on a field configuration.
     *
     * @param fieldConfiguration The field configuration to use for extraction
     * @param sourceValue        The source string to extract from
     * @return The extracted value, or null if no match was found
     */
    public String extractFieldValue(FieldConfiguration fieldConfiguration, String sourceValue) {
        if (sourceValue == null || sourceValue.isEmpty()) {
            return null;
        }

        // If no extraction pattern is specified, return the source value as is
        if (fieldConfiguration.getExtractionPattern() == null || fieldConfiguration.getExtractionPattern().isEmpty()) {
            return sourceValue;
        }

        try {
            // Compile the extraction pattern
            Pattern pattern = Pattern.compile(fieldConfiguration.getExtractionPattern());
            Matcher matcher = pattern.matcher(sourceValue);

            // Find the first match
            if (matcher.find()) {
                // If the pattern has capturing groups, return the first group
                if (matcher.groupCount() > 0) {
                    return matcher.group(1);
                }
                // Otherwise, return the entire match
                return matcher.group();
            }
        } catch (PatternSyntaxException e) {
            logger.error("Invalid extraction pattern in field configuration: {}", fieldConfiguration, e);
        }

        return null;
    }

    /**
     * Validate a field configuration.
     *
     * @param fieldConfiguration The field configuration to validate
     * @throws IllegalArgumentException if the field configuration is invalid
     */
    private void validateFieldConfiguration(FieldConfiguration fieldConfiguration) {
        // Check that the name is not null or empty
        if (fieldConfiguration.getName() == null || fieldConfiguration.getName().isEmpty()) {
            throw new IllegalArgumentException("Field configuration name cannot be null or empty");
        }

        // Check that the field type is not null
        if (fieldConfiguration.getFieldType() == null) {
            throw new IllegalArgumentException("Field configuration type cannot be null");
        }

        // Check that the source field is not null or empty
        if (fieldConfiguration.getSourceField() == null || fieldConfiguration.getSourceField().isEmpty()) {
            throw new IllegalArgumentException("Field configuration source field cannot be null or empty");
        }

        // Validate the extraction pattern if specified
        if (fieldConfiguration.getExtractionPattern() != null && !fieldConfiguration.getExtractionPattern().isEmpty()) {
            try {
                Pattern.compile(fieldConfiguration.getExtractionPattern());
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid extraction pattern: " + e.getMessage(), e);
            }
        }
    }
}