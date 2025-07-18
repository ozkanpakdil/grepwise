package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import io.github.ozkanpakdil.grepwise.service.FieldConfigurationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing field configurations.
 * This controller provides endpoints for creating, retrieving, updating, and deleting field configurations.
 */
@RestController
@RequestMapping("/api/config/field-configurations")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class FieldConfigurationController {

    private final FieldConfigurationService fieldConfigurationService;

    public FieldConfigurationController(FieldConfigurationService fieldConfigurationService) {
        this.fieldConfigurationService = fieldConfigurationService;
    }

    /**
     * Get all field configurations.
     *
     * @return A list of all field configurations
     */
    @GetMapping
    public ResponseEntity<List<FieldConfiguration>> getAllFieldConfigurations() {
        return ResponseEntity.ok(fieldConfigurationService.getAllFieldConfigurations());
    }

    /**
     * Get all enabled field configurations.
     *
     * @return A list of all enabled field configurations
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<FieldConfiguration>> getAllEnabledFieldConfigurations() {
        return ResponseEntity.ok(fieldConfigurationService.getAllEnabledFieldConfigurations());
    }

    /**
     * Get a field configuration by ID.
     *
     * @param id The ID of the field configuration to get
     * @return The field configuration
     */
    @GetMapping("/{id}")
    public ResponseEntity<FieldConfiguration> getFieldConfigurationById(@PathVariable String id) {
        FieldConfiguration fieldConfiguration = fieldConfigurationService.getFieldConfigurationById(id);
        if (fieldConfiguration == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fieldConfiguration);
    }

    /**
     * Get a field configuration by name.
     *
     * @param name The name of the field configuration to get
     * @return The field configuration
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<FieldConfiguration> getFieldConfigurationByName(@PathVariable String name) {
        FieldConfiguration fieldConfiguration = fieldConfigurationService.getFieldConfigurationByName(name);
        if (fieldConfiguration == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fieldConfiguration);
    }

    /**
     * Create a new field configuration.
     *
     * @param fieldConfiguration The field configuration to create
     * @return The created field configuration
     */
    @PostMapping
    public ResponseEntity<FieldConfiguration> createFieldConfiguration(@RequestBody FieldConfiguration fieldConfiguration) {
        try {
            return ResponseEntity.ok(fieldConfigurationService.saveFieldConfiguration(fieldConfiguration));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing field configuration.
     *
     * @param id The ID of the field configuration to update
     * @param fieldConfiguration The updated field configuration
     * @return The updated field configuration
     */
    @PutMapping("/{id}")
    public ResponseEntity<FieldConfiguration> updateFieldConfiguration(@PathVariable String id, @RequestBody FieldConfiguration fieldConfiguration) {
        FieldConfiguration existingFieldConfiguration = fieldConfigurationService.getFieldConfigurationById(id);
        if (existingFieldConfiguration == null) {
            return ResponseEntity.notFound().build();
        }

        fieldConfiguration.setId(id);
        try {
            return ResponseEntity.ok(fieldConfigurationService.saveFieldConfiguration(fieldConfiguration));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a field configuration.
     *
     * @param id The ID of the field configuration to delete
     * @return No content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFieldConfiguration(@PathVariable String id) {
        if (fieldConfigurationService.deleteFieldConfiguration(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Test a field configuration by extracting a value from a sample string.
     *
     * @param fieldConfiguration The field configuration to test
     * @param sampleString The sample string to extract from
     * @return The extracted value
     */
    @PostMapping("/test")
    public ResponseEntity<String> testFieldConfiguration(@RequestBody FieldConfiguration fieldConfiguration, @RequestParam String sampleString) {
        try {
            String extractedValue = fieldConfigurationService.extractFieldValue(fieldConfiguration, sampleString);
            if (extractedValue == null) {
                return ResponseEntity.ok("No match found");
            }
            return ResponseEntity.ok(extractedValue);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}