import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

/**
 * Test to verify the CI configuration is properly set up.
 * This test checks for the existence of the GitHub Actions workflow file
 * and verifies that it contains the required jobs.
 */
describe('CI Configuration', () => {
  it('should have a GitHub Actions workflow file', () => {
    const workflowPath = path.resolve('../../.github/workflows/ci.yml');
    const fileExists = fs.existsSync(workflowPath);
    
    expect(fileExists).toBe(true);
  });

  it('should have required jobs in the workflow file', () => {
    const workflowPath = path.resolve('../../.github/workflows/ci.yml');
    const content = fs.readFileSync(workflowPath, 'utf8');
    
    expect(content).toContain('backend-tests');
    expect(content).toContain('frontend-tests');
  });

  it('should have proper Node.js setup in the workflow', () => {
    const workflowPath = path.resolve('../../.github/workflows/ci.yml');
    const content = fs.readFileSync(workflowPath, 'utf8');
    
    expect(content).toContain('actions/setup-node');
    expect(content).toContain('npm run test');
  });

  it('should have proper JDK setup in the workflow', () => {
    const workflowPath = path.resolve('../../.github/workflows/ci.yml');
    const content = fs.readFileSync(workflowPath, 'utf8');
    
    expect(content).toContain('actions/setup-java');
    expect(content).toContain('mvnw clean test');
  });
});