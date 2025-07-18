package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.RetentionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for storing and retrieving log retention policies.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class RetentionPolicyRepository {
    private static final Logger logger = LoggerFactory.getLogger(RetentionPolicyRepository.class);
    private final Map<String, RetentionPolicy> policies = new ConcurrentHashMap<>();

    /**
     * Constructor that initializes the repository with a default retention policy.
     */
    public RetentionPolicyRepository() {
        logger.info("Initializing RetentionPolicyRepository");

        // Create a default retention policy (30 days for all sources)
        RetentionPolicy defaultPolicy = new RetentionPolicy();
        defaultPolicy.setName("Default Retention Policy");
        defaultPolicy.setMaxAgeDays(30);
        defaultPolicy.setEnabled(true);
        // null applyToSources means apply to all sources

        save(defaultPolicy);
        logger.info("Initialized default retention policy: {}", defaultPolicy);
    }

    /**
     * Save a retention policy.
     *
     * @param policy The policy to save
     * @return The saved policy with a generated ID
     */
    public RetentionPolicy save(RetentionPolicy policy) {
        if (policy.getId() == null || policy.getId().isEmpty()) {
            policy.setId(UUID.randomUUID().toString());
        }
        policies.put(policy.getId(), policy);
        return policy;
    }

    /**
     * Find a retention policy by ID.
     *
     * @param id The ID of the policy to find
     * @return The policy, or null if not found
     */
    public RetentionPolicy findById(String id) {
        return policies.get(id);
    }

    /**
     * Find all retention policies.
     *
     * @return A list of all policies
     */
    public List<RetentionPolicy> findAll() {
        return new ArrayList<>(policies.values());
    }

    /**
     * Find all enabled retention policies.
     *
     * @return A list of all enabled policies
     */
    public List<RetentionPolicy> findAllEnabled() {
        return policies.values().stream()
                .filter(RetentionPolicy::isEnabled)
                .toList();
    }

    /**
     * Delete a retention policy by ID.
     *
     * @param id The ID of the policy to delete
     * @return true if the policy was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return policies.remove(id) != null;
    }

    /**
     * Delete all retention policies.
     *
     * @return The number of policies deleted
     */
    public int deleteAll() {
        int count = policies.size();
        policies.clear();
        return count;
    }

    /**
     * Get the total number of retention policies.
     *
     * @return The total number of policies
     */
    public int count() {
        return policies.size();
    }
}