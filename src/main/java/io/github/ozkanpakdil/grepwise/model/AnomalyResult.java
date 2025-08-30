package io.github.ozkanpakdil.grepwise.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of an anomaly detection analysis.
 * Contains information about the detected anomaly, including its timestamp,
 * score, expected and actual values, and a description.
 */
public record AnomalyResult(
        String id,
        long timestamp,
        double score,
        double expectedValue,
        double actualValue,
        String description,
        Map<String, Object> metadata
) {
    public AnomalyResult {
        metadata = metadata != null ? metadata : new HashMap<>();
    }

    public AnomalyResult(long timestamp, double score, double expectedValue, double actualValue, String description) {
        this(null, timestamp, score, expectedValue, actualValue, description, new HashMap<>());
    }

    public AnomalyResult() {
        this(null, 0L, 0.0, 0.0, 0.0, null, new HashMap<>());
    }

    /**
     * Creates a builder for AnomalyResult.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating AnomalyResult instances.
     */
    public static class Builder {
        private String id;
        private long timestamp;
        private double score;
        private double expectedValue;
        private double actualValue;
        private String description;
        private Map<String, Object> metadata = new HashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder expectedValue(double expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        public Builder actualValue(double actualValue) {
            this.actualValue = actualValue;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : new HashMap<>();
            return this;
        }

        public AnomalyResult build() {
            return new AnomalyResult(id, timestamp, score, expectedValue, actualValue, description, metadata);
        }
    }
}