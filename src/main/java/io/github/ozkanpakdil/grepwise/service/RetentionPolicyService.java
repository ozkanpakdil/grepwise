package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.RetentionPolicy;
import io.github.ozkanpakdil.grepwise.repository.RetentionPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Service for managing and applying log retention policies.
 */
@Service
public class RetentionPolicyService {
    private static final Logger logger = LoggerFactory.getLogger(RetentionPolicyService.class);

    private final RetentionPolicyRepository retentionPolicyRepository;
    private final LuceneService luceneService;
    private final LogScannerService logScannerService;

    /**
     * Constructor for RetentionPolicyService.
     */
    public RetentionPolicyService(RetentionPolicyRepository retentionPolicyRepository,
                                  LuceneService luceneService,
                                  LogScannerService logScannerService) {
        this.retentionPolicyRepository = retentionPolicyRepository;
        this.luceneService = luceneService;
        this.logScannerService = logScannerService;
        logger.info("RetentionPolicyService initialized");
    }

    /**
     * Apply all enabled retention policies. This method is scheduled to run daily.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run at midnight every day
    public void applyRetentionPolicies() {
        logger.info("Starting scheduled application of retention policies");

        try {
            // Get all enabled retention policies
            List<RetentionPolicy> enabledPolicies = retentionPolicyRepository.findAllEnabled();
            logger.info("Found {} enabled retention policies", enabledPolicies.size());

            if (enabledPolicies.isEmpty()) {
                logger.info("No enabled retention policies found, skipping retention process");
                return;
            }

            // Apply each policy
            for (RetentionPolicy policy : enabledPolicies) {
                applyRetentionPolicy(policy);
            }

            logger.info("Completed application of retention policies");
        } catch (Exception e) {
            logger.error("Error applying retention policies", e);
        }
    }

    /**
     * Apply a single retention policy.
     *
     * @param policy The policy to apply
     * @return The number of logs deleted
     */
    public long applyRetentionPolicy(RetentionPolicy policy) {
        logger.info("Applying retention policy: {}", policy);

        try {
            long thresholdTimestamp = policy.getThresholdTimestamp();
            long deletedCount = 0;

            // If policy applies to specific sources
            List<String> sources = policy.getApplyToSources();
            if (sources != null && !sources.isEmpty()) {
                logger.info("Policy applies to specific sources: {}", sources);

                // Apply policy to each source
                for (String source : sources) {
                    long deleted = luceneService.deleteLogsOlderThanForSource(thresholdTimestamp, source);
                    deletedCount += deleted;
                    logger.info("Deleted {} logs for source: {}", deleted, source);
                }
            } else {
                // Policy applies to all sources
                logger.info("Policy applies to all sources");
                deletedCount = luceneService.deleteLogsOlderThan(thresholdTimestamp);
            }

            logger.info("Applied retention policy: {}. Deleted {} logs older than {} days",
                    policy.getName(), deletedCount, policy.getMaxAgeDays());

            return deletedCount;
        } catch (IOException e) {
            logger.error("Error applying retention policy: {}", policy, e);
            return 0;
        }
    }

    /**
     * Manually trigger the application of retention policies.
     *
     * @return The total number of logs deleted
     */
    public long manuallyApplyRetentionPolicies() {
        logger.info("Manually applying retention policies");

        try {
            // Get all enabled retention policies
            List<RetentionPolicy> enabledPolicies = retentionPolicyRepository.findAllEnabled();
            logger.info("Found {} enabled retention policies", enabledPolicies.size());

            if (enabledPolicies.isEmpty()) {
                logger.info("No enabled retention policies found, skipping retention process");
                return 0;
            }

            // Apply each policy
            long totalDeleted = 0;
            for (RetentionPolicy policy : enabledPolicies) {
                long deleted = applyRetentionPolicy(policy);
                totalDeleted += deleted;
            }

            logger.info("Completed manual application of retention policies. Deleted {} logs", totalDeleted);
            return totalDeleted;
        } catch (Exception e) {
            logger.error("Error applying retention policies", e);
            return 0;
        }
    }

    /**
     * Get all retention policies.
     */
    public List<RetentionPolicy> getAllPolicies() {
        return retentionPolicyRepository.findAll();
    }

    /**
     * Get a retention policy by ID.
     */
    public RetentionPolicy getPolicyById(String id) {
        return retentionPolicyRepository.findById(id);
    }

    /**
     * Save a retention policy.
     */
    public RetentionPolicy savePolicy(RetentionPolicy policy) {
        return retentionPolicyRepository.save(policy);
    }

    /**
     * Delete a retention policy by ID.
     */
    public boolean deletePolicy(String id) {
        return retentionPolicyRepository.deleteById(id);
    }
}