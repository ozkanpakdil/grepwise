package io.github.ozkanpakdil.grepwise;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify the CI configuration is properly set up.
 * This test checks for the existence of the GitHub Actions workflow file.
 * This is a simple JUnit test that doesn't require the Spring context.
 */
public class CIConfigurationTest {

    @Test
    public void testCIWorkflowFileExists() {
        // Check that the GitHub Actions workflow file exists
        Path workflowPath = Paths.get(".github", "workflows", "ci.yml");
        File workflowFile = workflowPath.toFile();
        
        assertTrue(workflowFile.exists(), "GitHub Actions workflow file should exist");
    }

    @Test
    public void testCIWorkflowFileContainsRequiredJobs() throws Exception {
        // Check that the GitHub Actions workflow file contains the required jobs
        Path workflowPath = Paths.get(".github", "workflows", "ci.yml");
        String content = Files.readString(workflowPath);
        
        assertTrue(content.contains("backend-tests"), "Workflow should contain backend tests job");
        assertTrue(content.contains("frontend-tests"), "Workflow should contain frontend tests job");
    }
}