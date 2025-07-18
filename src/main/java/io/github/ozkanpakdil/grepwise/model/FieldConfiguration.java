package io.github.ozkanpakdil.grepwise.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a field configuration for log entry indexing.
 * This defines how custom fields are extracted and indexed from log entries.
 */
public class FieldConfiguration {
    
    /**
     * Enum representing the type of field.
     */
    public enum FieldType {
        STRING,
        NUMBER,
        DATE,
        BOOLEAN
    }
    
    private String id;
    private String name;
    private String description;
    private FieldType fieldType;
    private String extractionPattern;
    private String sourceField;
    private boolean isStored;
    private boolean isIndexed;
    private boolean isTokenized;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
    
    public FieldConfiguration() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isStored = true;
        this.isIndexed = true;
        this.isTokenized = false;
        this.enabled = true;
        this.fieldType = FieldType.STRING;
    }
    
    public FieldConfiguration(String name, String description, FieldType fieldType, 
                             String extractionPattern, String sourceField, 
                             boolean isStored, boolean isIndexed, boolean isTokenized, 
                             boolean enabled) {
        this();
        this.name = name;
        this.description = description;
        this.fieldType = fieldType;
        this.extractionPattern = extractionPattern;
        this.sourceField = sourceField;
        this.isStored = isStored;
        this.isIndexed = isIndexed;
        this.isTokenized = isTokenized;
        this.enabled = enabled;
    }
    
    // Getters and setters
    
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
    
    public FieldType getFieldType() {
        return fieldType;
    }
    
    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }
    
    public String getExtractionPattern() {
        return extractionPattern;
    }
    
    public void setExtractionPattern(String extractionPattern) {
        this.extractionPattern = extractionPattern;
    }
    
    public String getSourceField() {
        return sourceField;
    }
    
    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }
    
    public boolean isStored() {
        return isStored;
    }
    
    public void setStored(boolean stored) {
        isStored = stored;
    }
    
    public boolean isIndexed() {
        return isIndexed;
    }
    
    public void setIndexed(boolean indexed) {
        isIndexed = indexed;
    }
    
    public boolean isTokenized() {
        return isTokenized;
    }
    
    public void setTokenized(boolean tokenized) {
        isTokenized = tokenized;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldConfiguration that = (FieldConfiguration) o;
        return isStored == that.isStored &&
               isIndexed == that.isIndexed &&
               isTokenized == that.isTokenized &&
               enabled == that.enabled &&
               Objects.equals(id, that.id) &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               fieldType == that.fieldType &&
               Objects.equals(extractionPattern, that.extractionPattern) &&
               Objects.equals(sourceField, that.sourceField);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, fieldType, extractionPattern, sourceField, 
                           isStored, isIndexed, isTokenized, enabled);
    }
    
    @Override
    public String toString() {
        return "FieldConfiguration{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", fieldType=" + fieldType +
               ", extractionPattern='" + extractionPattern + '\'' +
               ", sourceField='" + sourceField + '\'' +
               ", isStored=" + isStored +
               ", isIndexed=" + isIndexed +
               ", isTokenized=" + isTokenized +
               ", enabled=" + enabled +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}