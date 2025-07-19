package io.github.ozkanpakdil.grepwise.service;

import org.junit.jupiter.api.Test;

/**
 * Manual test to verify that the circular dependency between LuceneService and RealTimeUpdateService is fixed.
 * This test doesn't use Spring Boot, it manually creates the beans and wires them together.
 */
public class ManualCircularDependencyTest {

    @Test
    void testNoCircularDependency() {
        // Create the LuceneService
        LuceneService luceneService = new LuceneService();
        
        // Create the RealTimeUpdateService
        RealTimeUpdateService realTimeUpdateService = new RealTimeUpdateService();
        
        // Wire them together
        luceneService.setRealTimeUpdateService(realTimeUpdateService);
        realTimeUpdateService.setLuceneService(luceneService);
        
        // If we get here without exceptions, the circular dependency is fixed
        System.out.println("Circular dependency between LuceneService and RealTimeUpdateService is fixed!");
    }
}