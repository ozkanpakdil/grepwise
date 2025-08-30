package io.github.ozkanpakdil.grepwise.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of a predictive analytics operation.
 * Contains information about the prediction, including its timestamp,
 * predicted value, confidence level, and a description.
 */
public record PredictiveResult(
        String id,
        long timestamp,
        long predictionTimestamp,
        double predictedValue,
        double confidenceLevel,
        String predictionType,
        String description,
        Map<String, Object> metadata
) {
    public PredictiveResult {
        metadata = metadata != null ? metadata : new HashMap<>();
    }

    public PredictiveResult(long timestamp, long predictionTimestamp, double predictedValue,
                            double confidenceLevel, String predictionType, String description) {
        this(null, timestamp, predictionTimestamp, predictedValue, confidenceLevel,
                predictionType, description, new HashMap<>());
    }

    public PredictiveResult() {
        this(null, 0L, 0L, 0.0, 0.0, null, null, new HashMap<>());
    }

    /**
     * Creates a builder for PredictiveResult.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating PredictiveResult instances.
     */
    public static class Builder {
        private String id;
        private long timestamp;
        private long predictionTimestamp;
        private double predictedValue;
        private double confidenceLevel;
        private String predictionType;
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

        public Builder predictionTimestamp(long predictionTimestamp) {
            this.predictionTimestamp = predictionTimestamp;
            return this;
        }

        public Builder predictedValue(double predictedValue) {
            this.predictedValue = predictedValue;
            return this;
        }

        public Builder confidenceLevel(double confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
            return this;
        }

        public Builder predictionType(String predictionType) {
            this.predictionType = predictionType;
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

        public PredictiveResult build() {
            return new PredictiveResult(id, timestamp, predictionTimestamp, predictedValue,
                    confidenceLevel, predictionType, description, metadata);
        }
    }
}