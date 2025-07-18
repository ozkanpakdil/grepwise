package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.RetentionPolicy;
import io.github.ozkanpakdil.grepwise.repository.RetentionPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest
public class RetentionPolicyServiceTest {

    @Autowired
    private RetentionPolicyService retentionPolicyService;

    @Autowired
    private RetentionPolicyRepository retentionPolicyRepository;

    @MockBean
    private LuceneService luceneService;

    @BeforeEach
    void setUp() {
        // Clear any existing policies before each test
        List<RetentionPolicy> existingPolicies = retentionPolicyRepository.findAll();
        for (RetentionPolicy policy : existingPolicies) {
            retentionPolicyRepository.deleteById(policy.getId());
        }

        // Mock LuceneService methods with lenient() to avoid "unnecessary stubbing" errors
        try {
            // Use lenient() to avoid unnecessary stubbing errors
            lenient().when(luceneService.deleteLogsOlderThan(anyLong())).thenReturn(10L);
            lenient().when(luceneService.deleteLogsOlderThanForSource(anyLong(), any())).thenReturn(5L);
        } catch (IOException e) {
            fail("Failed to mock LuceneService", e);
        }
    }

    @Test
    void testCreateRetentionPolicy() {
        // Create a test policy
        RetentionPolicy policy = new RetentionPolicy();
        policy.setName("Test Policy");
        policy.setMaxAgeDays(30);
        policy.setEnabled(true);

        // Create the policy
        RetentionPolicy createdPolicy = retentionPolicyService.savePolicy(policy);

        // Verify the policy was created
        assertNotNull(createdPolicy);
        assertNotNull(createdPolicy.getId());
        assertEquals("Test Policy", createdPolicy.getName());
        assertEquals(30, createdPolicy.getMaxAgeDays());
        assertTrue(createdPolicy.isEnabled());
        assertNull(createdPolicy.getApplyToSources()); // Should be null by default (apply to all sources)
    }

    @Test
    void testCreateRetentionPolicyWithSources() {
        // Create a test policy with specific sources
        RetentionPolicy policy = new RetentionPolicy();
        policy.setName("Source-Specific Policy");
        policy.setMaxAgeDays(15);
        policy.setEnabled(true);
        policy.setApplyToSources(Arrays.asList("app.log", "system.log"));

        // Create the policy
        RetentionPolicy createdPolicy = retentionPolicyService.savePolicy(policy);

        // Verify the policy was created
        assertNotNull(createdPolicy);
        assertNotNull(createdPolicy.getId());
        assertEquals("Source-Specific Policy", createdPolicy.getName());
        assertEquals(15, createdPolicy.getMaxAgeDays());
        assertTrue(createdPolicy.isEnabled());
        assertEquals(2, createdPolicy.getApplyToSources().size());
        assertTrue(createdPolicy.getApplyToSources().contains("app.log"));
        assertTrue(createdPolicy.getApplyToSources().contains("system.log"));
    }

    @Test
    void testGetAllPolicies() {
        // Create multiple policies
        RetentionPolicy policy1 = createTestPolicy("Policy 1", 30, true, null);
        RetentionPolicy policy2 = createTestPolicy("Policy 2", 15, false, Arrays.asList("app.log"));

        retentionPolicyService.savePolicy(policy1);
        retentionPolicyService.savePolicy(policy2);

        // Get all policies
        List<RetentionPolicy> policies = retentionPolicyService.getAllPolicies();

        // Verify
        assertEquals(2, policies.size());
    }

    @Test
    void testUpdatePolicy() {
        // Create a policy
        RetentionPolicy policy = createTestPolicy("Original Name", 30, true, null);
        RetentionPolicy createdPolicy = retentionPolicyService.savePolicy(policy);

        // Update the policy
        createdPolicy.setName("Updated Name");
        createdPolicy.setMaxAgeDays(60);

        RetentionPolicy updatedPolicy = retentionPolicyService.savePolicy(createdPolicy);

        // Verify
        assertNotNull(updatedPolicy);
        assertEquals("Updated Name", updatedPolicy.getName());
        assertEquals(60, updatedPolicy.getMaxAgeDays());
    }

