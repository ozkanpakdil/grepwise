package io.github.ozkanpakdil.grepwise.model;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for log data retention policies.
 * Defines how long log data should be kept before being deleted.
 */
public class RetentionPolicy {
    private String id;
    private String name;
    private int maxAgeDays;
    private boolean enabled;
    private List<String> applyToSources;

    /**
     * Default constructor with sensible defaults.
     */
    public RetentionPolicy() {
        this.enabled = true;
        this.maxAgeDays = 30; // Default to 30 days retention
    }

    /**
     * Full constructor.
     */
    public RetentionPolicy(String id, String name, int maxAgeDays, boolean enabled, List<String> applyToSources) {
        this.id = id;
        this.name = name;
        this.maxAgeDays = maxAgeDays;
        this.enabled = enabled;
        this.applyToSources = applyToSources;
    }

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

    public int getMaxAgeDays() {
        return maxAgeDays;
    }

    public void setMaxAgeDays(int maxAgeDays) {
        this.maxAgeDays = maxAgeDays;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getApplyToSources() {
        return applyToSources;
    }

    public void setApplyToSources(List<String> applyToSources) {
        this.applyToSources = applyToSources;
    }

    /**
     * Checks if this policy applies to the given source.
     * If applyToSources is null or empty, the policy applies to all sources.
     */
    public boolean appliesTo(String source) {
        return applyToSources == null || applyToSources.isEmpty() || applyToSources.contains(source);
    }

    /**
     * Calculates the timestamp threshold for this policy.
     * Logs older than this timestamp should be deleted.
     */
    public long getThresholdTimestamp() {
        return System.currentTimeMillis() - (maxAgeDays * 24L * 60L * 60L * 1000L);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetentionPolicy that = (RetentionPolicy) o;
        return maxAgeDays == that.maxAgeDays &&
                enabled == that.enabled &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(applyToSources, that.applyToSources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, maxAgeDays, enabled, applyToSources);
    }

    @Override
    public String toString() {
        return "RetentionPolicy{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", maxAgeDays=" + maxAgeDays +
                ", enabled=" + enabled +
                ", applyToSources=" + applyToSources +
                '}';
    }
}