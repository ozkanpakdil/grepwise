package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.RetentionPolicy;
import io.github.ozkanpakdil.grepwise.service.RetentionPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing log retention policies.
 */
@RestController
@RequestMapping("/api/config/retention-policies")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class RetentionPolicyController {

    private final RetentionPolicyService retentionPolicyService;

    public RetentionPolicyController(RetentionPolicyService retentionPolicyService) {
        this.retentionPolicyService = retentionPolicyService;
    }

    /**
     * Get all retention policies.
     *
     * @return A list of all policies
     */
    @GetMapping
    public ResponseEntity<List<RetentionPolicy>> getAllPolicies() {
        return ResponseEntity.ok(retentionPolicyService.getAllPolicies());
    }

    /**
     * Get a retention policy by ID.
     *
     * @param id The ID of the policy to get
     * @return The policy
     */
    @GetMapping("/{id}")
    public ResponseEntity<RetentionPolicy> getPolicyById(@PathVariable String id) {
        RetentionPolicy policy = retentionPolicyService.getPolicyById(id);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(policy);
    }

    /**
     * Create a new retention policy.
     *
     * @param policy The policy to create
     * @return The created policy
     */
    @PostMapping
    public ResponseEntity<RetentionPolicy> createPolicy(@RequestBody RetentionPolicy policy) {
        return ResponseEntity.ok(retentionPolicyService.savePolicy(policy));
    }

    /**
     * Update an existing retention policy.
     *
     * @param id The ID of the policy to update
     * @param policy The updated policy
     * @return The updated policy
     */
    @PutMapping("/{id}")
    public ResponseEntity<RetentionPolicy> updatePolicy(@PathVariable String id, @RequestBody RetentionPolicy policy) {
        RetentionPolicy existingPolicy = retentionPolicyService.getPolicyById(id);
        if (existingPolicy == null) {
            return ResponseEntity.notFound().build();
        }

        policy.setId(id);
        return ResponseEntity.ok(retentionPolicyService.savePolicy(policy));
    }

    /**
     * Delete a retention policy.
     *
     * @param id The ID of the policy to delete
     * @return No content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable String id) {
        if (retentionPolicyService.deletePolicy(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Apply a specific retention policy.
     *
     * @param id The ID of the policy to apply
     * @return The number of logs deleted
     */
    @PostMapping("/{id}/apply")
    public ResponseEntity<Long> applyPolicy(@PathVariable String id) {
        RetentionPolicy policy = retentionPolicyService.getPolicyById(id);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }

        long deleted = retentionPolicyService.applyRetentionPolicy(policy);
        return ResponseEntity.ok(deleted);
    }

    /**
     * Apply all enabled retention policies.
     *
     * @return The total number of logs deleted
     */
    @PostMapping("/apply-all")
    public ResponseEntity<Long> applyAllPolicies() {
        long deleted = retentionPolicyService.manuallyApplyRetentionPolicies();
        return ResponseEntity.ok(deleted);
    }
}