    @Test
    void testDeletePolicy() {
        // Create a policy
        RetentionPolicy policy = createTestPolicy("To Delete", 30, true, null);
        RetentionPolicy createdPolicy = retentionPolicyService.savePolicy(policy);

        // Delete the policy
        boolean deleted = retentionPolicyService.deletePolicy(createdPolicy.getId());

        // Verify
        assertTrue(deleted);
        assertNull(retentionPolicyService.getPolicyById(createdPolicy.getId()));
    }

    @Test
    void testApplyRetentionPolicy() throws IOException {
        // Create a policy
        RetentionPolicy policy = createTestPolicy("Test Apply", 30, true, null);
        RetentionPolicy createdPolicy = retentionPolicyService.savePolicy(policy);

        // Apply the policy
        long deleted = retentionPolicyService.applyRetentionPolicy(createdPolicy);

        // Verify
        assertEquals(10L, deleted); // Should match the mocked return value
    }

    @Test
    void testApplyRetentionPolicyWithSources() throws IOException {
        // Create a policy with specific sources
        RetentionPolicy policy = createTestPolicy("Test Apply Sources", 30, true, 
                Arrays.asList("app.log", "system.log"));
        RetentionPolicy createdPolicy = retentionPolicyService.savePolicy(policy);

        // Apply the policy
        long deleted = retentionPolicyService.applyRetentionPolicy(createdPolicy);

        // Verify
        assertEquals(10L, deleted); // Should be 5L * 2 sources, but our mock returns a fixed value
    }

    @Test
    void testManuallyApplyRetentionPolicies() throws IOException {
        // Create multiple policies
        RetentionPolicy policy1 = createTestPolicy("Policy 1", 30, true, null);
        RetentionPolicy policy2 = createTestPolicy("Policy 2", 15, true, Arrays.asList("app.log"));
        RetentionPolicy policy3 = createTestPolicy("Policy 3", 7, false, null); // Disabled

        retentionPolicyService.savePolicy(policy1);
        retentionPolicyService.savePolicy(policy2);
        retentionPolicyService.savePolicy(policy3);

        // Apply all policies
        long deleted = retentionPolicyService.manuallyApplyRetentionPolicies();

        // Verify
        assertEquals(15L, deleted); // Should be 10L + 5L, as policy3 is disabled and policy2 deletes 5L logs
    }

    @Test
    void testPolicyAppliesTo() {
        // Create a policy with specific sources
        RetentionPolicy policy = createTestPolicy("Test Applies To", 30, true, 
                Arrays.asList("app.log", "system.log"));

        // Test appliesTo method
        assertTrue(policy.appliesTo("app.log"));
        assertTrue(policy.appliesTo("system.log"));
        assertFalse(policy.appliesTo("other.log"));

        // Create a policy with no sources (applies to all)
        RetentionPolicy allPolicy = createTestPolicy("All Sources", 30, true, null);

        // Test appliesTo method
        assertTrue(allPolicy.appliesTo("app.log"));
        assertTrue(allPolicy.appliesTo("system.log"));
        assertTrue(allPolicy.appliesTo("other.log"));
    }

    @Test
    void testGetThresholdTimestamp() {
        // Create a policy with 30 days retention
        RetentionPolicy policy = createTestPolicy("Test Threshold", 30, true, null);

        // Get threshold timestamp
        long threshold = policy.getThresholdTimestamp();

        // Verify it's in the past
        assertTrue(threshold < System.currentTimeMillis());

        // Verify it's approximately 30 days ago (with some margin for test execution time)
        long thirtyDaysInMillis = 30L * 24L * 60L * 60L * 1000L;
        long expectedThreshold = System.currentTimeMillis() - thirtyDaysInMillis;
        long margin = 1000L; // 1 second margin
        assertTrue(Math.abs(threshold - expectedThreshold) < margin);
    }

    private RetentionPolicy createTestPolicy(String name, int maxAgeDays, boolean enabled, List<String> sources) {
        RetentionPolicy policy = new RetentionPolicy();
        policy.setName(name);
        policy.setMaxAgeDays(maxAgeDays);
        policy.setEnabled(enabled);
        policy.setApplyToSources(sources);
        return policy;
    }
}