package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Simple test to verify that the circular dependency between LuceneService and RealTimeUpdateService is fixed.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class CircularDependencyTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testNoCircularDependency() {
        // Get the LuceneService bean
        LuceneService luceneService = applicationContext.getBean(LuceneService.class);
        assertNotNull(luceneService, "LuceneService bean should not be null");

        // Get the RealTimeUpdateService bean
        RealTimeUpdateService realTimeUpdateService = applicationContext.getBean(RealTimeUpdateService.class);
        assertNotNull(realTimeUpdateService, "RealTimeUpdateService bean should not be null");

        // If we get here without exceptions, the circular dependency is fixed
        System.out.println("Circular dependency between LuceneService and RealTimeUpdateService is fixed!");
    }
}